package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.NlpResult;
import com.booksight.booksight.entity.Recommendation;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.repository.NlpResultRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final NlpClient nlpClient;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final NlpResultRepository nlpResultRepository;
    private final UserRepository userRepository;

    // ── Controller'dan çağrılan metot ─────────────────────────────────────────

    public List<Recommendation> getRecommendations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        List<Book> recommended = recommend(user, 10);

        return recommended.stream().map(book -> {
            Recommendation rec = new Recommendation();
            rec.setUser(user);
            rec.setBook(book);
            rec.setIsAiGenerated(true);
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
            // avgRating'e göre sırala, limit kadar al
            return bookRepository.findAll().stream()
                    .sorted(Comparator.comparingDouble(
                            b -> b.getAvgRating() != null ? -b.getAvgRating() : 0))
                    .limit(limit)
                    .toList();
        }

        // Kullanıcının okuduğu kitap ID'leri
        Set<Long> readBookIds = reviewRepository
                .findByUserUserId(user.getUserId())
                .stream()
                .map(r -> r.getBook().getBookId())
                .collect(Collectors.toSet());

        // Tüm kitaplar için benzerlik hesapla
        List<Book> allBooks = bookRepository.findAll();
        List<BookScore> scored = allBooks.stream()
                .filter(b -> !readBookIds.contains(b.getBookId()))
                .map(b -> new BookScore(b, cosineSimilarity(userProfile, getBookProfile(b))))
                .filter(bs -> bs.score() > 0)
                .sorted(Comparator.comparingDouble(BookScore::score).reversed())
                .limit(limit)
                .toList();

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

    // ── Kitap profili ─────────────────────────────────────────────────────────

    private Map<String, Double> getBookProfile(Book book) {
        List<Review> reviews = reviewRepository.findByBookBookIdOrderByCreatedAtDesc(book.getBookId());
        Map<String, List<Double>> labelScores = new HashMap<>();

        for (Review review : reviews) {
            NlpResult nlp = nlpResultRepository.findByReview(review).orElse(null);
            if (nlp == null || nlp.getLabelScores() == null) continue;
            nlp.getLabelScores().forEach((label, score) ->
                    labelScores.computeIfAbsent(label, k -> new ArrayList<>()).add(score));
        }

        Map<String, Double> profile = new HashMap<>();
        labelScores.forEach((label, scores) ->
                profile.put(label, scores.stream().mapToDouble(Double::doubleValue).average().orElse(0)));

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