export function channelByte(value: number | null | undefined): boolean {
  return typeof value === "number" && Number.isInteger(value) && value >= 0 && value <= 255;
}

export function rgbCss(
  red: number | null | undefined,
  green: number | null | undefined,
  blue: number | null | undefined,
): string | null {
  if (!channelByte(red) || !channelByte(green) || !channelByte(blue)) {
    return null;
  }
  return `rgb(${red} ${green} ${blue})`;
}

export function normalizeHex(value: string): string | null {
  const trimmed = value.trim();
  if (!/^#[0-9a-fA-F]{6}$/.test(trimmed)) {
    return null;
  }
  return trimmed.toUpperCase();
}

export function hexToRgb(value: string): { red: number; green: number; blue: number } | null {
  const hex = normalizeHex(value);
  if (!hex) {
    return null;
  }
  return {
    red: Number.parseInt(hex.slice(1, 3), 16),
    green: Number.parseInt(hex.slice(3, 5), 16),
    blue: Number.parseInt(hex.slice(5, 7), 16),
  };
}
