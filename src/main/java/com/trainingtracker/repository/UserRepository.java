package com.trainingtracker.repository;

import com.trainingtracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
    List<User> findByResetTokenNotNullAndResetTokenExpiryAfter(LocalDateTime now);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
