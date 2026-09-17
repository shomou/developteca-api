package com.developteca.controller;

import com.developteca.dto.CategoryResponse;
import com.developteca.service.CategoryService;
import com.developteca.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService){
        this.categoryService = categoryService;
    }

    //============= LISTAR CATEGORIAS (público) ================
    @GetMapping
    public ResponseEntity<?> list (){
        try{
            List<CategoryResponse> categories = categoryService.listAll();
            return ResponseEntity.ok(new ApiResponse(true, "Categorias obtenidas", categories));
        } catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }

}
