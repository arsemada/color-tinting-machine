"use client";

import { ConnectionStatus } from "@/components/dashboard/ConnectionStatus";
import { FaultList } from "@/components/dashboard/FaultList";
import { PumpCard } from "@/components/dashboard/PumpCard";
import { MachineControls } from "@/components/controls/MachineControls";
import { Panel } from "@/components/ui/Panel";
import { Button } from "@/components/ui/Button";
import { useEvents, useMachineStatus, useMachineTelemetry, useRecipe, useRefreshMachine, useSettings } from "@/hooks/use-api";
import { selectedRecipeId } from "@/hooks/use-api";
import { useNow } from "@/hooks/use-now";
import type { PumpState } from "@/lib/api/types";
import { rgbCss } from "@/lib/utils/color";
import { dispensingProgress, formatDose } from "@/lib/utils/quantity";
import { elapsedDuration, formatTimestamp, readingAgeLabel } from "@/lib/utils/telemetry";
import { RefreshCw } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

export function DashboardView() {
  const settings = useSettings();
  const refreshMs = settings.data?.telemetryRefreshIntervalMs ?? 2000;
  const status = useMachineStatus(refreshMs);
  const telemetry = useMachineTelemetry(refreshMs);
  const events = useEvents(refreshMs);
  const refresh = useRefreshMachine();
  const [recipeId, setRecipeId] = useState<string | null>(null);
  const recipe = useRecipe(recipeId ?? "");

  useEffect(() => {
    const sync = () => setRecipeId(selectedRecipeId());
    sync();
    window.addEventListener("ctm-recipe", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("ctm-recipe", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  const machine = status.data;
  const live = telemetry.data;
  const simulated = Boolean(machine?.simulated || live?.simulated);
  const color = live?.color;
  const sensed = color?.available ? rgbCss(color.red, color.green, color.blue) : null;
  const weight = live?.loadCell;
  const tracked = machine?.trackedBatch ?? null;
  const progress = dispensingProgress(tracked?.actualTotalGrams, tracked?.targetTotalGrams);
  const now = useNow();
  const showTimes = settings.data?.showReadingTimestamps ?? true;

  return (
    <div className="space-y-4">
      {simulated ? (
        <div className="rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-950">
          Simulated fixture. These values are not ESP32 readings and must not be treated as live machine data.
        </div>
      ) : null}
      {status.isError ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger">
          The dashboard cannot reach the backend. No machine readings are available.
        </div>
      ) : null}

      <header className="rounded-xl border border-line bg-white p-4 shadow-[var(--shadow)]">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-accent">Color tinting machine</p>
            <h1 className="mt-1 text-2xl font-semibold">{machine?.displayName ?? "Color Tinting Machine"}</h1>
            <p className="mt-1 text-sm text-muted">Machine {machine?.machineId ?? "—"}</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <ConnectionStatus
              connectionState={machine?.connectionState ?? "CONNECTING"}
              dataSource={machine?.dataSource ?? "UNAVAILABLE"}
              simulated={simulated}
              stale={Boolean(machine?.stale)}
              probed={Boolean(machine?.probed)}
            />
            <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium">
              State {machine?.operatingState ?? "UNKNOWN"}
            </span>
            <Button type="button" onClick={() => refresh.mutate()} disabled={refresh.isPending}>
              <RefreshCw size={16} />
              Refresh
            </Button>
          </div>
        </div>
        <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-3">
          <div>
            <dt className="text-muted">Last successful telemetry</dt>
            <dd>{formatTimestamp(machine?.lastSuccessfulTelemetryAt)}</dd>
          </div>
          <div>
            <dt className="text-muted">Hardware verified</dt>
            <dd>{machine?.hardwareVerified ? "Yes" : "No"}</dd>
          </div>
          <div>
            <dt className="text-muted">Current fault</dt>
            <dd>{machine?.faults?.[0]?.message ?? "None reported"}</dd>
          </div>
        </dl>
        {machine?.message ? <p className="mt-3 text-sm text-muted">{machine.message}</p> : null}
      </header>

      <div className="grid gap-4 xl:grid-cols-2">
        <Panel title="Color detection">
          <div className="flex flex-col gap-4 sm:flex-row">
            <div
              className="h-28 w-full rounded-lg border border-line sm:w-28"
              style={{ background: sensed ?? "repeating-linear-gradient(45deg,#eef2f4,#eef2f4 8px,#f8fafb 8px,#f8fafb 16px)" }}
              aria-label={sensed ? "Sensed color preview" : "Color sensor unavailable"}
            />
            <div className="text-sm">
              <p className="font-medium">{color?.available ? "Sensor reading" : "Sensor unavailable"}</p>
              <p className="mt-1 text-muted">
                RGB {color?.available ? `${color.red}, ${color.green}, ${color.blue}` : "not reported"}
              </p>
              <p className="text-muted">Calibration {color?.calibrationState ?? "UNKNOWN"}</p>
              {showTimes ? <p className="text-muted">Reading {formatTimestamp(color?.readAt)}</p> : null}
              <p className="mt-2 text-xs text-muted">
                This is the sensed sample, separate from the selected target color. RGB values are only shown when the device supplies them.
              </p>
            </div>
          </div>
        </Panel>

        <Panel title="Target recipe" action={<Link className="text-sm font-medium text-accent" href="/recipes">Choose</Link>}>
          {recipe.data ? (
            <div className="flex gap-4">
              <div className="h-16 w-16 rounded-lg border border-line" style={{ background: recipe.data.targetColor }} />
              <div className="text-sm">
                <p className="font-medium">{recipe.data.name}</p>
                <p className="text-muted">Target swatch {recipe.data.targetColor}. This does not calculate a physical formula.</p>
                <p className="mt-1">C {formatDose(recipe.data.cyanDose)} · M {formatDose(recipe.data.magentaDose)}</p>
                <p>Y {formatDose(recipe.data.yellowDose)} · K {formatDose(recipe.data.blackDose)}</p>
                <p className="mt-1 font-medium">Estimated colorant total {formatDose(recipe.data.totalColorant)}</p>
              </div>
            </div>
          ) : (
            <p className="text-sm text-muted">No recipe is selected. Choose one from Recipes before requesting a batch.</p>
          )}
        </Panel>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Panel title="Live weight">
          <p className="text-3xl font-semibold tracking-tight">
            {weight?.available ? formatDose(weight.weightGrams) : "Unavailable"}
          </p>
          <p className="mt-2 text-sm text-muted">Target dose {tracked ? formatDose(tracked.targetTotalGrams) : "Not selected"}</p>
          <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100">
            <div className="h-full bg-accent" style={{ width: `${progress ?? 0}%` }} />
          </div>
          <p className="mt-2 text-sm">{progress == null ? "Progress unavailable" : `${progress.toFixed(0)}% of target`}</p>
          <p className="text-sm text-muted">Tare {weight?.tareState ?? "UNKNOWN"}</p>
          {showTimes ? (
            <p className="text-sm text-muted">
              {weight?.available && now != null ? readingAgeLabel(weight.readAt, now) : "No measurement timestamp"} · {formatTimestamp(weight?.readAt)}
            </p>
          ) : null}
          {live?.stale ? <p className="mt-2 text-sm font-medium text-warn">This reading is stale.</p> : null}
        </Panel>

        <Panel title="Batch progress">
          {tracked ? (
            <div className="space-y-1 text-sm">
              <p className="font-mono text-xs">{tracked.id}</p>
              <p className="font-medium">{tracked.recipeName}</p>
              <p>State {tracked.state}</p>
              <p>Target {formatDose(tracked.targetTotalGrams)}</p>
              <p>Actual {formatDose(tracked.actualTotalGrams)}</p>
              <p>Progress {progress == null ? "Unavailable" : `${progress.toFixed(0)}%`}</p>
              <p>Started {formatTimestamp(tracked.startedAt)}</p>
              <p>Elapsed {now == null ? "Unavailable" : elapsedDuration(tracked.state, tracked.startedAt, tracked.completedAt, now) ?? "Unavailable until dispensing is confirmed"}</p>
              {tracked.outcomeUnknown ? (
                <p className="font-medium text-warn">The outcome is unknown. The batch has not been marked completed.</p>
              ) : null}
            </div>
          ) : (
            <p className="text-sm text-muted">No active batch. Completion is recorded only after the machine reports it.</p>
          )}
        </Panel>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {(live?.pumps ?? ["CYAN", "MAGENTA", "YELLOW", "BLACK"].map((channel) => ({
          channel,
          state: "UNKNOWN" as PumpState,
          dispensedGrams: null,
          targetGrams: null,
          fault: null,
        }))).map((pump) => (
          <PumpCard key={pump.channel} {...pump} />
        ))}
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <MachineControls status={machine} recipeId={recipe.data?.id} />
        <Panel title="Faults and events">
          <FaultList
            faults={[
              ...(machine?.faults ?? []).map((fault) => ({ ...fault, simulated: machine?.simulated })),
              ...(events.data ?? []).map((event) => ({
                severity: event.severity,
                message: event.message,
                occurredAt: event.occurredAt,
                code: event.code,
                simulated: event.simulated,
              })),
            ].slice(0, 8)}
          />
        </Panel>
      </div>
    </div>
  );
}
