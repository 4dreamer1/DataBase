package dev.usr.database.service;

import dev.usr.database.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> findAll();
    
    Category findById(Long id);
    
    Category findByName(String name);
    
    Category save(Category category);
    
    Category update(Long id, Category category);
    
    void delete(Long id);
}