"use client";

import { RecipeForm } from "@/components/recipes/RecipeForm";
import { useSettings } from "@/hooks/use-api";
import { ApiError, apiRequest } from "@/lib/api/client";
import type { Recipe, RecipeInput } from "@/lib/api/types";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";

export function RecipeEditor({ recipe }: { recipe?: Recipe }) {
  const settings = useSettings();
  const router = useRouter();
  const client = useQueryClient();
  const [pending, setPending] = useState(false);
  const [errors, setErrors] = useState<{ field: string; message: string }[]>([]);
  const [message, setMessage] = useState<string | null>(null);

  async function save(values: RecipeInput) {
    setPending(true);
    setErrors([]);
    setMessage(null);
    try {
      const saved = await apiRequest<Recipe>(recipe ? `/api/v1/recipes/${recipe.id}` : "/api/v1/recipes", {
        method: recipe ? "PUT" : "POST",
        body: JSON.stringify({
          ...values,
          description: values.description || null,
          basePaintQuantity: values.basePaintQuantity || null,
        }),
      });
      await client.invalidateQueries({ queryKey: ["recipes"] });
      router.push(`/recipes/${saved.id}`);
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors(error.fieldErrors);
        setMessage(error.message);
      } else {
        setMessage("The recipe could not be saved.");
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="rounded-xl border border-line bg-white p-4 shadow-[var(--shadow)]">
      <h1 className="mb-4 text-xl font-semibold">{recipe ? "Edit recipe" : "Create recipe"}</h1>
      {message ? <p className="mb-3 text-sm text-danger">{message}</p> : null}
      <RecipeForm
        settings={settings.data}
        initial={recipe}
        submitting={pending}
        serverErrors={errors}
        onSubmit={save}
      />
    </div>
  );
}
