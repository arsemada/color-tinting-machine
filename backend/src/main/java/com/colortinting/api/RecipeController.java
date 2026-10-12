package com.colortinting.api;

import com.colortinting.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {
    private final RecipeService recipes;
    private final ApiPresenter presenter;

    public RecipeController(RecipeService recipes, ApiPresenter presenter) {
        this.recipes = recipes;
        this.presenter = presenter;
    }

    @GetMapping
    public List<ApiPresenter.RecipeResponse> list(@RequestParam(name = "q", required = false) String query) {
        return recipes.list(query).stream().map(presenter::recipe).toList();
    }

    @GetMapping("/{id}")
    public ApiPresenter.RecipeResponse get(@PathVariable UUID id) {
        return presenter.recipe(recipes.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiPresenter.RecipeResponse create(@Valid @RequestBody Requests.RecipeBody body) {
        return presenter.recipe(recipes.create(toDraft(body)));
    }

    @PutMapping("/{id}")
    public ApiPresenter.RecipeResponse update(@PathVariable UUID id, @Valid @RequestBody Requests.RecipeBody body) {
        return presenter.recipe(recipes.update(id, toDraft(body)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        recipes.delete(id);
    }

    private static RecipeService.RecipeDraft toDraft(Requests.RecipeBody body) {
        return new RecipeService.RecipeDraft(
                body.name(),
                body.description(),
                body.targetColor(),
                body.cyanDose(),
                body.magentaDose(),
                body.yellowDose(),
                body.blackDose(),
                body.doseUnit(),
                body.basePaintQuantity()
        );
    }
}
