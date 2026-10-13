"use client";

import { ConnectionStatus } from "@/components/dashboard/ConnectionStatus";
import { FaultList } from "@/components/dashboard/FaultList";
import { PumpCard } from "@/components/dashboard/PumpCard";
import { MachineControls } from "@/components/controls/MachineControls";
import { Panel } from "@/components/ui/Panel";
import { Button } from "@/components/ui/Button";
import {
  useEvents,
  useMachineStatus,
  useMachineTelemetry,
  useRecipe,
  useRefreshMachine,
  useSettings,
  selectedRecipeId,
} from "@/hooks/use-api";
import { useNow } from "@/hooks/use-now";
import type { PumpState } from "@/lib/api/types";
import { rgbCss } from "@/lib/utils/color";
import { dispensingProgress, formatDose } from "@/lib/utils/quantity";
import { elapsedDuration, formatTimestamp } from "@/lib/utils/telemetry";
import {
  RefreshCw,
  Layers,
  FlaskConical,
  AlertTriangle,
  ArrowRight,
  Sparkles,
  SlidersHorizontal,
} from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

const emptyPumps = ["CYAN", "MAGENTA", "YELLOW", "BLACK"].map((channel) => ({
  channel,
  state: "UNKNOWN" as PumpState,
  dispensedGrams: null,
  targetGrams: null,
  fault: null,
}));

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
  const pumps = live?.pumps ?? emptyPumps;

  return (
    <div className="space-y-6">
      {/* Simulation Notice Banner */}
      {simulated ? (
        <div className="flex items-center gap-3 rounded-2xl border border-amber-200/90 bg-amber-50/90 px-4 py-3 text-sm text-amber-950 shadow-xs">
          <FlaskConical size={18} className="shrink-0 text-amber-600" />
          <p>
            <strong className="font-semibold">Simulated Test Environment:</strong> Test fixture active. Real dispenser hardware is protected and will not fire.
          </p>
        </div>
      ) : null}

      {/* Backend Communication Error Banner */}
      {status.isError ? (
        <div className="flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800 shadow-xs">
          <AlertTriangle size={18} className="shrink-0 text-rose-600" />
          <p>
            <strong className="font-semibold">Backend Unreachable:</strong> The operator console cannot communicate with the Spring Boot server. Verify it is running on port 8080.
          </p>
        </div>
      ) : null}

      {/* Top Header / Cockpit Status */}
      <header className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200/80 bg-white/90 p-5 shadow-xs backdrop-blur-md">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              {machine?.displayName ?? "Color Tinting Machine"}
            </h1>
            <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-mono font-medium text-slate-500">
              {machine?.machineId ?? "machine-1"}
            </span>
          </div>
          <p className="mt-1 flex items-center gap-2 text-xs text-slate-500">
            <span className={`inline-block h-2 w-2 rounded-full ${machine?.hardwareVerified ? "bg-emerald-500" : "bg-slate-300"}`} />
            {machine?.hardwareVerified ? "Hardware Verified & Dispatched" : "Standby Mode · Pumps Inactive"}
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <ConnectionStatus
            connectionState={machine?.connectionState ?? "CONNECTING"}
            dataSource={machine?.dataSource ?? "UNAVAILABLE"}
            simulated={simulated}
            stale={Boolean(machine?.stale)}
            probed={Boolean(machine?.probed)}
          />
          <Button
            type="button"
            tone="secondary"
            onClick={() => refresh.mutate()}
            disabled={refresh.isPending}
            aria-label="Refresh status"
            className="px-3"
          >
            <RefreshCw size={15} className={refresh.isPending ? "animate-spin text-teal-600" : "text-slate-600"} />
            <span className="hidden sm:inline">Probe</span>
          </Button>
        </div>
      </header>

      {/* Hero Color Comparison: Sensor Reading vs Target Recipe */}
      <div className="grid gap-5 md:grid-cols-2">
        {/* Sensor Swatch */}
        <section className="relative overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-xs transition-shadow hover:shadow-md">
          <div className="p-4 pb-0 flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <Sparkles size={16} className="text-teal-600" />
              <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500">Color Optical Sensor (TCS3200)</h2>
            </div>
            {color?.available ? (
              <span className="rounded-full bg-emerald-50 px-2.5 py-0.5 text-[10px] font-bold text-emerald-700 border border-emerald-200">
                ACTIVE
              </span>
            ) : (
              <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-[10px] font-medium text-slate-500 border border-slate-200">
                STANDBY
              </span>
            )}
          </div>

          <div
            className="h-36 relative transition-colors duration-300"
            style={{
              background:
                sensed ??
                "repeating-linear-gradient(45deg, #f1f5f9, #f1f5f9 12px, #f8fafc 12px, #f8fafc 24px)",
            }}
            aria-label={sensed ? "Sensed color swatch" : "No sensor reading"}
          >
            <div className="absolute inset-0 bg-gradient-to-t from-black/25 via-transparent to-white/10 pointer-events-none" />
            {!color?.available ? (
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="rounded-xl bg-white/90 backdrop-blur-md border border-slate-200/80 px-3.5 py-1.5 text-xs font-semibold text-slate-600 shadow-sm">
                  Optical Sensor Not Connected
                </span>
              </div>
            ) : null}
          </div>

          <div className="flex items-center justify-between p-4 bg-slate-50/50">
            <div>
              <p className="text-xs font-medium text-slate-500">Live RGB Channels</p>
              {color?.available ? (
                <div className="mt-1 flex items-center gap-2">
                  <span className="rounded-md bg-rose-100 text-rose-800 px-2 py-0.5 text-xs font-bold">R {color.red}</span>
                  <span className="rounded-md bg-emerald-100 text-emerald-800 px-2 py-0.5 text-xs font-bold">G {color.green}</span>
                  <span className="rounded-md bg-blue-100 text-blue-800 px-2 py-0.5 text-xs font-bold">B {color.blue}</span>
                </div>
              ) : (
                <p className="mt-1 text-sm font-semibold text-slate-600">No reading reported</p>
              )}
            </div>
            {showTimes && color?.readAt ? (
              <p className="text-[11px] text-slate-400 font-mono">{formatTimestamp(color.readAt)}</p>
            ) : null}
          </div>
        </section>

        {/* Recipe Target Swatch */}
        <section className="relative overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-xs transition-shadow hover:shadow-md">
          <div className="p-4 pb-0 flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <Layers size={16} className="text-teal-600" />
              <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500">Selected Recipe Target</h2>
            </div>
            {recipe.data ? (
              <Link
                href="/recipes"
                className="text-xs font-semibold text-teal-700 hover:text-teal-800 hover:underline"
              >
                Change Formula →
              </Link>
            ) : null}
          </div>

          {recipe.data ? (
            <>
              <div
                className="h-36 relative transition-colors duration-300"
                style={{ background: recipe.data.targetColor }}
              >
                <div className="absolute inset-0 bg-gradient-to-t from-black/25 via-transparent to-white/10 pointer-events-none" />
                <div className="absolute bottom-3 left-4">
                  <span className="rounded-lg bg-black/60 backdrop-blur-md px-2.5 py-1 text-xs font-mono font-bold text-white shadow-sm">
                    {recipe.data.targetColor}
                  </span>
                </div>
              </div>

              <div className="p-4 bg-slate-50/50">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-base font-bold text-slate-900">{recipe.data.name}</p>
                    <div className="mt-1 flex flex-wrap gap-1.5 text-xs font-semibold">
                      <span className="rounded-md bg-cyan-100 text-cyan-800 px-1.5 py-0.5">C {formatDose(recipe.data.cyanDose)}</span>
                      <span className="rounded-md bg-pink-100 text-pink-800 px-1.5 py-0.5">M {formatDose(recipe.data.magentaDose)}</span>
                      <span className="rounded-md bg-amber-100 text-amber-800 px-1.5 py-0.5">Y {formatDose(recipe.data.yellowDose)}</span>
                      <span className="rounded-md bg-slate-200 text-slate-800 px-1.5 py-0.5">K {formatDose(recipe.data.blackDose)}</span>
                    </div>
                  </div>
                </div>
              </div>
            </>
          ) : (
            <div className="flex h-56 flex-col items-center justify-center p-6 text-center">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-teal-50 text-teal-700 mb-3 shadow-xs">
                <SlidersHorizontal size={22} />
              </div>
              <p className="text-base font-bold text-slate-800">No Recipe Selected</p>
              <p className="mt-1 text-xs text-slate-500 max-w-xs">
                Choose a paint formulation to load dispensing targets onto the console.
              </p>
              <Link
                href="/recipes"
                className="mt-4 inline-flex items-center gap-2 rounded-xl bg-teal-700 px-4 py-2 text-xs font-bold text-white shadow-sm hover:bg-teal-800 transition"
              >
                <span>Browse Recipes</span>
                <ArrowRight size={14} />
              </Link>
            </div>
          )}
        </section>
      </div>

      {/* Live Scale & Batch Status Cards */}
      <div className="grid gap-5 sm:grid-cols-2">
        <Panel title="Load Cell Scale">
          <div className="flex items-baseline justify-between">
            <div className="flex items-baseline gap-1.5">
              <span className="font-mono text-4xl font-extrabold tracking-tight text-slate-900">
                {weight?.available ? formatDose(weight.weightGrams) : "—"}
              </span>
            </div>
            <span
              className={`rounded-md px-2 py-0.5 text-xs font-semibold ${
                weight?.available ? "bg-emerald-100 text-emerald-800" : "bg-slate-100 text-slate-500"
              }`}
            >
              {weight?.tareState ?? "NOT TARED"}
            </span>
          </div>

          <div className="mt-4">
            <div className="flex items-center justify-between text-xs text-slate-500 mb-1">
              <span>Batch Dispensing Target</span>
              <span className="font-semibold text-slate-700">
                {progress == null ? "No target" : `${progress.toFixed(0)}% of ${formatDose(tracked?.targetTotalGrams)}`}
              </span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-teal-600 transition-all duration-300"
                style={{ width: `${progress != null ? Math.max(progress, 2) : 0}%` }}
              />
            </div>
          </div>

          {live?.stale ? (
            <p className="mt-3 rounded-lg bg-amber-50 p-2 text-xs font-medium text-amber-800 border border-amber-200">
              Warning: Scale readings are older than 10 seconds (stale).
            </p>
          ) : null}
        </Panel>

        <Panel title="Batch Execution Status">
          {tracked ? (
            <div>
              <div className="flex items-center justify-between">
                <p className="text-2xl font-bold tracking-tight text-slate-900">{tracked.state}</p>
                <span className="rounded-full bg-teal-100 text-teal-800 px-2.5 py-0.5 text-xs font-bold">
                  ACTIVE
                </span>
              </div>
              <p className="mt-1 text-xs font-semibold text-slate-500">{tracked.recipeName}</p>

              <div className="mt-3 grid grid-cols-2 gap-2 rounded-xl bg-slate-50 p-2.5 text-xs">
                <div>
                  <span className="text-slate-400 block">Dispensed / Target</span>
                  <span className="font-bold text-slate-800">
                    {formatDose(tracked.actualTotalGrams)} / {formatDose(tracked.targetTotalGrams)}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block">Elapsed Time</span>
                  <span className="font-bold text-slate-800 font-mono">
                    {now == null
                      ? "—"
                      : elapsedDuration(tracked.state, tracked.startedAt, tracked.completedAt, now) ?? "Standby"}
                  </span>
                </div>
              </div>
            </div>
          ) : (
            <div className="py-2">
              <p className="text-2xl font-bold tracking-tight text-slate-400">Idle</p>
              <p className="mt-1 text-xs text-slate-500">No dispensing batch currently in progress.</p>
              <div className="mt-4 rounded-xl border border-dashed border-slate-200 p-3 text-xs text-slate-400 text-center">
                Select a formula and click Start Batch to begin dispensing.
              </div>
            </div>
          )}
        </Panel>
      </div>

      {/* 4 CMYK Pump Vials */}
      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Peristaltic Dispensing Lines (CMYK)
          </h2>
          <span className="text-xs text-slate-400">4 Channels Managed</span>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {pumps.map((pump) => (
            <PumpCard key={pump.channel} {...pump} />
          ))}
        </div>
      </div>

      {/* Controls & Activity Split */}
      <div className="grid gap-5 xl:grid-cols-[1.15fr_0.85fr]">
        <MachineControls status={machine} recipeId={recipe.data?.id} />
        <Panel title="Station Activity & Faults">
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
            ].slice(0, 6)}
          />
        </Panel>
      </div>
    </div>
  );
}
