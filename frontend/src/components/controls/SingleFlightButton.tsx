"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { createCommandGuard } from "@/lib/utils/telemetry";

export function SingleFlightButton({
  label,
  disabled,
  onAction,
}: {
  label: string;
  disabled?: boolean;
  onAction: (idempotencyKey: string) => Promise<void>;
}) {
  const [guard] = useState(() => createCommandGuard());
  const [pending, setPending] = useState(false);

  return (
    <Button
      type="button"
      tone="primary"
      disabled={disabled || pending}
      onClick={() => {
        const key = guard.begin();
        if (!key) {
          return;
        }
        setPending(true);
        void onAction(key).finally(() => {
          guard.finish();
          setPending(false);
        });
      }}
    >
      {pending ? "Sending…" : label}
    </Button>
  );
}
