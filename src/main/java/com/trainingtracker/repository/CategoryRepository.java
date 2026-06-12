package com.trainingtracker.repository;

import com.trainingtracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByNameAndUserId(String name, Long userId);

    @Query("SELECT c FROM Category c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Category> findByIdWithUser(@org.springframework.data.repository.query.Param("id") Long id);
}
