package com.trainingtracker.repository;

import com.trainingtracker.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);
    List<Exercise> findByCategoryUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByNameAndCategoryId(String name, Long categoryId);
}
