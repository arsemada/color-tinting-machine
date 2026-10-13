import { forwardRef, type ButtonHTMLAttributes } from "react";

const tones = {
  primary:
    "bg-gradient-to-b from-teal-600 to-teal-700 text-white shadow-sm hover:from-teal-700 hover:to-teal-800 active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-teal-500/50",
  secondary:
    "bg-white text-slate-700 border border-slate-200/90 shadow-sm hover:bg-slate-50 hover:border-slate-300 active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-slate-400/50",
  danger:
    "bg-gradient-to-b from-rose-600 to-rose-700 text-white shadow-sm hover:from-rose-700 hover:to-rose-800 active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-rose-500/50",
  ghost:
    "bg-transparent text-slate-600 hover:bg-slate-100/80 hover:text-slate-900 active:scale-[0.98]",
};

export const Button = forwardRef<
  HTMLButtonElement,
  ButtonHTMLAttributes<HTMLButtonElement> & { tone?: keyof typeof tones }
>(function Button({ tone = "secondary", className = "", ...props }, ref) {
  return (
    <button
      ref={ref}
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium transition-all duration-150 disabled:pointer-events-none disabled:opacity-40 disabled:shadow-none ${tones[tone]} ${className}`}
      {...props}
    />
  );
});
