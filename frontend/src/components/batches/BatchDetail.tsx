"use client";

import { useBatch } from "@/hooks/use-api";
import { formatDose } from "@/lib/utils/quantity";
import { elapsedDuration, formatTimestamp } from "@/lib/utils/telemetry";
import { useNow } from "@/hooks/use-now";

export function BatchDetail({ id }: { id: string }) {
  const batch = useBatch(id);
  const state = batch.data?.state;
  const now = useNow(state === "DISPENSING" || state === "PAUSED" || state === "CANCELLING");
  if (batch.isLoading) {
    return <p className="text-sm text-muted">Loading batch…</p>;
  }
  if (batch.isError || !batch.data) {
    return <p className="text-sm text-danger">This batch could not be loaded.</p>;
  }
  const item = batch.data;
  const elapsed = elapsedDuration(item.state, item.startedAt, item.completedAt, now ?? 0);
  return (
    <div className="space-y-4 rounded-xl border border-line bg-white p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-mono text-xs text-muted">{item.id}</p>
          <h1 className="text-xl font-semibold">{item.recipeName}</h1>
          <p className="text-sm text-muted">{item.recipeDescription}</p>
        </div>
        <div className="h-12 w-12 rounded-lg border border-line" style={{ background: item.targetColor }} />
      </div>
      <dl className="grid gap-2 text-sm sm:grid-cols-2">
        <div>State: {item.state}</div>
        <div>Machine: {item.machineId}</div>
        <div>Target: {formatDose(item.targetTotalGrams)}</div>
        <div>Actual: {formatDose(item.actualTotalGrams)}</div>
        <div>Started: {formatTimestamp(item.startedAt)}</div>
        <div>Completed: {formatTimestamp(item.completedAt)}</div>
        <div>Elapsed: {elapsed ?? "Unavailable"}</div>
        <div>Outcome unknown: {item.outcomeUnknown ? "Yes" : "No"}</div>
      </dl>
      {item.failureDetail ? <p className="text-sm text-danger">{item.failureDetail}</p> : null}
      {item.cancellationDetail ? <p className="text-sm text-warn">{item.cancellationDetail}</p> : null}
      <p className="text-xs text-muted">
        These quantities are the snapshot used for this batch. Later recipe edits do not change them. Actual amounts stay blank until the machine reports them.
      </p>
      <div className="overflow-x-auto">
        <table className="min-w-full text-left text-sm">
          <thead>
            <tr className="text-muted">
              <th className="py-2">Channel</th>
              <th>Target</th>
              <th>Actual</th>
            </tr>
          </thead>
          <tbody>
            {item.pumps.map((pump) => (
              <tr key={pump.channel} className="border-t border-line">
                <td className="py-2">{pump.channel}</td>
                <td>{formatDose(pump.targetGrams)}</td>
                <td>{formatDose(pump.actualGrams)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
