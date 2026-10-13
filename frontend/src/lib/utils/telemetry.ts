export type StatusTone = "ok" | "warn" | "danger" | "neutral";

export interface ConnectionInput {
  connectionState: string;
  dataSource: string;
  simulated: boolean;
  stale: boolean;
  probed: boolean;
}

export function connectionPresentation(input: ConnectionInput): { label: string; tone: StatusTone } {
  if (input.simulated || input.dataSource === "SIMULATED") {
    return { label: "Simulated", tone: "warn" };
  }
  if (!input.probed || input.connectionState === "CONNECTING") {
    return { label: "Connecting", tone: "neutral" };
  }
  if (input.stale || input.connectionState === "STALE") {
    return { label: "Stale data", tone: "warn" };
  }
  switch (input.connectionState) {
    case "CONNECTED":
      return { label: "Connected", tone: "ok" };
    case "COMMUNICATION_ERROR":
      return { label: "Communication error", tone: "danger" };
    case "DISCONNECTED":
      return { label: "Disconnected", tone: "danger" };
    default:
      return { label: "Disconnected", tone: "neutral" };
  }
}

export function isReadingStale(observedAt: string | null | undefined, nowMs: number, staleAfterMs: number): boolean {
  if (!observedAt) {
    return false;
  }
  const time = Date.parse(observedAt);
  if (Number.isNaN(time)) {
    return true;
  }
  return nowMs - time > staleAfterMs;
}

export function readingAgeLabel(observedAt: string | null | undefined, nowMs: number): string {
  if (!observedAt) {
    return "No timestamp";
  }
  const time = Date.parse(observedAt);
  if (Number.isNaN(time)) {
    return "Invalid timestamp";
  }
  const seconds = Math.max(0, Math.round((nowMs - time) / 1000));
  if (seconds < 60) {
    return `${seconds}s old`;
  }
  const minutes = Math.round(seconds / 60);
  return `${minutes} min old`;
}

const ACTIVE = new Set(["STARTING", "DISPENSING", "PAUSED", "CANCELLING"]);

export function elapsedDuration(
  state: string,
  startedAt: string | null | undefined,
  completedAt: string | null | undefined,
  nowMs: number,
): string | null {
  if (!startedAt || state === "STARTING") {
    return null;
  }
  const start = Date.parse(startedAt);
  if (Number.isNaN(start)) {
    return null;
  }
  const end = ACTIVE.has(state) ? nowMs : completedAt ? Date.parse(completedAt) : Number.NaN;
  if (Number.isNaN(end) || end < start) {
    return null;
  }
  const seconds = Math.round((end - start) / 1000);
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return minutes > 0 ? `${minutes}m ${rest}s` : `${rest}s`;
}

export function formatTimestamp(value: string | null | undefined): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Invalid time";
  }
  // Avoid Intl dateStyle/timeZoneName. Some Windows engines throw
  // "Invalid option : option" and that exception takes down the dashboard.
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function createCommandGuard() {
  let pending = false;
  return {
    isPending: () => pending,
    begin(): string | null {
      if (pending) {
        return null;
      }
      pending = true;
      return crypto.randomUUID();
    },
    finish() {
      pending = false;
    },
  };
}
