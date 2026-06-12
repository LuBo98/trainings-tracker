package com.trainingtracker.service;

import com.trainingtracker.entity.User;
import com.trainingtracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String baseUrl;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JavaMailSender mailSender,
                       @Value("${app.mail.enabled:false}") boolean mailEnabled,
                       @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.baseUrl = baseUrl;
    }

    public User register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email.toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    /**
     * Passwort-Validierung: mindestens 8 Zeichen, Gross-/Kleinbuchstaben + Zahl oder Sonderzeichen.
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigitOrSpecial = password.matches(".*[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
        return (hasUpper && hasLower) && hasDigitOrSpecial;
    }

    public String validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigitOrSpecial = password.matches(".*[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
        if (!hasUpper || !hasLower) {
            return "Password must contain both uppercase and lowercase letters";
        }
        if (!hasDigitOrSpecial) {
            return "Password must contain at least one digit or special character";
        }
        return null; // valid
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    @Transactional
    public void requestPasswordReset(String email, RedirectAttributes redirectAttributes) {
        // Always show generic message to prevent email enumeration
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);

        if (user != null) {
            String token = UUID.randomUUID().toString();
            // Hash token before storing - prevents token leakage from DB access
            String hashedToken = passwordEncoder.encode(token);
            user.setResetToken(hashedToken);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);

            if (mailEnabled) {
                sendResetEmail(user, token);
                redirectAttributes.addFlashAttribute("info",
                        "If an account with that email exists, a reset link has been sent.");
            } else {
                // Fallback: store plaintext token for display when mail not configured
                // (stored separately, not in DB)
                redirectAttributes.addFlashAttribute("resetToken", token);
                redirectAttributes.addFlashAttribute("resetEmail", user.getEmail());
                redirectAttributes.addFlashAttribute("info",
                        "Mail not configured. Use this token directly (valid for 24 hours):");
            }
        } else {
            // Same message whether email exists or not - prevents enumeration
            redirectAttributes.addFlashAttribute("info",
                    "If an account with that email exists, a reset link has been sent.");
        }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        // Find all users with non-expired reset tokens and check if the provided
        // token matches the stored BCrypt hash
        List<User> candidates = userRepository
                .findByResetTokenNotNullAndResetTokenExpiryAfter(LocalDateTime.now());

        User target = null;
        for (User candidate : candidates) {
            if (passwordEncoder.matches(token, candidate.getResetToken())) {
                target = candidate;
                break;
            }
        }

        if (target == null) {
            return false;
        }

        target.setPassword(passwordEncoder.encode(newPassword));
        target.setResetToken(null);
        target.setResetTokenExpiry(null);
        userRepository.save(target);
        return true;
    }

    private void sendResetEmail(User user, String token) {
        try {
            String resetLink = baseUrl + "/auth/reset-password/" + token;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Password Reset - Training Tracker");
            message.setText("Click the link below to reset your password (valid for 24 hours):\n\n" +
                           resetLink + "\n\nIf you didn't request this, please ignore this email.");
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset email", e);
        }
    }
}
