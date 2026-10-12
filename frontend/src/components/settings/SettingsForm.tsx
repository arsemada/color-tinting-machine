"use client";

import { useState } from "react";
import { useSettings } from "@/hooks/use-api";
import { ApiError, apiRequest } from "@/lib/api/client";
import type { Settings } from "@/lib/api/types";
import { Button } from "@/components/ui/Button";
import { Field, inputClass } from "@/components/ui/Field";
import { useQueryClient } from "@tanstack/react-query";

export function SettingsForm() {
  const settings = useSettings();
  const client = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);

  if (settings.isLoading) {
    return <p className="text-sm text-muted">Loading settings…</p>;
  }
  if (settings.isError || !settings.data) {
    return <p className="text-sm text-danger">Settings could not be loaded.</p>;
  }

  return (
    <SettingsFields
      initial={settings.data}
      onSave={async (body) => {
        setMessage(null);
        try {
          await apiRequest<Settings>("/api/v1/settings", { method: "PUT", body: JSON.stringify(body) });
          await client.invalidateQueries({ queryKey: ["settings"] });
          setMessage("Display settings saved.");
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
    <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
      <form
        className="space-y-4 rounded-xl border border-line bg-white p-4"
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
        <h1 className="text-xl font-semibold">Settings</h1>
        <Field label="Display name">
          <input className={inputClass} value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
        </Field>
        <Field label="Telemetry refresh interval (ms)" hint="Between 1000 and 30000. This is how often the dashboard asks the backend, not the ESP32 directly.">
          <input className={inputClass} inputMode="numeric" value={interval} onChange={(event) => setInterval(event.target.value)} />
        </Field>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={timestamps} onChange={(event) => setTimestamps(event.target.checked)} />
          Show reading timestamps
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={compact} onChange={(event) => setCompact(event.target.checked)} />
          Compact layout preference
        </label>
        {message ? <p className="text-sm">{message}</p> : null}
        <Button type="submit" tone="primary">Save display settings</Button>
      </form>
      <aside className="space-y-3 rounded-xl border border-line bg-white p-4 text-sm">
        <h2 className="font-semibold">Machine configuration</h2>
        <p>Identifier: {initial.machineId}</p>
        <p>Dose unit: {initial.doseUnit}</p>
        <p>Pumps: {initial.pumps.join(", ")}</p>
        <p>Adapter: {initial.hardware.adapter}</p>
        <p>Base URL configured: {initial.hardware.baseUrlConfigured ? "Yes" : "No"}</p>
        <p>Commands enabled: {initial.hardware.commandsEnabled ? "Yes" : "No"}</p>
        <p>Integration confirmed: {initial.hardware.integrationConfirmed ? "Yes" : "No"}</p>
        <p>Hardware verified now: {initial.hardware.hardwareVerified ? "Yes" : "No"}</p>
        <p className="text-muted">{initial.softwareLimits.note}</p>
        <p>Max channel {initial.softwareLimits.maxChannelDoseGrams} g</p>
        <p>Max total {initial.softwareLimits.maxTotalDoseGrams} g</p>
        <p>Max pump test {initial.softwareLimits.maxPumpTestGrams} g / {initial.softwareLimits.maxPumpTestDurationMs} ms</p>
        <p className="text-muted">
          The ESP32 address is configured on the backend only. These display settings do not change firmware safety limits. Authentication is not enabled for this single-operator local prototype.
        </p>
      </aside>
    </div>
  );
}
