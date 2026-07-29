package com.example.mef.demo.Model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, String> {

    Optional<Ingredient> findByName(String name);

}