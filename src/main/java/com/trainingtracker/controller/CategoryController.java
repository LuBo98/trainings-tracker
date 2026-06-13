package com.trainingtracker.controller;

import com.trainingtracker.service.CategoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trainingtracker.entity.Category;
import com.trainingtracker.repository.UserRepository;
import com.trainingtracker.entity.User;

import java.util.List;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public CategoryController(CategoryService categoryService,
                              UserRepository userRepository) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // List all categories
    @GetMapping
    public String listCategories(@AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        Long userId = getCurrentUserId(userDetails);
        List<Category> categories = categoryService.findByUserId(userId);
        model.addAttribute("categories", categories);
        return "categories/categories";
    }

    // Show create form
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        return "categories/category-form";
    }

    // Handle create
    @PostMapping
    public String createCategory(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            categoryService.create(name, description, userId);
            redirectAttributes.addFlashAttribute("success", "Category created successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    // Show edit form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            Category category = categoryService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            if (!category.getUser().getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Not authorized");
                return "redirect:/categories";
            }
            model.addAttribute("category", category);
            return "categories/category-form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }

    // Handle update
    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            categoryService.update(id, name, description, userId);
            redirectAttributes.addFlashAttribute("success", "Category updated successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    // Delete category
    @DeleteMapping("/delete/{id}")
    public String deleteCategoryDelete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(userDetails);
            categoryService.delete(id, userId);
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    // Delete category (POST fallback for form-based deletion)
    @PostMapping("/delete/{id}")
    public String deleteCategoryPost(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        return deleteCategoryDelete(id, userDetails, redirectAttributes);
    }
}
