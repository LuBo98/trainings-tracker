package com.trainingtracker.repository;

import com.trainingtracker.entity.WorkoutEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkoutEntryRepository extends JpaRepository<WorkoutEntry, Long> {
    List<WorkoutEntry> findByExerciseIdOrderByWorkoutDateDesc(Long exerciseId);
    List<WorkoutEntry> findByExerciseIdAndExerciseCategoryUserIdOrderByWorkoutDateDesc(Long exerciseId, Long userId);
    List<WorkoutEntry> findByExerciseIdAndWorkoutDateBetweenOrderByWorkoutDateDesc(
            Long exerciseId, LocalDate startDate, LocalDate endDate);
    List<WorkoutEntry> findByExerciseCategoryIdOrderByWorkoutDateDesc(Long categoryId);
    List<WorkoutEntry> findByExerciseCategoryUserIdOrderByWorkoutDateDesc(Long userId);
    List<WorkoutEntry> findByExerciseCategoryUserIdAndWorkoutDateBetweenOrderByWorkoutDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT we FROM WorkoutEntry we JOIN FETCH we.exercise e JOIN FETCH e.category c " +
           "WHERE e.id = :exerciseId ORDER BY we.workoutDate DESC")
    List<WorkoutEntry> findWithExerciseAndCategory(@Param("exerciseId") Long exerciseId);

    @Query("SELECT MAX(we.weight) FROM WorkoutEntry we WHERE we.exercise.id = :exerciseId")
    Double findMaxWeightByExerciseId(@Param("exerciseId") Long exerciseId);

    @Query("SELECT COUNT(we) FROM WorkoutEntry we WHERE we.exercise.id = :exerciseId AND we.workoutDate BETWEEN :start AND :end")
    Long countByExerciseIdAndDateRange(@Param("exerciseId") Long exerciseId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);
}
