import type { HTMLAttributes } from "react";

const tones = {
  ok: "bg-emerald-50 text-ok ring-emerald-200",
  warn: "bg-amber-50 text-warn ring-amber-200",
  danger: "bg-red-50 text-danger ring-red-200",
  neutral: "bg-slate-100 text-slate-700 ring-slate-200",
};

export function Badge({
  tone = "neutral",
  className = "",
  children,
  ...props
}: HTMLAttributes<HTMLSpanElement> & { tone?: keyof typeof tones }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${tones[tone]} ${className}`}
      {...props}
    >
      {children}
    </span>
  );
}
