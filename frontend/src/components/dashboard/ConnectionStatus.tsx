"use client";

import { connectionPresentation, type StatusTone } from "@/lib/utils/telemetry";
import { Badge } from "@/components/ui/Badge";

export function ConnectionStatus({
  connectionState,
  dataSource,
  simulated,
  stale,
  probed,
}: {
  connectionState: string;
  dataSource: string;
  simulated: boolean;
  stale: boolean;
  probed: boolean;
}) {
  const presentation = connectionPresentation({ connectionState, dataSource, simulated, stale, probed });
  return (
    <Badge tone={presentation.tone as StatusTone} data-testid="connection-status">
      {presentation.label}
    </Badge>
  );
}
