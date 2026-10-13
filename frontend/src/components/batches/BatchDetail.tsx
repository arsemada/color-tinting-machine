"use client";

import Link from "next/link";
import { useBatch } from "@/hooks/use-api";
import { formatDose } from "@/lib/utils/quantity";
import { elapsedDuration, formatTimestamp } from "@/lib/utils/telemetry";
import { useNow } from "@/hooks/use-now";
import { ArrowLeft, AlertCircle, AlertTriangle } from "lucide-react";

export function BatchDetail({ id }: { id: string }) {
  const batch = useBatch(id);
  const state = batch.data?.state;
  const now = useNow(state === "DISPENSING" || state === "PAUSED" || state === "CANCELLING");

  if (batch.isLoading) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center text-slate-500">
        <p className="text-sm">Loading batch details...</p>
      </div>
    );
  }
  if (batch.isError || !batch.data) {
    return (
      <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center text-rose-700">
        <p className="text-sm font-semibold">This batch record could not be loaded.</p>
        <Link href="/batches" className="mt-3 inline-block text-xs font-bold text-rose-800 underline">
          Return to History
        </Link>
      </div>
    );
  }

  const item = batch.data;
  const elapsed = elapsedDuration(item.state, item.startedAt, item.completedAt, now ?? 0);

  return (
    <div className="space-y-6">
      {/* Back Link */}
      <Link
        href="/batches"
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-teal-700 hover:text-teal-800 hover:underline"
      >
        <ArrowLeft size={14} />
        Back to Batch History
      </Link>

      {/* Main Container Card */}
      <div className="space-y-6 rounded-2xl border border-slate-200/90 bg-white p-6 shadow-xs">
        {/* Header with Color Swatch */}
        <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-100 pb-5">
          <div>
            <div className="flex items-center gap-2">
              <span className="rounded-md bg-slate-100 px-2 py-0.5 font-mono text-[11px] font-semibold text-slate-500">
                {item.id}
              </span>
              <span className="rounded-full bg-teal-100 px-2.5 py-0.5 text-xs font-bold text-teal-800">
                {item.state}
              </span>
            </div>
            <h1 className="mt-2 text-2xl font-bold tracking-tight text-slate-900">{item.recipeName}</h1>
            {item.recipeDescription ? (
              <p className="mt-1 text-sm text-slate-500">{item.recipeDescription}</p>
            ) : null}
          </div>

          <div className="flex items-center gap-3">
            <div className="text-right">
              <span className="block text-xs uppercase tracking-wider text-slate-400 font-semibold">Target Color</span>
              <span className="font-mono text-xs font-bold text-slate-700">{item.targetColor}</span>
            </div>
            <div
              className="h-14 w-14 rounded-2xl border border-slate-200 shadow-sm"
              style={{ background: item.targetColor }}
            />
          </div>
        </div>

        {/* Key Operational Metrics */}
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-xl border border-slate-200/80 bg-slate-50/70 p-3.5">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 block">Target Colorant</span>
            <span className="text-lg font-bold font-mono text-slate-900">{formatDose(item.targetTotalGrams)}</span>
          </div>
          <div className="rounded-xl border border-slate-200/80 bg-slate-50/70 p-3.5">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 block">Actual Dispensed</span>
            <span className="text-lg font-bold font-mono text-slate-900">{formatDose(item.actualTotalGrams)}</span>
          </div>
          <div className="rounded-xl border border-slate-200/80 bg-slate-50/70 p-3.5">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 block">Started Timestamp</span>
            <span className="text-xs font-medium font-mono text-slate-700 block mt-1">{formatTimestamp(item.startedAt)}</span>
          </div>
          <div className="rounded-xl border border-slate-200/80 bg-slate-50/70 p-3.5">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 block">Elapsed Time</span>
            <span className="text-lg font-bold font-mono text-slate-900">{elapsed ?? "Standby"}</span>
          </div>
        </div>

        {/* Failure & Cancellation Details */}
        {item.failureDetail ? (
          <div className="flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50/80 p-3.5 text-xs text-rose-800">
            <AlertCircle size={16} className="shrink-0 text-rose-600 mt-0.5" />
            <div>
              <strong className="block font-semibold mb-0.5">Failure Details:</strong>
              {item.failureDetail}
            </div>
          </div>
        ) : null}

        {item.cancellationDetail ? (
          <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50/80 p-3.5 text-xs text-amber-800">
            <AlertTriangle size={16} className="shrink-0 text-amber-600 mt-0.5" />
            <div>
              <strong className="block font-semibold mb-0.5">Cancellation Details:</strong>
              {item.cancellationDetail}
            </div>
          </div>
        ) : null}

        {/* Per-Pump Channel Breakdown Table */}
        <div>
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2.5">
            Per-Channel Target vs Measured Quantities
          </h3>
          <div className="overflow-hidden rounded-xl border border-slate-200">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3">Channel</th>
                  <th className="px-4 py-3">Target Dose</th>
                  <th className="px-4 py-3">Actual Measured</th>
                  <th className="px-4 py-3 text-right">Yield Variance</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {item.pumps.map((pump) => {
                  const channelDots: Record<string, string> = {
                    CYAN: "bg-cyan-500",
                    MAGENTA: "bg-pink-600",
                    YELLOW: "bg-amber-500",
                    BLACK: "bg-slate-900",
                  };
                  return (
                    <tr key={pump.channel} className="hover:bg-slate-50/60">
                      <td className="px-4 py-3 font-semibold text-slate-900 flex items-center gap-2">
                        <span className={`h-2.5 w-2.5 rounded-full ${channelDots[pump.channel] ?? "bg-slate-400"}`} />
                        <span>{pump.channel}</span>
                      </td>
                      <td className="px-4 py-3 font-mono font-medium text-slate-700">
                        {formatDose(pump.targetGrams)}
                      </td>
                      <td className="px-4 py-3 font-mono font-bold text-slate-900">
                        {formatDose(pump.actualGrams)}
                      </td>
                      <td className="px-4 py-3 font-mono text-xs text-slate-500 text-right">
                        {pump.actualGrams != null ? "Recorded" : "Awaiting Report"}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Audit Disclaimer */}
        <div className="rounded-xl bg-slate-50 p-3.5 text-xs text-slate-500 leading-relaxed border border-slate-200/60">
          <p>
            <strong>Snapshot Integrity Guarantee:</strong> These quantities are an immutable snapshot recorded at the time of batch initiation. Subsequent edits or deletions of recipe formulas do not alter historical records.
          </p>
        </div>
      </div>
    </div>
  );
}
