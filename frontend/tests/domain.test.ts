import { describe, expect, it } from "vitest";
import { hexToRgb, normalizeHex, rgbCss } from "@/lib/utils/color";
import { dispensingProgress, formatDose, sumDoses } from "@/lib/utils/quantity";
import { connectionPresentation, createCommandGuard, elapsedDuration, formatTimestamp, isReadingStale } from "@/lib/utils/telemetry";
import { recipeSchema } from "@/lib/validation/recipe";

describe("color preview", () => {
  it("converts valid RGB into a CSS color and rejects missing channels", () => {
    expect(rgbCss(12, 34, 56)).toBe("rgb(12 34 56)");
    expect(rgbCss(null, 1, 2)).toBeNull();
    expect(rgbCss(256, 0, 0)).toBeNull();
  });

  it("normalizes hexadecimal colors", () => {
    expect(normalizeHex("#20a0b0")).toBe("#20A0B0");
    expect(hexToRgb("#0000ff")).toEqual({ red: 0, green: 0, blue: 255 });
    expect(normalizeHex("blue")).toBeNull();
  });
});

describe("quantities", () => {
  it("sums doses without treating a blank value as zero in a reported field", () => {
    expect(sumDoses(["1.5", "0.25", "", "0"])).toBe("1.750");
    expect(sumDoses(["1", "bad"])).toBeNull();
    expect(formatDose(null)).toBe("Not reported");
    expect(formatDose("")).toBe("Not reported");
  });

  it("does not invent progress for a zero or missing target", () => {
    expect(dispensingProgress("10", "0")).toBeNull();
    expect(dispensingProgress(null, "10")).toBeNull();
    expect(dispensingProgress("5", "10")).toBe(50);
  });
});

describe("telemetry presentation", () => {
  it("does not call simulated data connected", () => {
    expect(
      connectionPresentation({
        connectionState: "CONNECTED",
        dataSource: "SIMULATED",
        simulated: true,
        stale: false,
        probed: true,
      }).label,
    ).toBe("Simulated");
  });

  it("shows communication errors and stale readings distinctly", () => {
    expect(
      connectionPresentation({
        connectionState: "COMMUNICATION_ERROR",
        dataSource: "UNAVAILABLE",
        simulated: false,
        stale: false,
        probed: true,
      }).label,
    ).toBe("Communication error");
    expect(isReadingStale("2026-10-12T00:00:00Z", Date.parse("2026-10-12T00:00:11Z"), 10000)).toBe(true);
    expect(isReadingStale(null, Date.now(), 10000)).toBe(false);
  });

  it("formats timestamps without throwing when the timezone name option is unsupported", () => {
    expect(formatTimestamp(null)).toBe("—");
    expect(formatTimestamp("not-a-time")).toBe("Invalid time");
    expect(formatTimestamp("2026-10-12T21:45:15.380Z")).toMatch(/2026/);
  });

  it("hides elapsed time until dispensing is confirmed", () => {
    expect(elapsedDuration("STARTING", "2026-10-12T00:00:00Z", null, Date.parse("2026-10-12T00:01:00Z"))).toBeNull();
    expect(elapsedDuration("DISPENSING", "2026-10-12T00:00:00Z", null, Date.parse("2026-10-12T00:01:05Z"))).toBe("1m 5s");
  });
});

describe("recipe validation", () => {
  const schema = recipeSchema({ maxChannelDoseGrams: "10.000", maxTotalDoseGrams: "20.000" });

  it("requires a name, a color, and a positive dose", () => {
    const result = schema.safeParse({
      name: "",
      description: "",
      targetColor: "red",
      cyanDose: "0",
      magentaDose: "0",
      yellowDose: "0",
      blackDose: "0",
      doseUnit: "GRAMS",
      basePaintQuantity: "",
    });
    expect(result.success).toBe(false);
  });

  it("rejects a channel above the software limit", () => {
    const result = schema.safeParse({
      name: "Heavy",
      description: "",
      targetColor: "#112233",
      cyanDose: "11",
      magentaDose: "0",
      yellowDose: "0",
      blackDose: "0",
      doseUnit: "GRAMS",
      basePaintQuantity: "",
    });
    expect(result.success).toBe(false);
  });
});

describe("duplicate command guard", () => {
  it("returns one key until the operation finishes", () => {
    const guard = createCommandGuard();
    const first = guard.begin();
    expect(first).toBeTruthy();
    expect(guard.begin()).toBeNull();
    guard.finish();
    expect(guard.begin()).toBeTruthy();
  });
});
