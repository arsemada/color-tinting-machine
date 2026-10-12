import { formatTimestamp } from "@/lib/utils/telemetry";

export function FaultList({
  faults,
}: {
  faults: { severity: string; message: string; occurredAt?: string | null; code?: string; simulated?: boolean }[];
}) {
  if (faults.length === 0) {
    return <p className="text-sm text-muted">No faults or events recorded.</p>;
  }
  return (
    <ul className="space-y-2">
      {faults.map((fault, index) => (
        <li key={`${fault.code ?? "fault"}-${index}`} className="rounded-lg border border-line px-3 py-2">
          <div className="flex items-center justify-between gap-3 text-xs">
            <span className={`font-semibold ${fault.severity === "CRITICAL" || fault.severity === "ERROR" ? "text-danger" : "text-warn"}`}>
              {fault.severity}
              {fault.simulated ? " · simulated" : ""}
            </span>
            <time className="text-muted">{formatTimestamp(fault.occurredAt)}</time>
          </div>
          <p className="mt-1 text-sm">{fault.message}</p>
        </li>
      ))}
    </ul>
  );
}
