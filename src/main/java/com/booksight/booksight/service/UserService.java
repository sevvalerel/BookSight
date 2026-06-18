package com.booksight.booksight.service;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.*;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.ReadingStatusRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReviewRepository reviewRepository;
    private final ReadingStatusRepository readingStatusRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       ReviewRepository reviewRepository,
                       ReadingStatusRepository readingStatusRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.reviewRepository = reviewRepository;
        this.readingStatusRepository = readingStatusRepository;
    }

    public User register(String username, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Bu email zaten kayıtlı!");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Bu kullanıcı adı zaten alınmış!");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public User findByEmailOrUsername(String emailOrUsername) {
        return userRepository.findByEmail(emailOrUsername)
                .orElseGet(() -> userRepository.findByUsername(emailOrUsername)
                        .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!")));
    }

    public String loginWithUser(User user, String password, JwtUtil jwtUtil) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Şifre yanlış!");
        }
        return jwtUtil.generateToken(user.getUsername());
    }

    public String login(String email, String password, JwtUtil jwtUtil) {
        User user = findByEmailOrUsername(email);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Şifre yanlış!");
        }
        return jwtUtil.generateToken(user.getUsername());
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
    }

    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getUsername() != null) {
            String username = request.getUsername().trim();
            if (username.isEmpty()) {
                throw new RuntimeException("Kullanıcı adı boş olamaz!");
            }
            if (!username.equals(user.getUsername())
                    && userRepository.existsByUsernameAndUserIdNot(username, userId)) {
                throw new RuntimeException("Bu kullanıcı adı zaten alınmış!");
            }
            user.setUsername(username);
        }

        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        boolean hasNewPassword = request.getNewPassword() != null
                && !request.getNewPassword().isBlank();
        if (hasNewPassword) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new RuntimeException("Mevcut şifre gerekli!");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new RuntimeException("Mevcut şifre hatalı");
            }
            if (request.getNewPassword().equals(request.getCurrentPassword())) {
                throw new RuntimeException("Yeni şifre mevcut şifreyle aynı olamaz");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public CheckinResponseDTO checkin(Long userId) {
        User user = getUserById(userId);
        LocalDate today = LocalDate.now();
        LocalDate lastCheckin = user.getLastCheckinDate();

        if (lastCheckin != null && lastCheckin.equals(today)) {
            throw new RuntimeException("Bugün zaten okuma kaydettiniz");
        }

        if (lastCheckin != null && lastCheckin.equals(today.minusDays(1))) {
            user.setStreak(user.getStreak() + 1);
        } else {
            user.setStreak(1);
        }

        user.setTotalPoints(user.getTotalPoints() + 10);
        user.setLastCheckinDate(today);
        userRepository.save(user);

        return new CheckinResponseDTO(user.getStreak(), user.getTotalPoints(), false);
    }

    public CheckinResponseDTO getCheckinStatus(Long userId) {
        User user = getUserById(userId);
        boolean alreadyCheckedIn = LocalDate.now().equals(user.getLastCheckinDate());
        return new CheckinResponseDTO(user.getStreak(), user.getTotalPoints(), alreadyCheckedIn);
    }

    public List<LeaderboardEntryDTO> getLeaderboard() {
        return userRepository.findTop20ByOrderByTotalPointsDesc().stream()
                .map(u -> new LeaderboardEntryDTO(
                        u.getUserId(),
                        u.getUsername(),
                        u.getAvatarUrl(),
                        u.getTotalPoints(),
                        u.getStreak()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserPublicProfileDTO getUserPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        List<Review> reviews = reviewRepository.findByUserIdWithBook(user.getUserId());
        long readCount = readingStatusRepository.countByUserUserIdAndStatus(user.getUserId(), "READ");

        Map<String, Long> genreCounts = reviews.stream()
                .map(r -> r.getBook().getGenre())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        List<String> favoriteGenres = genreCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<UserPublicProfileDTO.SimpleReview> simpleReviews = reviews.stream()
                .map(r -> new UserPublicProfileDTO.SimpleReview(
                        r.getBook().getTitle(),
                        r.getBook().getCoverUrl(),
                        r.getRating(),
                        r.getReviewText()))
                .collect(Collectors.toList());

        return new UserPublicProfileDTO(
                user.getUserId(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getTotalPoints(),
                user.getStreak(),
                reviews.size(),
                readCount,
                favoriteGenres,
                simpleReviews);
    }
}
