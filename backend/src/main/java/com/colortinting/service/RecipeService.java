package com.colortinting.service;

import com.colortinting.api.ApiException;
import com.colortinting.config.AppProperties;
import com.colortinting.domain.ModelEnums.DoseUnit;
import com.colortinting.domain.Quantities;
import com.colortinting.domain.RecipeRules;
import com.colortinting.persistence.RecipeEntity;
import com.colortinting.persistence.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class RecipeService {
    private final RecipeRepository recipes;
    private final AppProperties properties;
    private final Clock clock;

    public RecipeService(RecipeRepository recipes, AppProperties properties, Clock clock) {
        this.recipes = recipes;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RecipeEntity> list(String query) {
        if (query == null || query.isBlank()) {
            return recipes.findAllByOrderByUpdatedAtDesc();
        }
        return recipes.findByNameContainingIgnoreCaseOrderByUpdatedAtDesc(query.trim());
    }

    @Transactional(readOnly = true)
    public RecipeEntity get(UUID id) {
        return recipes.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public RecipeEntity create(RecipeDraft draft) {
        ParsedRecipe parsed = parse(draft, false);
        ensureUniqueName(parsed.name(), null);
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(UUID.randomUUID());
        recipe.setCreatedAt(clock.instant());
        apply(recipe, parsed);
        return recipes.save(recipe);
    }

    @Transactional
    public RecipeEntity update(UUID id, RecipeDraft draft) {
        RecipeEntity recipe = get(id);
        ParsedRecipe parsed = parse(draft, false);
        ensureUniqueName(parsed.name(), id);
        apply(recipe, parsed);
        return recipes.save(recipe);
    }

    @Transactional
    public void delete(UUID id) {
        if (!recipes.existsById(id)) {
            throw notFound(id);
        }
        recipes.deleteById(id);
    }

    public ParsedRecipe parseForDispense(RecipeEntity recipe) {
        return parse(RecipeDraft.from(recipe), true);
    }

    private ParsedRecipe parse(RecipeDraft draft, boolean requirePositiveDose) {
        List<Quantities.FieldViolation> errors = RecipeRules.validate(
                draft.name(),
                draft.description(),
                draft.targetColor(),
                draft.cyanDose(),
                draft.magentaDose(),
                draft.yellowDose(),
                draft.blackDose(),
                draft.doseUnit(),
                draft.basePaintQuantity(),
                properties.getMachine().getLimits(),
                requirePositiveDose
        );
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The recipe is invalid.", errors);
        }
        List<Quantities.FieldViolation> sink = new java.util.ArrayList<>();
        BigDecimal cyan = Quantities.parseRequired(draft.cyanDose(), "cyanDose", sink);
        BigDecimal magenta = Quantities.parseRequired(draft.magentaDose(), "magentaDose", sink);
        BigDecimal yellow = Quantities.parseRequired(draft.yellowDose(), "yellowDose", sink);
        BigDecimal black = Quantities.parseRequired(draft.blackDose(), "blackDose", sink);
        BigDecimal base = Quantities.parseOptional(draft.basePaintQuantity(), "basePaintQuantity", sink);
        return new ParsedRecipe(
                draft.name().trim(),
                blankToNull(draft.description()),
                draft.targetColor().trim().toUpperCase(),
                cyan,
                magenta,
                yellow,
                black,
                DoseUnit.GRAMS,
                base
        );
    }

    private void apply(RecipeEntity recipe, ParsedRecipe parsed) {
        recipe.setName(parsed.name());
        recipe.setDescription(parsed.description());
        recipe.setTargetColor(parsed.targetColor());
        recipe.setCyanDose(parsed.cyan());
        recipe.setMagentaDose(parsed.magenta());
        recipe.setYellowDose(parsed.yellow());
        recipe.setBlackDose(parsed.black());
        recipe.setDoseUnit(parsed.unit());
        recipe.setBasePaintQuantity(parsed.basePaintQuantity());
        recipe.setUpdatedAt(clock.instant());
    }

    private void ensureUniqueName(String name, UUID currentId) {
        recipes.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "RECIPE_NAME_CONFLICT",
                        "A recipe named \"" + name + "\" already exists."
                );
            }
        });
    }

    private static ApiException notFound(UUID id) {
        return new ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe " + id + " was not found.");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record RecipeDraft(
            String name,
            String description,
            String targetColor,
            String cyanDose,
            String magentaDose,
            String yellowDose,
            String blackDose,
            String doseUnit,
            String basePaintQuantity
    ) {
        public static RecipeDraft from(RecipeEntity recipe) {
            return new RecipeDraft(
                    recipe.getName(),
                    recipe.getDescription(),
                    recipe.getTargetColor(),
                    Quantities.format(recipe.getCyanDose()),
                    Quantities.format(recipe.getMagentaDose()),
                    Quantities.format(recipe.getYellowDose()),
                    Quantities.format(recipe.getBlackDose()),
                    recipe.getDoseUnit().name(),
                    Quantities.format(recipe.getBasePaintQuantity())
            );
        }
    }

    public record ParsedRecipe(
            String name,
            String description,
            String targetColor,
            BigDecimal cyan,
            BigDecimal magenta,
            BigDecimal yellow,
            BigDecimal black,
            DoseUnit unit,
            BigDecimal basePaintQuantity
    ) {
        public BigDecimal totalColorant() {
            return Quantities.sum(cyan, magenta, yellow, black);
        }
    }
}
