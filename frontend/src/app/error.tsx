"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/Button";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="rounded-xl border border-red-200 bg-white p-6">
      <h1 className="text-lg font-semibold">The dashboard hit an unexpected error</h1>
      <p className="mt-2 text-sm text-muted">The page stopped rendering this section. Machine safety does not depend on this screen.</p>
      <Button className="mt-4" type="button" tone="primary" onClick={reset}>
        Try again
      </Button>
    </div>
  );
}
