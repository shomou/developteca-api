package com.developteca.service;

import com.developteca.dto.CategoryResponse;
import com.developteca.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository){
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> listAll(){
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription()))
                .collect(Collectors.toList());
    }
}
