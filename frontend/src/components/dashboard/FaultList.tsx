import { formatTimestamp } from "@/lib/utils/telemetry";
import { AlertCircle, AlertTriangle, CheckCircle2, Info } from "lucide-react";

export function FaultList({
  faults,
}: {
  faults: { severity: string; message: string; occurredAt?: string | null; code?: string; simulated?: boolean }[];
}) {
  if (faults.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-6 text-center text-slate-400">
        <CheckCircle2 size={24} className="mb-2 text-emerald-500/70" />
        <p className="text-sm font-medium">No active faults or alerts.</p>
        <p className="text-xs text-slate-400">Machine telemetry is clear.</p>
      </div>
    );
  }

  return (
    <ul className="space-y-2.5">
      {faults.map((fault, index) => {
        const isError = fault.severity === "CRITICAL" || fault.severity === "ERROR";
        const isWarning = fault.severity === "WARNING";

        return (
          <li
            key={`${fault.code ?? "fault"}-${index}`}
            className={`rounded-xl border p-3 transition-colors ${
              isError
                ? "border-rose-200/90 bg-rose-50/50"
                : isWarning
                ? "border-amber-200/90 bg-amber-50/50"
                : "border-slate-200 bg-slate-50/50"
            }`}
          >
            <div className="flex items-center justify-between gap-2 text-xs">
              <span
                className={`flex items-center gap-1.5 font-bold tracking-wide ${
                  isError ? "text-rose-700" : isWarning ? "text-amber-700" : "text-slate-600"
                }`}
              >
                {isError ? (
                  <AlertCircle size={14} className="text-rose-600" />
                ) : isWarning ? (
                  <AlertTriangle size={14} className="text-amber-600" />
                ) : (
                  <Info size={14} className="text-blue-500" />
                )}
                <span>{fault.severity}</span>
                {fault.simulated ? (
                  <span className="rounded-md bg-white/80 px-1.5 py-0.2 text-[10px] text-slate-500 border border-slate-200">
                    simulated
                  </span>
                ) : null}
              </span>
              <time className="text-[11px] text-slate-400 font-mono">{formatTimestamp(fault.occurredAt)}</time>
            </div>
            <p className="mt-1.5 text-xs font-medium text-slate-700 leading-relaxed">{fault.message}</p>
          </li>
        );
      })}
    </ul>
  );
}
