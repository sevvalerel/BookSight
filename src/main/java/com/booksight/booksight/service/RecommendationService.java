package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.NlpResult;
import com.booksight.booksight.entity.Recommendation;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.repository.NlpResultRepository;
import com.booksight.booksight.repository.ReadingStatusRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import com.booksight.booksight.repository.RecommendationFeedbackRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final NlpClient nlpClient;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final NlpResultRepository nlpResultRepository;
    private final UserRepository userRepository;
    private final ReadingStatusRepository readingStatusRepository;
    private final RecommendationFeedbackRepository feedbackRepository;

    // ── Controller'dan çağrılan metot ─────────────────────────────────────────

    public List<Recommendation> getRecommendations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        boolean isPersonalized = !buildUserProfile(user).isEmpty();
        List<Book> recommended = recommend(user, 10);
        return recommended.stream().map(book -> {
            Recommendation rec = new Recommendation();
            rec.setUser(user);
            rec.setBook(book);
            rec.setIsAiGenerated(isPersonalized);
            rec.setCreatedAt(java.time.LocalDateTime.now());
            rec.setConfidenceScore(0.0);
            return rec;
        }).toList();
    }

    // ── Ana öneri metodu ──────────────────────────────────────────────────────

    public List<Book> recommend(User user, int limit) {
        log.info("Öneri hesaplanıyor: userId={}", user.getUserId());

        Map<String, Double> userProfile = buildUserProfile(user);
        if (userProfile.isEmpty()) {
            log.warn("Kullanıcının henüz yorumu yok, popüler kitaplar dönülüyor");
            Set<Long> excludeIds = readingStatusRepository.findByUserUserId(user.getUserId())
                    .stream()
                    .map(rs -> rs.getBook().getBookId())
                    .collect(Collectors.toSet());
            return bookRepository.findAll().stream()
                    .filter(b -> !excludeIds.contains(b.getBookId()))
                    .sorted(Comparator.comparingDouble(
                            b -> b.getAvgRating() != null ? -b.getAvgRating() : 0))
                    .limit(limit)
                    .toList();
        }

        // Kullanıcının okuduğu/yorum yaptığı kitap ID'leri (öneri havuzundan hariç tutulacak)
        Set<Long> readBookIds = reviewRepository
                .findByUserUserId(user.getUserId())
                .stream()
                .map(r -> r.getBook().getBookId())
                .collect(Collectors.toCollection(HashSet::new));

        // Kullanıcının kütüphanesindeki (okudum/okuyorum/okuyacağım) kitapları da hariç tut
        readingStatusRepository.findByUserUserId(user.getUserId())
                .forEach(rs -> readBookIds.add(rs.getBook().getBookId()));
        // Beğenilmeyen kitapları da hariç tut
        feedbackRepository.findByUserUserIdAndLikedFalse(user.getUserId())
                .forEach(fb -> readBookIds.add(fb.getBook().getBookId()));
        // Tüm kitaplar için benzerlik hesapla
        List<Book> allBooks = bookRepository.findAll();
        List<Book> unread = allBooks.stream()
                .filter(b -> !readBookIds.contains(b.getBookId()))
                .toList();

        List<BookScore> scored = unread.stream()
                .map(b -> new BookScore(b, cosineSimilarity(userProfile, getBookProfile(b))))
                .filter(bs -> bs.score() > 0)
                .sorted(Comparator.comparingDouble(BookScore::score).reversed())
                .limit(limit)
                .collect(Collectors.toCollection(ArrayList::new));

        // NLP eşleşmesi yetersizse popüler kitaplarla tamamla
        if (scored.size() < limit) {
            Set<Long> scoredIds = scored.stream()
                    .map(bs -> bs.book().getBookId())
                    .collect(Collectors.toSet());
            unread.stream()
                    .filter(b -> !scoredIds.contains(b.getBookId()))
                    .sorted(Comparator.comparingDouble(
                            b -> b.getAvgRating() != null ? -b.getAvgRating() : 0))
                    .limit(limit - scored.size())
                    .forEach(b -> scored.add(new BookScore(b, 0.0)));
        }

        log.info("Öneri tamamlandı: {} kitap bulundu", scored.size());
        return scored.stream().map(BookScore::book).toList();
    }

    // ── Kullanıcı profili ─────────────────────────────────────────────────────

    private Map<String, Double> buildUserProfile(User user) {
        List<Review> reviews = reviewRepository.findByUserUserId(user.getUserId());
        Map<String, Double> profile = new HashMap<>();

        for (Review review : reviews) {
            NlpResult nlp = nlpResultRepository.findByReview(review).orElse(null);
            if (nlp == null || nlp.getDetectedLabels() == null) continue;
            for (String label : nlp.getDetectedLabels()) {
                profile.merge(label, 1.0, Double::sum);
            }
        }

        if (!profile.isEmpty()) {
            double max = Collections.max(profile.values());
            profile.replaceAll((k, v) -> v / max);
        }

        return profile;
    }

    // ── Kitap profili — book.labels kolonundan okur ───────────────────────────

    private Map<String, Double> getBookProfile(Book book) {
        List<String> labels = book.getLabels();
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Double> profile = new HashMap<>();
        for (String label : labels) {
            profile.put(label, 1.0);
        }
        return profile;
    }

    // ── Kosinüs benzerliği ────────────────────────────────────────────────────

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;

        Set<String> allKeys = new HashSet<>(a.keySet());
        allKeys.addAll(b.keySet());

        double dot = 0, normA = 0, normB = 0;
        for (String key : allKeys) {
            double va = a.getOrDefault(key, 0.0);
            double vb = b.getOrDefault(key, 0.0);
            dot   += va * vb;
            normA += va * va;
            normB += vb * vb;
        }

        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ── Yorum kaydedince NLP çalıştır ─────────────────────────────────────────

    public NlpResult analyzeAndSave(Review review) {
        try {
            var response = nlpClient.analyze(
                    review.getReviewId().toString(),
                    review.getReviewText()
            );

            NlpResult result = new NlpResult();
            result.setReview(review);
            result.setDetectedLabels(response.getDetectedLabels());

            Map<String, Double> scores = new HashMap<>();
            if (response.getLabels() != null) {
                response.getLabels().forEach(l -> scores.put(l.getLabel(), l.getConfidence()));
            }
            result.setLabelScores(scores);
            result.setProcessedAt(response.getProcessed_at());

            return nlpResultRepository.save(result);
        } catch (Exception e) {
            log.error("NLP analiz hatası reviewId={}: {}", review.getReviewId(), e.getMessage());
            return null;
        }
    }

    // ── Inner class ───────────────────────────────────────────────────────────
    private record BookScore(Book book, double score) {}
}