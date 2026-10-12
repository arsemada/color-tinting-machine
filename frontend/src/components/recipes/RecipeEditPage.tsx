"use client";

import { RecipeEditor } from "@/components/recipes/RecipeEditor";
import { useRecipe } from "@/hooks/use-api";

export function RecipeEditPage({ id }: { id: string }) {
  const recipe = useRecipe(id);
  if (recipe.isLoading) {
    return <p className="text-sm text-muted">Loading recipe…</p>;
  }
  if (recipe.isError || !recipe.data) {
    return <p className="text-sm text-danger">This recipe could not be loaded.</p>;
  }
  return <RecipeEditor recipe={recipe.data} />;
}
