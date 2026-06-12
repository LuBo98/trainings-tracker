package com.trainingtracker.service;

import com.trainingtracker.entity.Category;
import com.trainingtracker.entity.User;
import com.trainingtracker.repository.CategoryRepository;
import com.trainingtracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<Category> findByUserId(Long userId) {
        return categoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    public Optional<Category> findByIdWithUser(Long id) {
        return categoryRepository.findByIdWithUser(id);
    }

    @Transactional
    public Category create(String name, String description, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (categoryRepository.existsByNameAndUserId(name, userId)) {
            throw new RuntimeException("Category with this name already exists");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setUser(user);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, String name, String description, Long userId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this category");
        }

        if (!name.equals(category.getName()) &&
            categoryRepository.existsByNameAndUserId(name, userId)) {
            throw new RuntimeException("Category with this name already exists");
        }

        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this category");
        }

        categoryRepository.delete(category);
    }
}
