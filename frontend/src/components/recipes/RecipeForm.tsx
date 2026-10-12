"use client";

import { Field, inputClass } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { emptyRecipe, recipeSchema, type RecipeFormValues } from "@/lib/validation/recipe";
import { sumDoses } from "@/lib/utils/quantity";
import { normalizeHex } from "@/lib/utils/color";
import type { Recipe, Settings } from "@/lib/api/types";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMemo } from "react";
import { useForm } from "react-hook-form";

export function RecipeForm({
  settings,
  initial,
  submitting,
  serverErrors,
  onSubmit,
}: {
  settings?: Settings;
  initial?: Recipe;
  submitting?: boolean;
  serverErrors?: { field: string; message: string }[];
  onSubmit: (values: RecipeFormValues) => Promise<void>;
}) {
  const schema = useMemo(
    () =>
      recipeSchema(
        settings?.softwareLimits ?? {
          maxChannelDoseGrams: "500.000",
          maxTotalDoseGrams: "2000.000",
        },
      ),
    [settings?.softwareLimits],
  );
  const form = useForm<RecipeFormValues>({
    resolver: zodResolver(schema),
    defaultValues: initial
      ? {
          name: initial.name,
          description: initial.description ?? "",
          targetColor: initial.targetColor,
          cyanDose: strip(initial.cyanDose),
          magentaDose: strip(initial.magentaDose),
          yellowDose: strip(initial.yellowDose),
          blackDose: strip(initial.blackDose),
          doseUnit: "GRAMS",
          basePaintQuantity: initial.basePaintQuantity ? strip(initial.basePaintQuantity) : "",
        }
      : emptyRecipe,
  });
  const values = form.watch();
  const total = sumDoses([values.cyanDose, values.magentaDose, values.yellowDose, values.blackDose]);
  const preview = normalizeHex(values.targetColor ?? "");
  const localErrors = form.formState.errors;

  return (
    <form
      className="space-y-4"
      onSubmit={form.handleSubmit(async (input) => {
        await onSubmit({ ...input, targetColor: normalizeHex(input.targetColor) ?? input.targetColor });
      })}
    >
      <div className="grid gap-4 md:grid-cols-[180px_1fr]">
        <div>
          <div className="h-28 rounded-lg border border-line" style={{ background: preview ?? "transparent" }} />
          <p className="mt-2 text-xs text-muted">Screen preview only. It does not determine a paint formula.</p>
        </div>
        <div className="grid gap-3">
          <Field label="Name" error={message(localErrors.name?.message, serverErrors, "name")}>
            <input className={inputClass} {...form.register("name")} />
          </Field>
          <Field label="Description" error={message(localErrors.description?.message, serverErrors, "description")}>
            <textarea className={inputClass} rows={3} {...form.register("description")} />
          </Field>
          <Field label="Target color" error={message(localErrors.targetColor?.message, serverErrors, "targetColor")}>
            <div className="flex gap-2">
              <input
                type="color"
                aria-label="Color picker"
                className="h-10 w-14 rounded border border-line"
                value={preview ?? "#208094"}
                onChange={(event) => form.setValue("targetColor", event.target.value.toUpperCase(), { shouldValidate: true })}
              />
              <input className={inputClass} {...form.register("targetColor")} />
              <Button type="button" onClick={() => form.setValue("targetColor", "#808080", { shouldValidate: true })}>
                Clear preview
              </Button>
            </div>
          </Field>
        </div>
      </div>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {([
          ["cyanDose", "Cyan"],
          ["magentaDose", "Magenta"],
          ["yellowDose", "Yellow"],
          ["blackDose", "Black"],
        ] as const).map(([name, label]) => (
          <Field key={name} label={`${label} (g)`} error={message(localErrors[name]?.message, serverErrors, name)}>
            <input className={inputClass} inputMode="decimal" {...form.register(name)} />
          </Field>
        ))}
      </div>
      <Field
        label="Base paint quantity (g, optional)"
        error={message(localErrors.basePaintQuantity?.message, serverErrors, "basePaintQuantity")}
        hint="Not part of the dispensed colorant total."
      >
        <input className={inputClass} inputMode="decimal" {...form.register("basePaintQuantity")} />
      </Field>
      <p className="text-sm">Total requested colorant: {total ? `${total} g` : "Enter valid doses"}</p>
      {extraMessage(localErrors, "doses") || serverErrors?.find((error) => error.field === "doses") ? (
        <p className="text-sm font-medium text-danger">
          {extraMessage(localErrors, "doses") ?? serverErrors?.find((error) => error.field === "doses")?.message}
        </p>
      ) : null}
      {extraMessage(localErrors, "totalDose") ? (
        <p className="text-sm font-medium text-danger">{extraMessage(localErrors, "totalDose")}</p>
      ) : null}
      <p className="text-xs text-muted">{settings?.softwareLimits.note}</p>
      <Button type="submit" tone="primary" disabled={submitting}>
        {submitting ? "Saving…" : "Save recipe"}
      </Button>
    </form>
  );
}

function strip(value: string): string {
  return value.replace(/0+$/, "").replace(/\.$/, "");
}

function extraMessage(errors: object, field: string): string | undefined {
  const value = (errors as Record<string, { message?: string } | undefined>)[field];
  return typeof value?.message === "string" ? value.message : undefined;
}

function message(local: string | undefined, server: { field: string; message: string }[] | undefined, field: string) {
  return local ?? server?.find((error) => error.field === field)?.message;
}
