import type { PumpState } from "@/lib/api/types";
import { formatDose } from "@/lib/utils/quantity";

const accents: Record<string, string> = {
  CYAN: "border-l-cyan-600",
  MAGENTA: "border-l-pink-700",
  YELLOW: "border-l-amber-500",
  BLACK: "border-l-slate-900",
};

export function PumpCard({
  channel,
  state,
  dispensedGrams,
  targetGrams,
  fault,
}: {
  channel: string;
  state: PumpState | string;
  dispensedGrams: string | null;
  targetGrams: string | null;
  fault: string | null;
}) {
  return (
    <article className={`rounded-xl border border-line border-l-4 bg-white p-4 ${accents[channel] ?? "border-l-slate-400"}`}>
      <div className="flex items-center justify-between gap-2">
        <h3 className="font-semibold">{channel.charAt(0) + channel.slice(1).toLowerCase()}</h3>
        <span className="font-mono text-xs font-semibold tracking-wide" data-testid={`pump-state-${channel}`}>
          {state || "UNKNOWN"}
        </span>
      </div>
      <dl className="mt-3 space-y-1 text-sm">
        <div className="flex justify-between gap-3">
          <dt className="text-muted">Dispensed</dt>
          <dd>{formatDose(dispensedGrams)}</dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt className="text-muted">Target</dt>
          <dd>{formatDose(targetGrams)}</dd>
        </div>
      </dl>
      {fault ? <p className="mt-3 text-sm font-medium text-danger">{fault}</p> : null}
    </article>
  );
}
