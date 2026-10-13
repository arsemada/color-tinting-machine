"use client";

import { useState } from "react";
import Link from "next/link";
import { useBatches } from "@/hooks/use-api";
import { formatDose } from "@/lib/utils/quantity";
import { formatTimestamp } from "@/lib/utils/telemetry";
import {
  History,
  Filter,
  ChevronLeft,
  ChevronRight,
  Clock,
  ArrowUpRight,
  AlertCircle,
  CheckCircle2,
  XCircle,
  HelpCircle,
} from "lucide-react";

const states = ["", "STARTING", "DISPENSING", "PAUSED", "CANCELLING", "COMPLETED", "CANCELLED", "FAILED"];

const stateBadges: Record<string, { label: string; className: string; icon: typeof CheckCircle2 }> = {
  COMPLETED: { label: "Completed", className: "bg-emerald-50 text-emerald-800 border-emerald-200", icon: CheckCircle2 },
  CANCELLED: { label: "Cancelled", className: "bg-amber-50 text-amber-800 border-amber-200", icon: AlertCircle },
  FAILED: { label: "Failed", className: "bg-rose-50 text-rose-800 border-rose-200", icon: XCircle },
  DISPENSING: { label: "Dispensing", className: "bg-teal-50 text-teal-800 border-teal-200 animate-pulse", icon: Clock },
  STARTING: { label: "Starting", className: "bg-cyan-50 text-cyan-800 border-cyan-200", icon: Clock },
  PAUSED: { label: "Paused", className: "bg-slate-100 text-slate-800 border-slate-200", icon: AlertCircle },
  CANCELLING: { label: "Cancelling", className: "bg-amber-50 text-amber-800 border-amber-200", icon: AlertCircle },
};

export function BatchHistory() {
  const [state, setState] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const batches = useBatches({
    state: state || undefined,
    from: from ? new Date(from).toISOString() : undefined,
    to: to ? new Date(to).toISOString() : undefined,
    page,
  });

  return (
    <div className="space-y-5">
      {/* Page Title */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Batch Dispense History</h1>
        <p className="text-xs text-slate-500 mt-0.5">
          Immutable audit record of all physical batches mixed on this machine.
        </p>
      </div>

      {/* Filter Toolbar */}
      <div className="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-xs">
        <div className="flex items-center gap-2 mb-3 text-xs font-bold uppercase tracking-wider text-slate-500">
          <Filter size={14} className="text-teal-600" />
          <span>Filter Records</span>
        </div>
        <div className="grid gap-3 sm:grid-cols-3">
          <label className="text-xs font-semibold text-slate-700">
            Lifecycle Status
            <select
              className="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm shadow-xs focus:border-teal-600 focus:outline-none"
              value={state}
              onChange={(event) => {
                setState(event.target.value);
                setPage(0);
              }}
            >
              {states.map((item) => (
                <option key={item || "all"} value={item}>
                  {item ? item : "All Lifecycles"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-xs font-semibold text-slate-700">
            From Date
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm shadow-xs focus:border-teal-600 focus:outline-none"
              type="datetime-local"
              value={from}
              onChange={(event) => {
                setFrom(event.target.value);
                setPage(0);
              }}
            />
          </label>
          <label className="text-xs font-semibold text-slate-700">
            To Date
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm shadow-xs focus:border-teal-600 focus:outline-none"
              type="datetime-local"
              value={to}
              onChange={(event) => {
                setTo(event.target.value);
                setPage(0);
              }}
            />
          </label>
        </div>
      </div>

      {batches.isError ? (
        <p className="rounded-xl bg-rose-50 border border-rose-200 p-3 text-sm text-rose-700">
          Batch history could not be loaded.
        </p>
      ) : null}

      {/* Table */}
      <div className="overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-xs">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-slate-100 bg-slate-50/70 text-xs font-semibold uppercase tracking-wider text-slate-500">
            <tr>
              <th className="px-5 py-3.5">Started At</th>
              <th className="px-4 py-3.5">Formula Snapshot</th>
              <th className="px-4 py-3.5">Execution State</th>
              <th className="px-4 py-3.5">Target Doses</th>
              <th className="px-4 py-3.5">Actual Measured</th>
              <th className="px-5 py-3.5 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {batches.data?.items.length ? (
              batches.data.items.map((batch) => {
                const badge = stateBadges[batch.state] ?? {
                  label: batch.state,
                  className: "bg-slate-100 text-slate-700 border-slate-200",
                  icon: HelpCircle,
                };
                const BadgeIcon = badge.icon;

                return (
                  <tr key={batch.id} className="transition-colors hover:bg-slate-50/70">
                    <td className="px-5 py-3.5 font-mono text-xs text-slate-600">
                      {formatTimestamp(batch.startedAt)}
                    </td>
                    <td className="px-4 py-3.5">
                      <Link
                        className="font-bold text-slate-900 hover:text-teal-700 hover:underline flex items-center gap-1.5"
                        href={`/batches/${batch.id}`}
                      >
                        <span>{batch.recipeName}</span>
                      </Link>
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="flex items-center gap-1.5">
                        <span
                          className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold ${badge.className}`}
                        >
                          <BadgeIcon size={12} />
                          {badge.label}
                        </span>
                        {batch.outcomeUnknown ? (
                          <span className="rounded-md bg-amber-100 px-1.5 py-0.5 text-[10px] font-bold text-amber-800">
                            Unconfirmed
                          </span>
                        ) : null}
                      </div>
                    </td>
                    <td className="px-4 py-3.5 font-mono text-xs font-semibold text-slate-700">
                      {formatDose(batch.targetTotalGrams)}
                    </td>
                    <td className="px-4 py-3.5 font-mono text-xs font-bold text-slate-900">
                      {formatDose(batch.actualTotalGrams)}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <Link
                        href={`/batches/${batch.id}`}
                        className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-600 hover:bg-slate-50 transition"
                      >
                        <span>View</span>
                        <ArrowUpRight size={13} />
                      </Link>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td className="px-5 py-12 text-center text-slate-400" colSpan={6}>
                  <div className="flex flex-col items-center justify-center">
                    <History size={32} className="mb-2 text-slate-300" />
                    <p className="text-sm font-semibold text-slate-600">
                      {batches.isLoading ? "Loading batch audit records..." : "No past batch runs found."}
                    </p>
                    <p className="mt-1 text-xs text-slate-400">
                      Past dispensing operations will appear here automatically.
                    </p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div className="flex items-center justify-between text-xs text-slate-500 font-medium">
        <button
          type="button"
          className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-xs hover:bg-slate-50 disabled:opacity-40"
          disabled={page === 0}
          onClick={() => setPage((value) => value - 1)}
        >
          <ChevronLeft size={15} />
          Previous Page
        </button>
        <span className="font-mono">
          Page {page + 1} {batches.data ? `of ${Math.max(batches.data.totalPages, 1)}` : ""}
        </span>
        <button
          type="button"
          className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-xs hover:bg-slate-50 disabled:opacity-40"
          disabled={!batches.data || page + 1 >= batches.data.totalPages}
          onClick={() => setPage((value) => value + 1)}
        >
          Next Page
          <ChevronRight size={15} />
        </button>
      </div>
    </div>
  );
}
