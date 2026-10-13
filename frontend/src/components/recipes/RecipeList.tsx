"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRecipes } from "@/hooks/use-api";
import { selectRecipe, selectedRecipeId } from "@/hooks/use-api";
import { ApiError } from "@/lib/api/client";
import { apiRequest } from "@/lib/api/client";
import { formatDose } from "@/lib/utils/quantity";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { useQueryClient } from "@tanstack/react-query";
import {
  Search,
  Plus,
  Check,
  Pencil,
  Trash2,
  Droplets,
  CheckCircle2,
} from "lucide-react";

export function RecipeList() {
  const [query, setQuery] = useState("");
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const recipes = useRecipes(query.trim());

  useEffect(() => {
    const sync = () => setSelectedId(selectedRecipeId());
    sync();
    window.addEventListener("ctm-recipe", sync);
    return () => window.removeEventListener("ctm-recipe", sync);
  }, []);
  const client = useQueryClient();

  return (
    <div className="space-y-5">
      {/* Page Title & Search Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Color Formulations</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Manage your CMYK paint recipes and select targets for dispensing.
          </p>
        </div>
        <Link
          href="/recipes/new"
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-b from-teal-600 to-teal-700 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:from-teal-700 hover:to-teal-800 transition active:scale-[0.98]"
        >
          <Plus size={16} />
          Create Recipe
        </Link>
      </div>

      {/* Search Input Bar */}
      <div className="relative">
        <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          className="w-full rounded-xl border border-slate-200/90 bg-white pl-10 pr-4 py-2.5 text-sm text-slate-900 shadow-xs focus:border-teal-600 focus:outline-none placeholder:text-slate-400"
          placeholder="Search recipes by name or color..."
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          aria-label="Search recipes"
        />
      </div>

      {recipes.isError ? (
        <p className="rounded-xl bg-rose-50 border border-rose-200 p-3 text-sm text-rose-700">
          Recipes could not be loaded from the database.
        </p>
      ) : null}

      {/* Elevated Recipes Table */}
      <div className="overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-xs">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-slate-100 bg-slate-50/70 text-xs font-semibold uppercase tracking-wider text-slate-500">
            <tr>
              <th className="px-5 py-3.5">Recipe & Formula</th>
              <th className="px-4 py-3.5">Target Swatch</th>
              <th className="px-4 py-3.5">CMYK Doses</th>
              <th className="px-4 py-3.5">Total Colorant</th>
              <th className="px-5 py-3.5 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {recipes.data?.length ? (
              recipes.data.map((recipe) => {
                const isSelected = selectedId === recipe.id;
                return (
                  <tr
                    key={recipe.id}
                    className={`transition-colors hover:bg-slate-50/70 ${
                      isSelected ? "bg-teal-50/40" : ""
                    }`}
                  >
                    {/* Name & Desc */}
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        <p className="font-bold text-slate-900">{recipe.name}</p>
                        {isSelected ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-teal-100 px-2 py-0.5 text-[10px] font-bold text-teal-800">
                            <CheckCircle2 size={11} />
                            Active
                          </span>
                        ) : null}
                      </div>
                      {recipe.description ? (
                        <p className="mt-0.5 text-xs text-slate-500 line-clamp-1">{recipe.description}</p>
                      ) : null}
                    </td>

                    {/* Color Swatch */}
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-2.5">
                        <div
                          className="h-7 w-7 rounded-lg border border-slate-200 shadow-xs shrink-0"
                          style={{ background: recipe.targetColor }}
                        />
                        <span className="font-mono text-xs font-semibold text-slate-700">
                          {recipe.targetColor}
                        </span>
                      </div>
                    </td>

                    {/* CMYK Chips */}
                    <td className="px-4 py-4">
                      <div className="flex flex-wrap gap-1 text-[11px] font-mono">
                        <span className="rounded bg-cyan-100/90 text-cyan-900 px-1.5 py-0.5 font-semibold">
                          C {formatDose(recipe.cyanDose)}
                        </span>
                        <span className="rounded bg-pink-100/90 text-pink-900 px-1.5 py-0.5 font-semibold">
                          M {formatDose(recipe.magentaDose)}
                        </span>
                        <span className="rounded bg-amber-100/90 text-amber-900 px-1.5 py-0.5 font-semibold">
                          Y {formatDose(recipe.yellowDose)}
                        </span>
                        <span className="rounded bg-slate-200 text-slate-900 px-1.5 py-0.5 font-semibold">
                          K {formatDose(recipe.blackDose)}
                        </span>
                      </div>
                    </td>

                    {/* Total Grams */}
                    <td className="px-4 py-4 font-semibold text-slate-800">
                      {formatDose(recipe.totalColorant)}
                    </td>

                    {/* Action Buttons */}
                    <td className="px-5 py-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          type="button"
                          tone={isSelected ? "primary" : "secondary"}
                          onClick={() => selectRecipe(recipe.id)}
                          className="px-3 py-1.5 text-xs font-semibold"
                        >
                          {isSelected ? (
                            <>
                              <Check size={13} />
                              Selected
                            </>
                          ) : (
                            "Select"
                          )}
                        </Button>
                        <Link
                          className="inline-flex items-center justify-center rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-50 transition"
                          href={`/recipes/${recipe.id}`}
                          aria-label={`Edit ${recipe.name}`}
                        >
                          <Pencil size={13} />
                        </Link>
                        <Button
                          type="button"
                          tone="danger"
                          onClick={() => setPendingDelete(recipe.id)}
                          className="px-2.5 py-1.5 text-xs"
                          aria-label={`Delete ${recipe.name}`}
                        >
                          <Trash2 size={13} />
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td className="px-5 py-12 text-center text-slate-400" colSpan={5}>
                  <div className="flex flex-col items-center justify-center">
                    <Droplets size={32} className="mb-2 text-slate-300" />
                    <p className="text-sm font-semibold text-slate-600">
                      {recipes.isLoading ? "Loading recipes..." : query.trim() ? "No matching recipes found." : "No recipes stored yet."}
                    </p>
                    <p className="mt-1 text-xs text-slate-400">
                      {query.trim() ? "Try searching for a different keyword." : "Create your first color formula to get started."}
                    </p>
                  </div>
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
        confirmLabel="Delete recipe"
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
