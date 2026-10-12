import { RecipeList } from "@/components/recipes/RecipeList";

export default function RecipesPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold">Recipes</h1>
        <p className="text-sm text-muted">Saved formulas stay in PostgreSQL. Editing a recipe does not change a batch that has already started.</p>
      </div>
      <RecipeList />
    </div>
  );
}
