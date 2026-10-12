import { forwardRef, type ButtonHTMLAttributes } from "react";

const tones = {
  primary: "bg-accent text-white hover:bg-accent-strong",
  secondary: "bg-white text-foreground border border-line hover:bg-slate-50",
  danger: "bg-danger text-white hover:bg-red-800",
  ghost: "bg-transparent text-foreground hover:bg-slate-100",
};

export const Button = forwardRef<
  HTMLButtonElement,
  ButtonHTMLAttributes<HTMLButtonElement> & { tone?: keyof typeof tones }
>(function Button({ tone = "secondary", className = "", ...props }, ref) {
  return (
    <button
      ref={ref}
      className={`inline-flex items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition disabled:opacity-50 ${tones[tone]} ${className}`}
      {...props}
    />
  );
});
