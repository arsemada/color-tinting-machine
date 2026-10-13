import { RecipeList } from "@/components/recipes/RecipeList";

export default function RecipesPage() {
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">Recipes</h1>
      <RecipeList />
    </div>
  );
}
