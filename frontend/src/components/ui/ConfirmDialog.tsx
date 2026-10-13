"use client";

import { useEffect, useId, useRef } from "react";
import { Button } from "@/components/ui/Button";
import { AlertTriangle, HelpCircle } from "lucide-react";

export function ConfirmDialog({
  open,
  title,
  body,
  confirmLabel,
  tone = "primary",
  onConfirm,
  onClose,
}: {
  open: boolean;
  title: string;
  body: string;
  confirmLabel: string;
  tone?: "primary" | "danger";
  onConfirm: () => void;
  onClose: () => void;
}) {
  const titleId = useId();
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (open) {
      cancelRef.current?.focus();
    }
  }, [open]);

  if (!open) {
    return null;
  }

  const isDanger = tone === "danger";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm bg-slate-900/50 animate-in fade-in duration-200">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl transition-all"
      >
        <div className="flex items-start gap-3.5">
          <div
            className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
              isDanger ? "bg-rose-100 text-rose-600" : "bg-teal-100 text-teal-700"
            }`}
          >
            {isDanger ? <AlertTriangle size={20} /> : <HelpCircle size={20} />}
          </div>
          <div>
            <h2 id={titleId} className="text-lg font-semibold text-slate-900">
              {title}
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-slate-600">{body}</p>
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-2.5 border-t border-slate-100 pt-4">
          <Button ref={cancelRef} type="button" tone="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="button"
            tone={tone}
            onClick={() => {
              onConfirm();
              onClose();
            }}
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
