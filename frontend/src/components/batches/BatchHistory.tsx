"use client";

import { useState } from "react";
import Link from "next/link";
import { useBatches } from "@/hooks/use-api";
import { formatDose } from "@/lib/utils/quantity";
import { formatTimestamp } from "@/lib/utils/telemetry";

const states = ["", "STARTING", "DISPENSING", "PAUSED", "CANCELLING", "COMPLETED", "CANCELLED", "FAILED"];

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
    <div className="space-y-4">
      <div className="grid gap-3 rounded-xl border border-line bg-white p-4 md:grid-cols-4">
        <label className="text-sm">
          Status
          <select className="mt-1 w-full rounded-md border border-line px-3 py-2" value={state} onChange={(event) => { setState(event.target.value); setPage(0); }}>
            {states.map((item) => (
              <option key={item || "all"} value={item}>{item || "All"}</option>
            ))}
          </select>
        </label>
        <label className="text-sm">
          From
          <input className="mt-1 w-full rounded-md border border-line px-3 py-2" type="datetime-local" value={from} onChange={(event) => { setFrom(event.target.value); setPage(0); }} />
        </label>
        <label className="text-sm">
          To
          <input className="mt-1 w-full rounded-md border border-line px-3 py-2" type="datetime-local" value={to} onChange={(event) => { setTo(event.target.value); setPage(0); }} />
        </label>
      </div>
      {batches.isError ? <p className="text-sm text-danger">Batch history could not be loaded.</p> : null}
      <div className="overflow-x-auto rounded-xl border border-line bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-line text-muted">
            <tr>
              <th className="px-3 py-2">Started</th>
              <th className="px-3 py-2">Recipe</th>
              <th className="px-3 py-2">State</th>
              <th className="px-3 py-2">Target</th>
              <th className="px-3 py-2">Actual</th>
            </tr>
          </thead>
          <tbody>
            {batches.data?.items.length ? (
              batches.data.items.map((batch) => (
                <tr key={batch.id} className="border-b border-line last:border-0">
                  <td className="px-3 py-3">{formatTimestamp(batch.startedAt)}</td>
                  <td className="px-3 py-3">
                    <Link className="font-medium text-accent" href={`/batches/${batch.id}`}>{batch.recipeName}</Link>
                  </td>
                  <td className="px-3 py-3">{batch.state}{batch.outcomeUnknown ? " · outcome unknown" : ""}</td>
                  <td className="px-3 py-3">{formatDose(batch.targetTotalGrams)}</td>
                  <td className="px-3 py-3">{formatDose(batch.actualTotalGrams)}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td className="px-3 py-6 text-muted" colSpan={5}>
                  {batches.isLoading ? "Loading batches…" : "No batches match these filters."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="flex items-center justify-between text-sm">
        <button type="button" className="rounded-md border border-line px-3 py-2 disabled:opacity-50" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
          Previous
        </button>
        <span>Page {page + 1}{batches.data ? ` of ${Math.max(batches.data.totalPages, 1)}` : ""}</span>
        <button
          type="button"
          className="rounded-md border border-line px-3 py-2 disabled:opacity-50"
          disabled={!batches.data || page + 1 >= batches.data.totalPages}
          onClick={() => setPage((value) => value + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
