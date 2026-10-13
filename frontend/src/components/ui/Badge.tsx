import type { HTMLAttributes } from "react";

const tones = {
  ok: "bg-emerald-50 text-emerald-700 border-emerald-200 shadow-sm",
  warn: "bg-amber-50 text-amber-700 border-amber-200 shadow-sm",
  danger: "bg-rose-50 text-rose-700 border-rose-200 shadow-sm",
  neutral: "bg-slate-100 text-slate-700 border-slate-200 shadow-sm",
};

const dotColors = {
  ok: "bg-emerald-500",
  warn: "bg-amber-500",
  danger: "bg-rose-500",
  neutral: "bg-slate-400",
};

export function Badge({
  tone = "neutral",
  className = "",
  children,
  ...props
}: HTMLAttributes<HTMLSpanElement> & { tone?: keyof typeof tones }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium tracking-wide ${tones[tone]} ${className}`}
      {...props}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${dotColors[tone]}`} aria-hidden="true" />
      {children}
    </span>
  );
}
