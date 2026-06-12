package com.trainingtracker.controller;

import com.trainingtracker.entity.Category;
import com.trainingtracker.entity.Exercise;
import com.trainingtracker.repository.UserRepository;
import com.trainingtracker.entity.User;
import com.trainingtracker.service.CategoryService;
import com.trainingtracker.service.ExerciseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public ExerciseController(ExerciseService exerciseService,
                               CategoryService categoryService,
                               UserRepository userRepository) {
        this.exerciseService = exerciseService;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // List exercises for a category
    @GetMapping("/category/{categoryId}")
    public String listByCategory(@PathVariable Long categoryId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            Category category = categoryService.findByIdWithUser(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            // Check authorization
            Long categoryUserId = category.getUser() != null ? category.getUser().getId() : null;
            if (categoryUserId == null || !categoryUserId.equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Not authorized");
                return "redirect:/categories";
            }

            List<Exercise> exercises = exerciseService.findByCategoryId(categoryId);
            model.addAttribute("category", category);
            model.addAttribute("exercises", exercises);
            return "exercises/exercises";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }

    // Show create form
    @GetMapping("/new")
    public String showCreateForm(@RequestParam Long categoryId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            Category category = categoryService.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            if (!category.getUser().getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Not authorized");
                return "redirect:/categories";
            }
            model.addAttribute("exercise", new Exercise());
            model.addAttribute("category", category);
            return "exercises/exercise-form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }

    // Handle create
    @PostMapping
    public String createExercise(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam Long categoryId,
                                 @RequestParam(required = false, defaultValue = "exercises") String redirect,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            exerciseService.create(name, description, categoryId, userId);
            redirectAttributes.addFlashAttribute("success", "Uebung erstellt!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if ("categories".equals(redirect)) {
            return "redirect:/categories";
        }
        return "redirect:/exercises/category/" + categoryId;
    }

    // Show edit form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            Exercise exercise = exerciseService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Exercise not found"));
            if (!exercise.getCategory().getUser().getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Not authorized");
                return "redirect:/categories";
            }
            model.addAttribute("exercise", exercise);
            return "exercises/exercise-form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }

    // Handle update
    @PostMapping("/update/{id}")
    public String updateExercise(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            Long categoryId = exerciseService.findById(id)
                    .map(exercise -> exercise.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Exercise not found"));
            exerciseService.update(id, name, description, userId);
            redirectAttributes.addFlashAttribute("success", "Exercise updated successfully");
            return "redirect:/exercises/category/" + categoryId;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }

    // Delete exercise
    @PostMapping("/delete/{id}")
    public String deleteExercise(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            Long categoryId = exerciseService.findById(id)
                    .map(exercise -> exercise.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Exercise not found"));
            exerciseService.delete(id, userId);
            redirectAttributes.addFlashAttribute("success", "Exercise deleted successfully");
            return "redirect:/exercises/category/" + categoryId;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }
}
