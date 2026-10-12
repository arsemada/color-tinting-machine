const CHANNEL = /^(?:0|[1-9]\d{0,8})(?:\.\d{1,3})?$/;

export function parseMilliGrams(value: string | number | null | undefined): number | null {
  if (value == null) {
    return null;
  }
  const raw = String(value).trim();
  if (!CHANNEL.test(raw)) {
    return null;
  }
  const [whole, fraction = ""] = raw.split(".");
  return Number(whole) * 1000 + Number(fraction.padEnd(3, "0"));
}

export function formatMilliGrams(milli: number): string {
  const whole = Math.trunc(milli / 1000);
  const fraction = String(Math.abs(milli % 1000)).padStart(3, "0");
  return `${whole}.${fraction}`;
}

export function formatDose(value: string | number | null | undefined): string {
  const milli = parseMilliGrams(value);
  if (milli == null) {
    return "Not reported";
  }
  return `${formatMilliGrams(milli)} g`;
}

export function sumDoses(values: Array<string | null | undefined>): string | null {
  let total = 0;
  for (const value of values) {
    if (value == null || value.trim() === "") {
      continue;
    }
    const milli = parseMilliGrams(value);
    if (milli == null) {
      return null;
    }
    total += milli;
  }
  return formatMilliGrams(total);
}

export function dispensingProgress(
  actual: string | number | null | undefined,
  target: string | number | null | undefined,
): number | null {
  const actualMilli = parseMilliGrams(actual);
  const targetMilli = parseMilliGrams(target);
  if (actualMilli == null || targetMilli == null || targetMilli <= 0) {
    return null;
  }
  return Math.min(100, Math.max(0, (actualMilli / targetMilli) * 100));
}
