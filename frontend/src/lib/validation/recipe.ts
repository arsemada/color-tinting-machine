import { z } from "zod";

const dose = z
  .string()
  .trim()
  .regex(/^(?:0|[1-9]\d{0,8})(?:\.\d{1,3})?$/, "Use a nonnegative number with at most 3 decimal places.");

export function recipeSchema(limits: { maxChannelDoseGrams: string; maxTotalDoseGrams: string }) {
  const maxChannel = Number(limits.maxChannelDoseGrams);
  const maxTotal = Number(limits.maxTotalDoseGrams);
  return z
    .object({
      name: z.string().trim().min(1, "Name is required.").max(120, "Name must be 120 characters or fewer."),
      description: z.string().max(2000, "Description must be 2000 characters or fewer."),
      targetColor: z.string().trim().regex(/^#[0-9A-Fa-f]{6}$/, "Use a color in #RRGGBB form."),
      cyanDose: dose,
      magentaDose: dose,
      yellowDose: dose,
      blackDose: dose,
      doseUnit: z.literal("GRAMS"),
      basePaintQuantity: z.string().trim(),
    })
    .superRefine((value, ctx) => {
      const channels = [
        ["cyanDose", value.cyanDose],
        ["magentaDose", value.magentaDose],
        ["yellowDose", value.yellowDose],
        ["blackDose", value.blackDose],
      ] as const;
      let total = 0;
      let positive = false;
      for (const [field, raw] of channels) {
        const numeric = Number(raw);
        total += numeric;
        if (numeric > 0) {
          positive = true;
        }
        if (Number.isFinite(maxChannel) && numeric > maxChannel) {
          ctx.addIssue({
            code: "custom",
            path: [field],
            message: `Dose exceeds the configured software limit of ${limits.maxChannelDoseGrams} g.`,
          });
        }
      }
      if (!positive) {
        ctx.addIssue({
          code: "custom",
          path: ["doses"],
          message: "At least one colorant dose must be greater than zero before dispensing.",
        });
      }
      if (Number.isFinite(maxTotal) && total > maxTotal) {
        ctx.addIssue({
          code: "custom",
          path: ["totalDose"],
          message: `Total colorant dose exceeds the configured software limit of ${limits.maxTotalDoseGrams} g.`,
        });
      }
      if (value.basePaintQuantity !== "" && !/^(?:0|[1-9]\d{0,8})(?:\.\d{1,3})?$/.test(value.basePaintQuantity)) {
        ctx.addIssue({
          code: "custom",
          path: ["basePaintQuantity"],
          message: "Use a nonnegative number with at most 3 decimal places.",
        });
      }
    });
}

export type RecipeFormValues = z.input<ReturnType<typeof recipeSchema>>;

export const emptyRecipe = {
  name: "",
  description: "",
  targetColor: "#208094",
  cyanDose: "0",
  magentaDose: "0",
  yellowDose: "0",
  blackDose: "0",
  doseUnit: "GRAMS" as const,
  basePaintQuantity: "",
};

export function fieldErrors(error: z.ZodError): Record<string, string> {
  const result: Record<string, string> = {};
  for (const issue of error.issues) {
    const key = issue.path[0];
    if (typeof key === "string" && !result[key]) {
      result[key] = issue.message;
    }
  }
  return result;
}
