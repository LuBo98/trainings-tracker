package com.trainingtracker.service;

import com.trainingtracker.entity.*;
import com.trainingtracker.repository.ExerciseRepository;
import com.trainingtracker.repository.WorkoutEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkoutService {

    private final WorkoutEntryRepository workoutEntryRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutService(WorkoutEntryRepository workoutEntryRepository,
                          ExerciseRepository exerciseRepository) {
        this.workoutEntryRepository = workoutEntryRepository;
        this.exerciseRepository = exerciseRepository;
    }

    public List<WorkoutEntry> findByExerciseId(Long exerciseId) {
        return workoutEntryRepository.findByExerciseIdOrderByWorkoutDateDesc(exerciseId);
    }

    public List<WorkoutEntry> findByExerciseId(Long exerciseId, Long userId) {
        return workoutEntryRepository.findByExerciseIdAndExerciseCategoryUserIdOrderByWorkoutDateDesc(exerciseId, userId);
    }

    public List<WorkoutEntry> findByUserId(Long userId) {
        return workoutEntryRepository.findByExerciseCategoryUserIdOrderByWorkoutDateDesc(userId);
    }

    public List<WorkoutEntry> getRecentWorkouts(Long userId) {
        return workoutEntryRepository.findByExerciseCategoryUserIdOrderByWorkoutDateDesc(userId);
    }

    public List<WorkoutEntry> findByUserIdAndDateRange(Long userId, LocalDate start, LocalDate end) {
        return workoutEntryRepository
                .findByExerciseCategoryUserIdAndWorkoutDateBetweenOrderByWorkoutDateDesc(userId, start, end);
    }

    public List<WorkoutEntry> findByExerciseIdAndDateRange(Long exerciseId, LocalDate start, LocalDate end) {
        return workoutEntryRepository
                .findByExerciseIdAndWorkoutDateBetweenOrderByWorkoutDateDesc(exerciseId, start, end);
    }

    @Transactional
    public WorkoutEntry addEntry(Long exerciseId, LocalDate workoutDate,
                                  int sets, int reps, double weight, String notes, int difficulty, Long userId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));

        if (!exercise.getCategory().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to log workouts for this exercise");
        }

        WorkoutEntry entry = new WorkoutEntry();
        entry.setExercise(exercise);
        entry.setWorkoutDate(workoutDate);
        entry.setSets(sets);
        entry.setReps(reps);
        entry.setWeight(weight);
        entry.setNotes(notes);
        entry.setDifficulty(difficulty);
        return workoutEntryRepository.save(entry);
    }

    @Transactional
    public WorkoutEntry updateEntry(Long id, LocalDate workoutDate,
                                     int sets, int reps, double weight, String notes, int difficulty, Long userId) {
        WorkoutEntry entry = workoutEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workout entry not found"));

        if (!entry.getExercise().getCategory().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to edit this entry");
        }

        entry.setWorkoutDate(workoutDate);
        entry.setSets(sets);
        entry.setReps(reps);
        entry.setWeight(weight);
        entry.setNotes(notes);
        entry.setDifficulty(difficulty);
        return workoutEntryRepository.save(entry);
    }

    @Transactional
    public void deleteEntry(Long id, Long userId) {
        WorkoutEntry entry = workoutEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workout entry not found"));

        if (!entry.getExercise().getCategory().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this entry");
        }

        workoutEntryRepository.delete(entry);
    }

    // Get last entry for an exercise (for auto-fill)
    public Optional<WorkoutEntry> getLastEntryByExerciseId(Long exerciseId, Long userId) {
        return workoutEntryRepository
                .findByExerciseIdAndExerciseCategoryUserIdOrderByWorkoutDateDesc(exerciseId, userId)
                .stream()
                .findFirst();
    }

    // Get last entries for all exercises in a category (for bulk auto-fill)
    public Map<Long, WorkoutEntry> getLastEntriesByCategory(Long categoryId, Long userId) {
        List<Exercise> exercises = exerciseRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId);
        Map<Long, WorkoutEntry> result = new HashMap<>();
        for (Exercise ex : exercises) {
            getLastEntryByExerciseId(ex.getId(), userId).ifPresent(entry -> result.put(ex.getId(), entry));
        }
        return result;
    }

    // Statistics methods
    public Map<String, Object> getExerciseStats(Long exerciseId, LocalDate start, LocalDate end) {
        List<WorkoutEntry> entries = findByExerciseIdAndDateRange(exerciseId, start, end);
        return computeStats(entries);
    }

    public Map<String, Object> getUserStats(Long userId, LocalDate start, LocalDate end) {
        List<WorkoutEntry> entries = findByUserIdAndDateRange(userId, start, end);
        return computeStats(entries);
    }

    public Map<String, Object> getCategoryStats(Long categoryId, Long userId, LocalDate start, LocalDate end) {
        List<WorkoutEntry> allEntries = workoutEntryRepository
                .findByExerciseCategoryIdOrderByWorkoutDateDesc(categoryId);

        List<WorkoutEntry> filtered = allEntries.stream()
                .filter(e -> !e.getWorkoutDate().isBefore(start) && !e.getWorkoutDate().isAfter(end))
                .collect(Collectors.toList());

        return computeStats(filtered);
    }

    // Chart data for the stats page
    public Map<String, Object> getChartData(String scope, Long exerciseId, Long categoryId,
                                            Long userId, LocalDate start, LocalDate end) {
        List<WorkoutEntry> entries;

        switch (scope) {
            case "exercise":
                entries = findByExerciseIdAndDateRange(exerciseId, start, end);
                break;
            case "category":
                entries = workoutEntryRepository
                        .findByExerciseCategoryIdOrderByWorkoutDateDesc(categoryId);
                entries = entries.stream()
                        .filter(e -> !e.getWorkoutDate().isBefore(start) && !e.getWorkoutDate().isAfter(end))
                        .collect(Collectors.toList());
                break;
            default:
                entries = findByUserIdAndDateRange(userId, start, end);
                break;
        }

        // Sort by date ascending for charts
        entries.sort(Comparator.comparing(WorkoutEntry::getWorkoutDate));

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", entries.stream()
                .map(e -> e.getWorkoutDate().toString().replace("-", "."))
                .collect(Collectors.toList()));
        chartData.put("volumes", entries.stream()
                .mapToDouble(e -> e.getWeight() * e.getSets() * e.getReps())
                .boxed().collect(Collectors.toList()));
        chartData.put("difficulties", entries.stream()
                .mapToInt(WorkoutEntry::getDifficulty)
                .boxed().collect(Collectors.toList()));
        chartData.put("weights", entries.stream()
                .mapToDouble(WorkoutEntry::getWeight)
                .boxed().collect(Collectors.toList()));
        chartData.put("sets", entries.stream()
                .mapToInt(WorkoutEntry::getSets)
                .boxed().collect(Collectors.toList()));
        chartData.put("reps", entries.stream()
                .mapToInt(WorkoutEntry::getReps)
                .boxed().collect(Collectors.toList()));

        // Category breakdown (doughnut chart data)
        if (!"exercise".equals(scope)) {
            Map<String, Long> catBreakdown = entries.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getExercise().getCategory().getName(),
                            Collectors.counting()
                    ));
            Map<String, Object> catChart = new HashMap<>();
            catChart.put("labels", new ArrayList<>(catBreakdown.keySet()));
            catChart.put("data", new ArrayList<>(catBreakdown.values()));
            chartData.put("categoryBreakdown", catChart);
        }

        return chartData;
    }

    private Map<String, Object> computeStats(List<WorkoutEntry> entries) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWorkouts", entries.size());

        if (entries.isEmpty()) {
            stats.put("totalSets", 0);
            stats.put("totalReps", 0);
            stats.put("totalVolume", 0.0);
            stats.put("avgWeight", 0.0);
            stats.put("maxWeight", 0.0);
            stats.put("uniqueDays", 0);
            stats.put("avgDifficulty", 0.0);
            return stats;
        }

        long totalSets = entries.stream().mapToLong(WorkoutEntry::getSets).sum();
        long totalReps = entries.stream()
                .mapToLong(e -> (long) e.getSets() * e.getReps()).sum();
        double totalVolume = entries.stream()
                .mapToDouble(e -> e.getWeight() * e.getSets() * e.getReps()).sum();
        double avgWeight = entries.stream().mapToDouble(WorkoutEntry::getWeight).average().orElse(0.0);
        double maxWeight = entries.stream().mapToDouble(WorkoutEntry::getWeight).max().orElse(0.0);
        long uniqueDays = entries.stream().map(WorkoutEntry::getWorkoutDate).distinct().count();
        double avgDifficulty = entries.stream().mapToDouble(WorkoutEntry::getDifficulty).average().orElse(0.0);

        stats.put("totalSets", totalSets);
        stats.put("totalReps", totalReps);
        stats.put("totalVolume", Math.round(totalVolume * 100.0) / 100.0);
        stats.put("avgWeight", Math.round(avgWeight * 100.0) / 100.0);
        stats.put("maxWeight", maxWeight);
        stats.put("uniqueDays", uniqueDays);
        stats.put("avgDifficulty", Math.round(avgDifficulty * 100.0) / 100.0);

        return stats;
    }
}
