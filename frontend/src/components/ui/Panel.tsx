import type { ReactNode } from "react";

export function Panel({
  title,
  hint,
  action,
  children,
  className = "",
}: {
  title: string;
  hint?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={`rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.04),0_6px_16px_rgb(0_0_0/0.02)] transition-shadow duration-200 ${className}`}
    >
      <div className="mb-3.5 flex items-start justify-between gap-3 border-b border-slate-100 pb-2.5">
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">{title}</h2>
          {hint ? <p className="mt-0.5 text-xs text-slate-400">{hint}</p> : null}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}
