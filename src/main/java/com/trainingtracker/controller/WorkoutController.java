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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.HashMap;
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

    // Workout overview page - grouped by category and date
    @GetMapping
    public String showWorkouts(@AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        Long userId = getCurrentUserId(userDetails);
        // Group by category, then by date - each "workout" = all entries of one day in one category
        Map<Category, Map<LocalDate, List<com.trainingtracker.entity.WorkoutEntry>>> workoutsByCategory = 
                workoutService.getWorkoutsByCategoryAndDate(userId);
        
        model.addAttribute("workoutsByCategory", workoutsByCategory);
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
        // Auto-fill with last values
        workoutService.getLastEntryByExerciseId(exerciseId, userId).ifPresent(last -> {
            model.addAttribute("lastSets", last.getSets());
            model.addAttribute("lastReps", last.getReps());
            model.addAttribute("lastWeight", last.getWeight());
        });
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

    // EDIT entire workout (all entries of one day in one category) - show form
    @GetMapping("/edit")
    public String showEditWorkout(@RequestParam Long categoryId,
                                  @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        Long userId = getCurrentUserId(userDetails);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!category.getUser().getId().equals(userId)) {
            return "redirect:/workouts";
        }
        List<com.trainingtracker.entity.WorkoutEntry> entries = 
                workoutService.getWorkoutEntries(categoryId, date, userId);
        
        // Create a map of exercise ID to entry for easy lookup
        Map<Long, com.trainingtracker.entity.WorkoutEntry> entryMap = new HashMap<>();
        for (com.trainingtracker.entity.WorkoutEntry e : entries) {
            entryMap.put(e.getExercise().getId(), e);
        }
        
        model.addAttribute("category", category);
        model.addAttribute("workoutDate", date);
        model.addAttribute("entries", entries);
        model.addAttribute("entryMap", entryMap);
        model.addAttribute("exercises", category.getExercises());
        return "workouts/edit-workout";
    }

    // Handle edit entire workout
    @PostMapping("/edit")
    public String editWorkout(@RequestParam Long categoryId,
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

            // Get existing entries for this workout
            List<com.trainingtracker.entity.WorkoutEntry> existingEntries = 
                    workoutService.getWorkoutEntries(categoryId, workoutDate, userId);
            Map<Long, com.trainingtracker.entity.WorkoutEntry> entryMap = new HashMap<>();
            for (com.trainingtracker.entity.WorkoutEntry e : existingEntries) {
                entryMap.put(e.getExercise().getId(), e);
            }

            int count = 0;
            for (com.trainingtracker.entity.Exercise exercise : category.getExercises()) {
                String setsStr = allParams.get("sets_" + exercise.getId());
                String repsStr = allParams.get("reps_" + exercise.getId());
                String weightStr = allParams.get("weight_" + exercise.getId());
                String diffStr = allParams.get("diff_" + exercise.getId());

                if (setsStr != null && !setsStr.isEmpty() && repsStr != null && !repsStr.isEmpty()) {
                    int sets = Integer.parseInt(setsStr);
                    int reps = Integer.parseInt(repsStr);
                    double weight = (weightStr != null && !weightStr.isEmpty()) ? Double.parseDouble(weightStr) : 0;
                    int difficulty = (diffStr != null && !diffStr.isEmpty()) ? Integer.parseInt(diffStr) : 3;

                    com.trainingtracker.entity.WorkoutEntry existing = entryMap.get(exercise.getId());
                    if (existing != null) {
                        // Update existing entry
                        workoutService.updateEntry(existing.getId(), workoutDate, sets, reps, weight, null, difficulty, userId);
                    } else {
                        // Add new entry for this exercise
                        workoutService.addEntry(exercise.getId(), workoutDate, sets, reps, weight, null, difficulty, userId);
                    }
                    count++;
                }
            }

            redirectAttributes.addFlashAttribute("success", "Workout fuer '" + category.getName() + "' aktualisiert!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler: " + e.getMessage());
        }
        return "redirect:/workouts";
    }

    // Delete entire workout (all entries of one day in one category)
    @GetMapping("/delete")
    public String deleteWorkout(@RequestParam Long categoryId,
                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            workoutService.deleteWorkout(categoryId, date, userId);
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
                               HttpSession session,
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
        
        // Auto-fill with last values
        Map<Long, com.trainingtracker.entity.WorkoutEntry> lastEntries = 
                workoutService.getLastEntriesByCategory(categoryId, userId);
        model.addAttribute("lastEntries", lastEntries);
        
        // Restore draft from session if exists
        Map<String, Object> draft = (Map<String, Object>) session.getAttribute("bulkDraft_" + categoryId);
        if (draft != null) {
            model.addAttribute("draft", draft);
        }
        
        return "workouts/bulk-workout";
    }

    // Handle bulk workout submission
    @PostMapping("/bulk")
    public String logBulkWorkout(@RequestParam Long categoryId,
                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate workoutDate,
                                 @RequestParam Map<String, String> allParams,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 HttpSession session,
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

            // Clear draft from session
            session.removeAttribute("bulkDraft_" + categoryId);
            
            redirectAttributes.addFlashAttribute("success", "Workout fuer '" + category.getName() + "' eingetragen!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler: " + e.getMessage());
        }
        return "redirect:/workouts";
    }

    // Save draft to session (AJAX call)
    @PostMapping("/bulk/draft")
    public String saveDraft(@RequestParam Long categoryId,
                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate workoutDate,
                            @RequestParam Map<String, String> allParams,
                            HttpSession session) {
        Map<String, Object> draft = new HashMap<>();
        draft.put("workoutDate", workoutDate.toString());
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            draft.put(entry.getKey(), entry.getValue());
        }
        session.setAttribute("bulkDraft_" + categoryId, draft);
        return "redirect:/workouts/bulk?categoryId=" + categoryId;
    }

    // Add new exercise to category from bulk workout page
    @PostMapping("/bulk/add-exercise")
    public String addExerciseToCategory(@RequestParam Long categoryId,
                                        @RequestParam String exerciseName,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            if (!category.getUser().getId().equals(userId)) {
                throw new RuntimeException("Not authorized");
            }
            if (exerciseRepository.existsByNameAndCategoryId(exerciseName, categoryId)) {
                throw new RuntimeException("Uebung existiert bereits in dieser Kategorie");
            }
            com.trainingtracker.entity.Exercise exercise = new com.trainingtracker.entity.Exercise();
            exercise.setName(exerciseName);
            exercise.setCategory(category);
            exerciseRepository.save(exercise);
            redirectAttributes.addFlashAttribute("success", "Uebung '" + exerciseName + "' hinzugefuegt!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler: " + e.getMessage());
        }
        return "redirect:/workouts/bulk?categoryId=" + categoryId;
    }
}
