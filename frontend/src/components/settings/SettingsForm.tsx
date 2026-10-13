"use client";

import { useState } from "react";
import { useSettings } from "@/hooks/use-api";
import { ApiError, apiRequest } from "@/lib/api/client";
import type { Settings } from "@/lib/api/types";
import { Button } from "@/components/ui/Button";
import { Field, inputClass } from "@/components/ui/Field";
import { useQueryClient } from "@tanstack/react-query";
import { Sliders, Save, Cpu, CheckCircle2 } from "lucide-react";

export function SettingsForm() {
  const settings = useSettings();
  const client = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);

  if (settings.isLoading) {
    return <p className="text-sm text-slate-500">Loading console settings…</p>;
  }
  if (settings.isError || !settings.data) {
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
        Settings could not be loaded from backend.
      </div>
    );
  }

  return (
    <SettingsFields
      initial={settings.data}
      onSave={async (body) => {
        setMessage(null);
        try {
          await apiRequest<Settings>("/api/v1/settings", { method: "PUT", body: JSON.stringify(body) });
          await client.invalidateQueries({ queryKey: ["settings"] });
          setMessage("Display settings saved successfully.");
        } catch (error) {
          setMessage(error instanceof ApiError ? error.message : "Settings could not be saved.");
        }
      }}
      message={message}
    />
  );
}

function SettingsFields({
  initial,
  onSave,
  message,
}: {
  initial: Settings;
  message: string | null;
  onSave: (body: {
    displayName: string;
    telemetryRefreshIntervalMs: number;
    showReadingTimestamps: boolean;
    compactLayout: boolean;
  }) => Promise<void>;
}) {
  const [displayName, setDisplayName] = useState(initial.displayName);
  const [interval, setInterval] = useState(String(initial.telemetryRefreshIntervalMs));
  const [timestamps, setTimestamps] = useState(initial.showReadingTimestamps);
  const [compact, setCompact] = useState(initial.compactLayout);

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Console Settings</h1>
        <p className="text-xs text-slate-500 mt-0.5">
          Configure operator console preferences and inspect hardware boundaries.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        {/* Form Card */}
        <form
          className="space-y-5 rounded-2xl border border-slate-200/90 bg-white p-6 shadow-xs"
          onSubmit={(event) => {
            event.preventDefault();
            void onSave({
              displayName,
              telemetryRefreshIntervalMs: Number(interval),
              showReadingTimestamps: timestamps,
              compactLayout: compact,
            });
          }}
        >
          <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
            <Sliders size={18} className="text-teal-600" />
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-700">
              Operator Preferences
            </h2>
          </div>

          <Field label="Machine Display Name" hint="Appears in header and batch history">
            <input
              className={inputClass}
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
            />
          </Field>

          <Field label="Telemetry Polling Rate (ms)" hint="Supported range: 1000ms – 30000ms">
            <input
              className={`${inputClass} font-mono`}
              inputMode="numeric"
              value={interval}
              onChange={(event) => setInterval(event.target.value)}
            />
          </Field>

          <div className="space-y-3 pt-2 border-t border-slate-100">
            <label className="flex items-center gap-3 text-sm font-medium text-slate-700 cursor-pointer">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-slate-300 text-teal-600 focus:ring-teal-500 cursor-pointer"
                checked={timestamps}
                onChange={(event) => setTimestamps(event.target.checked)}
              />
              <span>Show exact reading timestamps on sensor swatches</span>
            </label>

            <label className="flex items-center gap-3 text-sm font-medium text-slate-700 cursor-pointer">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-slate-300 text-teal-600 focus:ring-teal-500 cursor-pointer"
                checked={compact}
                onChange={(event) => setCompact(event.target.checked)}
              />
              <span>Compact layout mode (optimizes spacing for small touchscreens)</span>
            </label>
          </div>

          {message ? (
            <div className="flex items-center gap-2 rounded-xl bg-teal-50 border border-teal-200 p-3 text-xs font-semibold text-teal-800">
              <CheckCircle2 size={16} className="text-teal-600" />
              <span>{message}</span>
            </div>
          ) : null}

          <div className="pt-2">
            <Button type="submit" tone="primary" className="px-5 py-2.5 shadow-sm">
              <Save size={15} />
              Save Preferences
            </Button>
          </div>
        </form>

        {/* Hardware Status Card */}
        <aside className="rounded-2xl border border-slate-200/90 bg-white p-6 shadow-xs text-sm space-y-4">
          <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
            <Cpu size={18} className="text-slate-500" />
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-700">
              Hardware Parameters
            </h2>
          </div>

          <dl className="space-y-3 text-xs">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <dt className="text-slate-500 font-medium">Machine Identifier</dt>
              <dd className="font-mono font-bold text-slate-800">{initial.machineId}</dd>
            </div>
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <dt className="text-slate-500 font-medium">Adapter Protocol</dt>
              <dd className="font-mono uppercase font-bold text-slate-800">{initial.hardware.adapter}</dd>
            </div>
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <dt className="text-slate-500 font-medium">Hardware Verified</dt>
              <dd>
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                    initial.hardware.hardwareVerified
                      ? "bg-emerald-100 text-emerald-800"
                      : "bg-slate-100 text-slate-600"
                  }`}
                >
                  {initial.hardware.hardwareVerified ? "YES" : "NO"}
                </span>
              </dd>
            </div>
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <dt className="text-slate-500 font-medium">Hardware Commands Allowed</dt>
              <dd>
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                    initial.hardware.commandsEnabled
                      ? "bg-emerald-100 text-emerald-800"
                      : "bg-rose-100 text-rose-800"
                  }`}
                >
                  {initial.hardware.commandsEnabled ? "ENABLED" : "LOCKED"}
                </span>
              </dd>
            </div>
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <dt className="text-slate-500 font-medium">Max Total Colorant Dose</dt>
              <dd className="font-mono font-bold text-slate-800">{initial.softwareLimits.maxTotalDoseGrams} g</dd>
            </div>
            <div className="flex items-center justify-between">
              <dt className="text-slate-500 font-medium">Max Single Channel Dose</dt>
              <dd className="font-mono font-bold text-slate-800">{initial.softwareLimits.maxChannelDoseGrams} g</dd>
            </div>
          </dl>

          <div className="rounded-xl bg-slate-50 p-3 text-[11px] text-slate-500 leading-relaxed border border-slate-200/60">
            {initial.softwareLimits.note}
          </div>
        </aside>
      </div>
    </div>
  );
}
