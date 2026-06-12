package com.trainingtracker.controller;

import com.trainingtracker.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Login page
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully");
        }
        return "auth/login";
    }

    // Register page
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", Map.of());
        return "auth/register";
    }

    // Handle registration
    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          RedirectAttributes redirectAttributes) {
        try {
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwords do not match");
                return "redirect:/auth/register";
            }
            String passwordError = userService.validatePassword(password);
            if (passwordError != null) {
                redirectAttributes.addFlashAttribute("error", passwordError);
                return "redirect:/auth/register";
            }
            userService.register(username, email, password);
            redirectAttributes.addFlashAttribute("success",
                    "Account created successfully! Please log in.");
            return "redirect:/auth/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        }
    }

    // Forgot password page
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotForm", Map.of());
        return "auth/forgot-password";
    }

    // Handle forgot password request
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.requestPasswordReset(email, redirectAttributes);
            return "redirect:/auth/reset-password-requested";
        } catch (RuntimeException e) {
            // Don't reveal if email exists or not for security
            redirectAttributes.addFlashAttribute("info",
                    "If an account with that email exists, a reset link has been sent.");
            return "redirect:/auth/login";
        }
    }

    // Reset password requested page (shows token when mail not configured)
    @GetMapping("/reset-password-requested")
    public String resetPasswordRequested(Model model) {
        return "auth/reset-password-requested";
    }

    // Reset password page (via token)
    @GetMapping("/reset-password/{token}")
    public String resetPasswordPage(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("resetForm", Map.of());
        return "auth/reset-password";
    }

    // Handle password reset
    @PostMapping("/reset-password/{token}")
    public String resetPassword(@PathVariable String token,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/auth/reset-password/" + token;
        }
        String passwordError = userService.validatePassword(password);
        if (passwordError != null) {
            redirectAttributes.addFlashAttribute("error", passwordError);
            return "redirect:/auth/reset-password/" + token;
        }

        boolean success = userService.resetPassword(token, password);
        if (success) {
            redirectAttributes.addFlashAttribute("success",
                    "Password changed successfully! Please log in with your new password.");
            return "redirect:/auth/login";
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid or expired reset token. Please request a new one.");
            return "redirect:/auth/forgot-password";
        }
    }

    // Dashboard redirect
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("username", userDetails.getUsername());
        return "dashboard";
    }
}
