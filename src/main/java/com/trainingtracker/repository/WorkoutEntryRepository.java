package com.trainingtracker.repository;

import com.trainingtracker.entity.WorkoutEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    // 1RM Verlauf: Gewicht * (1 + reps/30) nach Epley-Formel
    @Query("SELECT new map(we.workoutDate as date, ROUND(we.weight * (1 + we.reps / 30.0), 2) as oneRM) " +
           "FROM WorkoutEntry we WHERE we.exercise.id = :exerciseId AND we.workoutDate BETWEEN :start AND :end ORDER BY we.workoutDate ASC")
    List<Map<String, Object>> findOneRMSeries(@Param("exerciseId") Long exerciseId,
                                              @Param("start") LocalDate start,
                                              @Param("end") LocalDate end);

    // Workout Delta: Letzter Eintrag für eine Übung (schnellste Query)
    @Query("SELECT new map(we.id as id, we.workoutDate as date, we.sets as sets, we.reps as reps, " +
           "we.weight as weight, we.difficulty as difficulty, we.notes as notes) " +
           "FROM WorkoutEntry we WHERE we.exercise.id = :exerciseId AND we.exercise.category.user.id = :userId " +
           "ORDER BY we.workoutDate DESC, we.id DESC")
    List<Map<String, Object>> findLastEntryForExercise(@Param("exerciseId") Long exerciseId,
                                                       @Param("userId") Long userId);

    // Gesamtvolumen pro Tag für eine Übung
    @Query("SELECT new map(we.workoutDate as date, ROUND(we.sets * we.reps * we.weight, 2) as volume) " +
           "FROM WorkoutEntry we WHERE we.exercise.id = :exerciseId AND we.workoutDate BETWEEN :start AND :end ORDER BY we.workoutDate ASC")
    List<Map<String, Object>> findVolumeSeries(@Param("exerciseId") Long exerciseId,
                                               @Param("start") LocalDate start,
                                               @Param("end") LocalDate end);

    // RPE + Gewicht pro Tag für eine Übung
    @Query("SELECT new map(we.workoutDate as date, ROUND(we.weight, 2) as weight, we.difficulty as rpe) " +
           "FROM WorkoutEntry we WHERE we.exercise.id = :exerciseId AND we.workoutDate BETWEEN :start AND :end ORDER BY we.workoutDate ASC")
    List<Map<String, Object>> findRpeWeightSeries(@Param("exerciseId") Long exerciseId,
                                                  @Param("start") LocalDate start,
                                                  @Param("end") LocalDate end);
}
