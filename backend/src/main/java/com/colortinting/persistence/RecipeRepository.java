package com.colortinting.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<RecipeEntity, UUID> {
    Optional<RecipeEntity> findByNameIgnoreCase(String name);

    List<RecipeEntity> findByNameContainingIgnoreCaseOrderByUpdatedAtDesc(String name);

    List<RecipeEntity> findAllByOrderByUpdatedAtDesc();
}
