package dev.usr.database.service.impl;

import dev.usr.database.entity.Category;
import dev.usr.database.repository.CategoryRepository;
import dev.usr.database.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("未找到分类: " + id));
    }

    @Override
    public Category findByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("未找到分类: " + name));
    }

    @Override
    @Transactional
    public Category save(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("分类名称已存在: " + category.getName());
        }
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category update(Long id, Category category) {
        Category existingCategory = findById(id);
        
        if (!existingCategory.getName().equals(category.getName()) && 
                categoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("分类名称已存在: " + category.getName());
        }
        
        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        
        return categoryRepository.save(existingCategory);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        
        if (category.getEquipments() != null && !category.getEquipments().isEmpty()) {
            throw new IllegalStateException("无法删除分类，因为它包含装备");
        }
        
        categoryRepository.delete(category);
    }
} 