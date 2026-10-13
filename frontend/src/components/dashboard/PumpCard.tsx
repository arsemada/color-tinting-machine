import type { PumpState } from "@/lib/api/types";
import { dispensingProgress, formatDose } from "@/lib/utils/quantity";

const accents: Record<
  string,
  {
    gradient: string;
    border: string;
    text: string;
    badge: string;
    bar: string;
    label: string;
    letter: string;
  }
> = {
  CYAN: {
    gradient: "from-cyan-500/10 to-teal-500/5",
    border: "border-cyan-200/90",
    text: "text-cyan-800",
    badge: "bg-cyan-500 text-white",
    bar: "bg-cyan-500",
    label: "Cyan Channel",
    letter: "C",
  },
  MAGENTA: {
    gradient: "from-pink-500/10 to-rose-500/5",
    border: "border-pink-200/90",
    text: "text-pink-800",
    badge: "bg-pink-600 text-white",
    bar: "bg-pink-600",
    label: "Magenta Channel",
    letter: "M",
  },
  YELLOW: {
    gradient: "from-amber-500/10 to-yellow-500/5",
    border: "border-amber-200/90",
    text: "text-amber-800",
    badge: "bg-amber-500 text-white",
    bar: "bg-amber-500",
    label: "Yellow Channel",
    letter: "Y",
  },
  BLACK: {
    gradient: "from-slate-800/10 to-slate-900/5",
    border: "border-slate-300",
    text: "text-slate-900",
    badge: "bg-slate-900 text-white",
    bar: "bg-slate-900",
    label: "Black Channel",
    letter: "K",
  },
};

const stateLabels: Record<string, { label: string; tone: string }> = {
  UNKNOWN: { label: "Waiting", tone: "bg-slate-100 text-slate-600 border-slate-200" },
  OFF: { label: "Off", tone: "bg-slate-100 text-slate-600 border-slate-200" },
  ON: { label: "Running", tone: "bg-emerald-100 text-emerald-800 border-emerald-300" },
  STARTING: { label: "Starting", tone: "bg-teal-100 text-teal-800 border-teal-300" },
  STOPPING: { label: "Stopping", tone: "bg-amber-100 text-amber-800 border-amber-300" },
  FAULT: { label: "Fault", tone: "bg-rose-100 text-rose-800 border-rose-300" },
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
  const look = accents[channel] ?? {
    gradient: "from-slate-100 to-slate-50",
    border: "border-slate-200",
    text: "text-slate-800",
    badge: "bg-slate-700 text-white",
    bar: "bg-slate-700",
    label: channel,
    letter: channel.slice(0, 1),
  };
  const reported = state || "UNKNOWN";
  const stateMeta = stateLabels[reported] ?? { label: reported, tone: "bg-slate-100 text-slate-600 border-slate-200" };
  const progress = dispensingProgress(dispensedGrams, targetGrams);
  const isPumping = reported === "ON";

  return (
    <article
      className={`relative overflow-hidden rounded-2xl border bg-gradient-to-br ${look.gradient} ${look.border} p-4 shadow-sm transition-all duration-200 hover:shadow-md`}
    >
      <div className="flex items-center justify-between gap-2">
        <h3 className="flex items-center gap-2 text-sm font-bold text-slate-800">
          <span
            className={`flex h-6 w-6 items-center justify-center rounded-lg text-xs font-black shadow-xs ${look.badge}`}
            aria-hidden="true"
          >
            {look.letter}
          </span>
          <span>{look.label}</span>
        </h3>
        <span
          className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold ${stateMeta.tone}`}
          data-testid={`pump-state-${channel}`}
        >
          {isPumping ? <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-beacon" /> : null}
          {stateMeta.label}
          <span className="sr-only">{reported}</span>
        </span>
      </div>

      <div className="mt-4">
        <p className="text-xs uppercase tracking-wider text-slate-500 font-medium">Dispensed</p>
        <p className="text-xl font-bold tracking-tight text-slate-900">{formatDose(dispensedGrams)}</p>
        <p className="text-xs text-slate-500">Target: {formatDose(targetGrams)}</p>
      </div>

      {/* Fluid progress indicator bar */}
      <div className="mt-3">
        <div className="h-2 w-full overflow-hidden rounded-full bg-white/80 border border-slate-200/60 p-[1px]">
          <div
            className={`h-full rounded-full transition-all duration-300 ${look.bar}`}
            style={{ width: `${progress != null ? Math.max(progress, 2) : 0}%` }}
          />
        </div>
      </div>

      {fault ? (
        <p className="mt-2.5 rounded-lg bg-rose-50 border border-rose-200 p-2 text-xs font-semibold text-rose-700">
          {fault}
        </p>
      ) : null}
    </article>
  );
}
