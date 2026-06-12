package com.trainingtracker.service;

import com.trainingtracker.entity.Category;
import com.trainingtracker.entity.Exercise;
import com.trainingtracker.entity.User;
import com.trainingtracker.repository.CategoryRepository;
import com.trainingtracker.repository.ExerciseRepository;
import com.trainingtracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ExerciseService(ExerciseRepository exerciseRepository,
                           CategoryRepository categoryRepository,
                           UserRepository userRepository) {
        this.exerciseRepository = exerciseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<Exercise> findByCategoryId(Long categoryId) {
        return exerciseRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId);
    }

    public List<Exercise> findByUserId(Long userId) {
        return exerciseRepository.findByCategoryUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Exercise> findById(Long id) {
        return exerciseRepository.findById(id);
    }

    @Transactional
    public Exercise create(String name, String description, Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to add exercises to this category");
        }

        if (exerciseRepository.existsByNameAndCategoryId(name, categoryId)) {
            throw new RuntimeException("Exercise with this name already exists in this category");
        }

        Exercise exercise = new Exercise();
        exercise.setName(name);
        exercise.setDescription(description);
        exercise.setCategory(category);
        return exerciseRepository.save(exercise);
    }

    @Transactional
    public Exercise update(Long id, String name, String description, Long userId) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));

        if (!exercise.getCategory().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this exercise");
        }

        if (!name.equals(exercise.getName()) &&
            exerciseRepository.existsByNameAndCategoryId(name, exercise.getCategory().getId())) {
            throw new RuntimeException("Exercise with this name already exists in this category");
        }

        exercise.setName(name);
        exercise.setDescription(description);
        return exerciseRepository.save(exercise);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));

        if (!exercise.getCategory().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this exercise");
        }

        exerciseRepository.delete(exercise);
    }
}
