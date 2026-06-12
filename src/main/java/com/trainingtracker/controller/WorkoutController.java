package com.trainingtracker.controller;

import com.trainingtracker.entity.Category;
import com.trainingtracker.entity.User;
import com.trainingtracker.repository.CategoryRepository;
import com.trainingtracker.repository.ExerciseRepository;
import com.trainingtracker.repository.UserRepository;
import com.trainingtracker.service.WorkoutService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;
    private final ExerciseRepository exerciseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public WorkoutController(WorkoutService workoutService,
                             ExerciseRepository exerciseRepository,
                             CategoryRepository categoryRepository,
                             UserRepository userRepository) {
        this.workoutService = workoutService;
        this.exerciseRepository = exerciseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Workout overview page
    @GetMapping
    public String showWorkouts(@AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        Long userId = getCurrentUserId(userDetails);
        List<com.trainingtracker.entity.WorkoutEntry> recentWorkouts = workoutService.getRecentWorkouts(userId);
        
        // Group by category, then by exercise within each category
        Map<Category, Map<com.trainingtracker.entity.Exercise, List<com.trainingtracker.entity.WorkoutEntry>>> byCategory = new java.util.LinkedHashMap<>();
        for (com.trainingtracker.entity.WorkoutEntry entry : recentWorkouts) {
            Category cat = entry.getExercise().getCategory();
            com.trainingtracker.entity.Exercise ex = entry.getExercise();
            byCategory.computeIfAbsent(cat, k -> new java.util.LinkedHashMap<>())
                     .computeIfAbsent(ex, k -> new java.util.ArrayList<>())
                     .add(entry);
        }
        
        model.addAttribute("categoryEntries", byCategory);
        return "workouts/workouts";
    }

    // Exercise history page
    @GetMapping("/exercise/{exerciseId}")
    public String showExerciseHistory(@PathVariable Long exerciseId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {
        Long userId = getCurrentUserId(userDetails);
        com.trainingtracker.entity.Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));
        if (!exercise.getCategory().getUser().getId().equals(userId)) {
            return "redirect:/workouts";
        }
        List<com.trainingtracker.entity.WorkoutEntry> entries = workoutService.findByExerciseId(exerciseId, userId);
        model.addAttribute("exercise", exercise);
        model.addAttribute("entries", entries);
        return "workouts/exercise-history";
    }

    // Single workout log page
    @GetMapping("/log")
    public String showLogForm(@RequestParam Long exerciseId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        Long userId = getCurrentUserId(userDetails);
        com.trainingtracker.entity.Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));
        if (!exercise.getCategory().getUser().getId().equals(userId)) {
            return "redirect:/workouts";
        }
        model.addAttribute("exercise", exercise);
        model.addAttribute("todayDate", LocalDate.now().toString());
        return "workouts/log-workout";
    }

    // Handle log workout
    @PostMapping("/log")
    public String logWorkout(@RequestParam Long exerciseId,
                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate workoutDate,
                             @RequestParam int sets,
                             @RequestParam int reps,
                             @RequestParam double weight,
                             @RequestParam String notes,
                             @RequestParam int difficulty,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            workoutService.addEntry(exerciseId, workoutDate, sets, reps, weight, notes, difficulty, userId);
            redirectAttributes.addFlashAttribute("success", "Workout eingetragen!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler: " + e.getMessage());
        }
        return "redirect:/workouts";
    }

    // Delete workout entry
    @GetMapping("/delete/{id}")
    public String deleteWorkout(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            workoutService.deleteEntry(id, userId);
            redirectAttributes.addFlashAttribute("success", "Workout geloescht!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler: " + e.getMessage());
        }
        return "redirect:/workouts";
    }

    // BULK WORKOUT: Landing page - choose a category
    @GetMapping("/bulk-landing")
    public String showBulkLanding(@AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        Long userId = getCurrentUserId(userDetails);
        List<Category> categories = categoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        model.addAttribute("categories", categories);
        return "workouts/bulk-landing";
    }

    // BULK WORKOUT: Show bulk form for a category
    @GetMapping("/bulk")
    public String showBulkForm(@RequestParam Long categoryId,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        Long userId = getCurrentUserId(userDetails);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            return "redirect:/workouts";
        }

        model.addAttribute("category", category);
        model.addAttribute("exercises", category.getExercises());
        model.addAttribute("todayDate", LocalDate.now().toString());
        return "workouts/bulk-workout";
    }

    // Handle bulk workout submission
    @PostMapping("/bulk")
    public String logBulkWorkout(@RequestParam Long categoryId,
                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate workoutDate,
                                 @RequestParam Map<String, String> allParams,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            if (!category.getUser().getId().equals(userId)) {
                throw new RuntimeException("Not authorized");
            }

            List<com.trainingtracker.entity.Exercise> exercises = category.getExercises();
            int count = 0;
            for (com.trainingtracker.entity.Exercise exercise : exercises) {
                String setsStr = allParams.get("sets_" + exercise.getId());
                String repsStr = allParams.get("reps_" + exercise.getId());
                String weightStr = allParams.get("weight_" + exercise.getId());
                String diffStr = allParams.get("diff_" + exercise.getId());

                // Only save if at least sets and reps are provided
                if (setsStr != null && !setsStr.isEmpty() && repsStr != null && !repsStr.isEmpty()) {
                    int sets = Integer.parseInt(setsStr);
                    int reps = Integer.parseInt(repsStr);
                    double weight = (weightStr != null && !weightStr.isEmpty()) ? Double.parseDouble(weightStr) : 0;
                    int difficulty = (diffStr != null && !diffStr.isEmpty()) ? Integer.parseInt(diffStr) : 3;

                    workoutService.addEntry(
                            exercise.getId(),
                            workoutDate, sets, reps, weight, null, difficulty, userId
                    );
                    count++;
                }
            }

            redirectAttributes.addFlashAttribute("success", count + " Workout(s) fuer '" + category.getName() + "' eingetragen!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler: " + e.getMessage());
        }
        return "redirect:/workouts";
    }
}
