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
import { Save, RotateCcw } from "lucide-react";

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
      className="space-y-6 rounded-2xl border border-slate-200/90 bg-white p-6 shadow-xs"
      onSubmit={form.handleSubmit(async (input) => {
        await onSubmit({ ...input, targetColor: normalizeHex(input.targetColor) ?? input.targetColor });
      })}
    >
      {/* Top Section: Color Swatch & Basic Info */}
      <div className="grid gap-6 md:grid-cols-[200px_1fr]">
        {/* Live Swatch Preview */}
        <div>
          <span className="mb-2 block text-xs font-semibold uppercase tracking-wider text-slate-500">
            Live Color Swatch
          </span>
          <div
            className="h-36 w-full rounded-2xl border border-slate-200/90 shadow-md relative overflow-hidden transition-all duration-200"
            style={{ background: preview ?? "#f1f5f9" }}
          >
            <div className="absolute inset-0 bg-gradient-to-t from-black/25 via-transparent to-white/10 pointer-events-none" />
            <div className="absolute bottom-2.5 left-2.5">
              <span className="rounded-lg bg-black/60 backdrop-blur-md px-2 py-0.5 text-xs font-mono font-bold text-white shadow-xs">
                {preview ?? "#------"}
              </span>
            </div>
          </div>
        </div>

        {/* Name, Desc & Color Input */}
        <div className="grid gap-4">
          <Field label="Formula Name" error={message(localErrors.name?.message, serverErrors, "name")}>
            <input className={inputClass} placeholder="e.g. Coastal Azure Blue" {...form.register("name")} />
          </Field>
          <Field label="Formula Description" error={message(localErrors.description?.message, serverErrors, "description")}>
            <textarea
              className={inputClass}
              rows={2}
              placeholder="e.g. 5-gallon exterior acrylic semi-gloss recipe"
              {...form.register("description")}
            />
          </Field>
          <Field label="Target Color Hex" error={message(localErrors.targetColor?.message, serverErrors, "targetColor")}>
            <div className="flex items-center gap-2.5">
              <input
                type="color"
                aria-label="Color picker"
                className="h-10 w-14 cursor-pointer rounded-xl border border-slate-300 bg-white p-1 shadow-xs"
                value={preview ?? "#208094"}
                onChange={(event) => form.setValue("targetColor", event.target.value.toUpperCase(), { shouldValidate: true })}
              />
              <input className={inputClass} placeholder="#RRGGBB" {...form.register("targetColor")} />
              <Button
                type="button"
                tone="secondary"
                className="px-3"
                onClick={() => form.setValue("targetColor", "#808080", { shouldValidate: true })}
              >
                <RotateCcw size={14} />
                <span className="hidden sm:inline">Reset</span>
              </Button>
            </div>
          </Field>
        </div>
      </div>

      {/* CMYK Doses Section */}
      <div className="rounded-2xl border border-slate-200/80 bg-slate-50/60 p-5">
        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3.5">
          Colorant Channel Doses (Grams)
        </h3>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {([
            ["cyanDose", "Cyan", "bg-cyan-500", "border-cyan-200"],
            ["magentaDose", "Magenta", "bg-pink-600", "border-pink-200"],
            ["yellowDose", "Yellow", "bg-amber-500", "border-amber-200"],
            ["blackDose", "Black", "bg-slate-900", "border-slate-300"],
          ] as const).map(([name, label, dot, border]) => (
            <div key={name} className={`rounded-xl border ${border} bg-white p-3.5 shadow-xs`}>
              <div className="flex items-center justify-between mb-2">
                <span className="flex items-center gap-2 text-xs font-bold text-slate-700">
                  <span className={`h-2.5 w-2.5 rounded-full ${dot}`} />
                  {label}
                </span>
                <span className="text-[11px] font-mono font-medium text-slate-400">grams</span>
              </div>
              <input
                className={`${inputClass} font-mono font-bold text-base py-1.5`}
                inputMode="decimal"
                placeholder="0.000"
                {...form.register(name)}
              />
              {message(localErrors[name]?.message, serverErrors, name) ? (
                <p className="mt-1.5 text-xs font-semibold text-rose-600">
                  {message(localErrors[name]?.message, serverErrors, name)}
                </p>
              ) : null}
            </div>
          ))}
        </div>

        {/* Base Paint & Total Summary */}
        <div className="mt-4 grid gap-4 sm:grid-cols-2 pt-3 border-t border-slate-200/60">
          <Field
            label="Base Paint Quantity (g, optional)"
            error={message(localErrors.basePaintQuantity?.message, serverErrors, "basePaintQuantity")}
          >
            <input
              className={`${inputClass} font-mono`}
              inputMode="decimal"
              placeholder="e.g. 1000.000"
              {...form.register("basePaintQuantity")}
            />
          </Field>

          <div className="flex flex-col justify-end rounded-xl bg-white border border-slate-200/80 p-3.5">
            <span className="text-xs font-medium text-slate-500">Total Colorant Weight</span>
            <span className="text-2xl font-mono font-black text-slate-900">
              {total ? `${total} g` : "0.000 g"}
            </span>
          </div>
        </div>

        {extraMessage(localErrors, "doses") || serverErrors?.find((error) => error.field === "doses") ? (
          <p className="mt-3 rounded-lg bg-rose-50 border border-rose-200 p-2.5 text-xs font-semibold text-rose-700">
            {extraMessage(localErrors, "doses") ?? serverErrors?.find((error) => error.field === "doses")?.message}
          </p>
        ) : null}
        {extraMessage(localErrors, "totalDose") ? (
          <p className="mt-3 rounded-lg bg-rose-50 border border-rose-200 p-2.5 text-xs font-semibold text-rose-700">
            {extraMessage(localErrors, "totalDose")}
          </p>
        ) : null}
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <Button type="submit" tone="primary" disabled={submitting} className="px-6 py-2.5 text-sm font-semibold shadow-md">
          <Save size={16} />
          {submitting ? "Saving Formula..." : "Save Recipe"}
        </Button>
      </div>
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
