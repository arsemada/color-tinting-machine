"use client";

import { useEffect, useState } from "react";

export function useNow(enabled = true): number | null {
  const [now, setNow] = useState<number | null>(null);

  useEffect(() => {
    if (!enabled) {
      return;
    }
    const update = () => setNow(Date.now());
    update();
    const id = window.setInterval(update, 1000);
    return () => window.clearInterval(id);
  }, [enabled]);

  return now;
}
