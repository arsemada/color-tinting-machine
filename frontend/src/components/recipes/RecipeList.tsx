"use client";

import { useState } from "react";
import Link from "next/link";
import { useRecipes } from "@/hooks/use-api";
import { selectRecipe } from "@/hooks/use-api";
import { ApiError } from "@/lib/api/client";
import { apiRequest } from "@/lib/api/client";
import { formatDose } from "@/lib/utils/quantity";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { useQueryClient } from "@tanstack/react-query";

export function RecipeList() {
  const [query, setQuery] = useState("");
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);
  const recipes = useRecipes(query.trim());
  const client = useQueryClient();

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <input
          className="w-full max-w-sm rounded-md border border-line bg-white px-3 py-2 text-sm"
          placeholder="Search recipes"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          aria-label="Search recipes"
        />
        <Link href="/recipes/new" className="rounded-md bg-accent px-3 py-2 text-sm font-medium text-white">
          Create recipe
        </Link>
      </div>
      {recipes.isError ? <p className="text-sm text-danger">Recipes could not be loaded.</p> : null}
      <div className="overflow-x-auto rounded-xl border border-line bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-line text-muted">
            <tr>
              <th className="px-3 py-2 font-medium">Recipe</th>
              <th className="px-3 py-2 font-medium">Target</th>
              <th className="px-3 py-2 font-medium">Total</th>
              <th className="px-3 py-2 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {recipes.data?.length ? (
              recipes.data.map((recipe) => (
                <tr key={recipe.id} className="border-b border-line last:border-0">
                  <td className="px-3 py-3">
                    <p className="font-medium">{recipe.name}</p>
                    <p className="text-xs text-muted">{recipe.description}</p>
                  </td>
                  <td className="px-3 py-3">
                    <span className="inline-flex items-center gap-2">
                      <span className="h-4 w-4 rounded border border-line" style={{ background: recipe.targetColor }} />
                      {recipe.targetColor}
                    </span>
                  </td>
                  <td className="px-3 py-3">{formatDose(recipe.totalColorant)}</td>
                  <td className="px-3 py-3">
                    <div className="flex flex-wrap gap-2">
                      <Button type="button" onClick={() => selectRecipe(recipe.id)}>
                        Select
                      </Button>
                      <Link className="rounded-md border border-line px-3 py-2" href={`/recipes/${recipe.id}`}>
                        Edit
                      </Link>
                      <Button type="button" tone="danger" onClick={() => setPendingDelete(recipe.id)}>
                        Delete
                      </Button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td className="px-3 py-6 text-muted" colSpan={4}>
                  {recipes.isLoading ? "Loading recipes…" : "No recipes yet."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <ConfirmDialog
        open={pendingDelete != null}
        title="Delete this recipe?"
        body="Historical batches keep their own snapshot. Deleting the recipe does not change records that already exist."
        confirmLabel="Delete"
        tone="danger"
        onClose={() => setPendingDelete(null)}
        onConfirm={() => {
          if (!pendingDelete) return;
          void apiRequest(`/api/v1/recipes/${pendingDelete}`, { method: "DELETE" })
            .then(async () => {
              if (window.localStorage.getItem("ctm.selectedRecipeId") === pendingDelete) {
                selectRecipe(null);
              }
              await client.invalidateQueries({ queryKey: ["recipes"] });
            })
            .catch((error: unknown) => {
              if (error instanceof ApiError) {
                window.alert(error.message);
              }
            });
        }}
      />
    </div>
  );
}
