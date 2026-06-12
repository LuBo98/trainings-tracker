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

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    @Transactional
    public void requestPasswordReset(String email, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("No account found with that email"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        if (mailEnabled) {
            sendResetEmail(user, token);
            redirectAttributes.addFlashAttribute("info",
                    "Password reset link sent to " + user.getEmail());
        } else {
            // Fallback: store token info for display when mail not configured
            redirectAttributes.addFlashAttribute("resetToken", token);
            redirectAttributes.addFlashAttribute("resetEmail", user.getEmail());
            redirectAttributes.addFlashAttribute("info",
                    "Mail not configured. Use this token directly (valid for 24 hours):");
        }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        if (user.getResetTokenExpiry() == null ||
            user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
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
