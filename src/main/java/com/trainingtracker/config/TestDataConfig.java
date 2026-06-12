package com.trainingtracker.config;

import com.trainingtracker.entity.User;
import com.trainingtracker.entity.Exercise;
import com.trainingtracker.entity.Category;
import com.trainingtracker.repository.UserRepository;
import com.trainingtracker.repository.ExerciseRepository;
import com.trainingtracker.repository.CategoryRepository;
import com.trainingtracker.repository.WorkoutEntryRepository;
import com.trainingtracker.service.WorkoutService;
import com.trainingtracker.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Configuration
public class TestDataConfig {

    @Bean
    public CommandLineRunner initData(WorkoutService workoutService, UserRepository userRepository,
                                      ExerciseRepository exerciseRepository,
                                      CategoryRepository categoryRepository,
                                      WorkoutEntryRepository workoutEntryRepository,
                                      UserService userService) {
        return args -> {
            // Only add test data if there are no workout entries yet
            if (workoutEntryRepository.count() > 0) {
                System.out.println("Workout entries already exist, skipping test data initialization");
                return;
            }

            // Create user if not exists
            User user = userRepository.findByUsername("max").orElse(null);
            if (user == null) {
                System.out.println("Creating test user 'max'...");
                try {
                    user = userService.register("max", "max@test.com", "max123");
                } catch (Exception e) {
                    System.out.println("Failed to create user: " + e.getMessage());
                    return;
                }
            }

            // Create category if not exists
            Category category = null;
            for (Category c : categoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
                if ("Oberkoerper".equals(c.getName())) {
                    category = c;
                    break;
                }
            }
            if (category == null) {
                System.out.println("Creating test category 'Oberkoerper'...");
                category = new Category();
                category.setName("Oberkoerper");
                category.setUser(user);
                category = categoryRepository.save(category);
            }

            // Create exercises if not exists
            String[] exerciseNames = {"Bankdruecken", "Klimmzuege", "Liegestuetze"};
            Map<String, Long> exerciseIds = new HashMap<>();
            for (String name : exerciseNames) {
                if (!exerciseRepository.existsByNameAndCategoryId(name, category.getId())) {
                    System.out.println("Creating exercise: " + name);
                    Exercise ex = new Exercise();
                    ex.setName(name);
                    ex.setCategory(category);
                    ex = exerciseRepository.save(ex);
                    exerciseIds.put(name.toLowerCase(), ex.getId());
                } else {
                    // Find existing exercise
                    List<Exercise> existing = exerciseRepository.findByCategoryIdOrderByCreatedAtDesc(category.getId());
                    for (Exercise ex : existing) {
                        if (name.equalsIgnoreCase(ex.getName())) {
                            exerciseIds.put(name.toLowerCase(), ex.getId());
                            break;
                        }
                    }
                }
            }

            System.out.println("Found exercises: " + exerciseIds.keySet());

            // Add some workout entries for testing
            Long bankdrueckenId = exerciseIds.get("bankdruecken");
            Long klimmzuegeId = exerciseIds.get("klimmzuege");
            Long liegestuetzeId = exerciseIds.get("liegestuetze");

            if (bankdrueckenId != null) {
                workoutService.addEntry(bankdrueckenId, LocalDate.of(2026, 6, 12), 4, 10, 80.0, "Gutes Training", 3, user.getId());
                workoutService.addEntry(bankdrueckenId, LocalDate.of(2026, 6, 11), 3, 12, 70.0, "", 2, user.getId());
                workoutService.addEntry(bankdrueckenId, LocalDate.of(2026, 6, 10), 4, 8, 90.0, "Schwer", 4, user.getId());
                workoutService.addEntry(bankdrueckenId, LocalDate.of(2026, 6, 9), 3, 10, 75.0, "", 3, user.getId());
                workoutService.addEntry(bankdrueckenId, LocalDate.of(2026, 6, 8), 3, 10, 75.0, "", 2, user.getId());
            }

            if (klimmzuegeId != null) {
                workoutService.addEntry(klimmzuegeId, LocalDate.of(2026, 6, 12), 3, 8, 0.0, "Korpergewicht", 3, user.getId());
                workoutService.addEntry(klimmzuegeId, LocalDate.of(2026, 6, 11), 4, 10, 0.0, "", 2, user.getId());
                workoutService.addEntry(klimmzuegeId, LocalDate.of(2026, 6, 10), 3, 12, 0.0, "", 2, user.getId());
                workoutService.addEntry(klimmzuegeId, LocalDate.of(2026, 6, 8), 3, 8, 0.0, "", 3, user.getId());
            }

            if (liegestuetzeId != null) {
                workoutService.addEntry(liegestuetzeId, LocalDate.of(2026, 6, 12), 3, 20, 0.0, "", 2, user.getId());
                workoutService.addEntry(liegestuetzeId, LocalDate.of(2026, 6, 11), 4, 15, 0.0, "", 2, user.getId());
                workoutService.addEntry(liegestuetzeId, LocalDate.of(2026, 6, 9), 3, 25, 0.0, "", 1, user.getId());
            }

            System.out.println("Test workout data initialized successfully - " + workoutEntryRepository.count() + " entries");
        };
    }
}
