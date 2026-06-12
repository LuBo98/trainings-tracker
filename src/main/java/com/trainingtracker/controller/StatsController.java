package com.trainingtracker.controller;

import com.trainingtracker.entity.Category;
import com.trainingtracker.entity.User;
import com.trainingtracker.repository.CategoryRepository;
import com.trainingtracker.repository.UserRepository;
import com.trainingtracker.service.WorkoutService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/stats")
public class StatsController {

    private final WorkoutService workoutService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public StatsController(WorkoutService workoutService,
                            UserRepository userRepository,
                            CategoryRepository categoryRepository) {
        this.workoutService = workoutService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public String showStats(@RequestParam(required = false, defaultValue = "all") String scope,
                           @RequestParam(required = false) Long exerciseId,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        Long userId = getCurrentUserId(userDetails);

        // Default date range: last 30 days
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        Map<String, Object> stats;
        List<Category> categories = categoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        switch (scope) {
            case "exercise":
                if (exerciseId == null) {
                    stats = Map.of();
                    model.addAttribute("error", "Bitte waehle eine Uebung");
                } else {
                    stats = workoutService.getExerciseStats(exerciseId, startDate, endDate);
                }
                break;
            case "category":
                stats = workoutService.getCategoryStats(categoryId != null ? categoryId : null, userId, startDate, endDate);
                break;
            default:
                stats = workoutService.getUserStats(userId, startDate, endDate);
                break;
        }

        // Build chart data
        Map<String, Object> chartData = workoutService.getChartData(
                scope, exerciseId, categoryId, userId, startDate, endDate);

        model.addAttribute("stats", stats);
        model.addAttribute("scope", scope);
        model.addAttribute("exerciseId", exerciseId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("startDateVal", startDate.toString());
        model.addAttribute("endDateVal", endDate.toString());
        model.addAttribute("chartData", chartData);
        model.addAttribute("categories", categories);

        return "stats/stats";
    }
}
