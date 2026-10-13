import type { ReactNode } from "react";
import { AlertCircle } from "lucide-react";

export function Field({
  label,
  error,
  children,
  hint,
}: {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className="block text-sm">
      <span className="mb-1.5 block font-semibold text-slate-700 text-xs uppercase tracking-wider">{label}</span>
      {children}
      {hint ? <span className="mt-1 block text-xs text-slate-400">{hint}</span> : null}
      {error ? (
        <span className="mt-1.5 flex items-center gap-1.5 text-xs font-semibold text-rose-600">
          <AlertCircle size={13} className="shrink-0" />
          {error}
        </span>
      ) : null}
    </label>
  );
}

export const inputClass =
  "w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm font-medium text-slate-800 shadow-xs outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-500/20";
