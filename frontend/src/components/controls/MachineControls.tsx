"use client";

import { useState } from "react";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Panel } from "@/components/ui/Panel";
import { Button } from "@/components/ui/Button";
import { useCommand } from "@/hooks/use-api";
import type { CommandResult, MachineStatus } from "@/lib/api/types";
import { createCommandGuard } from "@/lib/utils/telemetry";

export function MachineControls({
  status,
  recipeId,
}: {
  status?: MachineStatus;
  recipeId?: string;
}) {
  const controls = status?.controls;
  const start = useCommand("/api/v1/commands/batches/start");
  const cancel = useCommand(status?.trackedBatch ? `/api/v1/commands/batches/${status.trackedBatch.id}/cancel` : "/api/v1/commands/batches/cancel");
  const tare = useCommand("/api/v1/commands/tare");
  const calibrate = useCommand("/api/v1/commands/sensor/calibrate");
  const pump = useCommand("/api/v1/commands/pumps/test");
  const estop = useCommand("/api/v1/commands/emergency-stop");
  const [guard] = useState(() => createCommandGuard());
  const [dialog, setDialog] = useState<string | null>(null);
  const [result, setResult] = useState<CommandResult | null>(null);
  const [channel, setChannel] = useState("CYAN");
  const reasons = controls?.reasons ?? ["Machine status has not been verified."];

  async function send(action: (key: string) => Promise<CommandResult>) {
    const key = guard.begin();
    if (!key) {
      return;
    }
    try {
      setResult(await action(key));
    } finally {
      guard.finish();
    }
  }

  const busy = start.isPending || cancel.isPending || tare.isPending || calibrate.isPending || pump.isPending || estop.isPending;

  return (
    <Panel title="Machine controls">
      <p className="mb-3 text-sm text-muted">
        Controls stay unavailable until a verified ESP32 reports a ready state. A click is a request, not confirmation that a pump moved.
      </p>
      <div className="grid gap-2 sm:grid-cols-2">
        <Button type="button" tone="primary" disabled={!controls?.startBatch || !recipeId || busy} onClick={() => setDialog("start")}>
          Start batch
        </Button>
        <Button type="button" disabled={!controls?.cancelBatch || busy} onClick={() => setDialog("cancel")}>
          Cancel batch
        </Button>
        <Button type="button" disabled={!controls?.tare || busy} onClick={() => setDialog("tare")}>
          Tare scale
        </Button>
        <Button type="button" disabled={!controls?.calibrateSensor || busy} onClick={() => setDialog("calibrate")}>
          Calibrate sensor
        </Button>
      </div>
      <div className="mt-3 grid gap-2 sm:grid-cols-[1fr_auto]">
        <label className="text-sm">
          <span className="mb-1 block font-medium">Pump test channel</span>
          <select className="w-full rounded-md border border-line px-3 py-2" value={channel} onChange={(event) => setChannel(event.target.value)}>
            {["CYAN", "MAGENTA", "YELLOW", "BLACK"].map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
        </label>
        <Button className="self-end" type="button" disabled={!controls?.pumpTest || busy} onClick={() => setDialog("pump")}>
          Test one pump
        </Button>
      </div>
      <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3">
        <p className="text-sm font-semibold text-danger">Software emergency stop</p>
        <p className="mt-1 text-sm text-red-900">
          This is supplementary. The physical emergency stop and firmware must stop the pumps without this dashboard, the backend, or Wi-Fi.
          {!controls?.emergencyStop ? " The software command cannot be confirmed because hardware is not verified." : ""}
        </p>
        <Button className="mt-3" type="button" tone="danger" disabled={!controls?.emergencyStop || busy} onClick={() => setDialog("estop")}>
          Request emergency stop
        </Button>
      </div>
      {reasons.length > 0 && !status?.hardwareVerified ? (
        <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-muted">
          {reasons.map((reason) => (
            <li key={reason}>{reason}</li>
          ))}
        </ul>
      ) : null}
      {result ? (
        <div className="mt-3 rounded-lg bg-slate-50 p-3 text-sm">
          <p className="font-medium">Command {result.lifecycle}</p>
          <p>{result.message}</p>
          <p className="text-muted">
            {result.dispatchedToHardware ? "Dispatched to the adapter." : "Not sent to hardware."}
            {result.simulated ? " Simulated fixture." : ""}
          </p>
        </div>
      ) : null}

      <ConfirmDialog
        open={dialog === "start"}
        title="Start this batch?"
        body="The backend will validate the recipe and send a start request only if hardware commands are confirmed. Acceptance does not mean the batch is complete."
        confirmLabel="Request start"
        onClose={() => setDialog(null)}
        onConfirm={() => {
          if (!recipeId) return;
          void send((key) => start.mutateAsync({ recipeId, idempotencyKey: key }));
        }}
      />
      <ConfirmDialog
        open={dialog === "cancel"}
        title="Request cancellation?"
        body="The batch stays in its current state until the machine reports CANCELLED."
        confirmLabel="Request cancel"
        onClose={() => setDialog(null)}
        onConfirm={() => void send((key) => cancel.mutateAsync({ idempotencyKey: key }))}
      />
      <ConfirmDialog
        open={dialog === "tare"}
        title="Request tare?"
        body="This sends a tare request. The dashboard will not set the displayed weight to zero locally."
        confirmLabel="Request tare"
        onClose={() => setDialog(null)}
        onConfirm={() => void send((key) => tare.mutateAsync({ idempotencyKey: key }))}
      />
      <ConfirmDialog
        open={dialog === "calibrate"}
        title="Request sensor calibration?"
        body="The hardware developer has not supplied a calibration procedure. This request stays disabled until that contract is confirmed, and success is never assumed."
        confirmLabel="Request calibration"
        onClose={() => setDialog(null)}
        onConfirm={() => void send((key) => calibrate.mutateAsync({ idempotencyKey: key }))}
      />
      <ConfirmDialog
        open={dialog === "pump"}
        title={`Test the ${channel.toLowerCase()} pump?`}
        body="Only one pump is requested. The test uses the configured software quantity and duration limits, and firmware must enforce its own limits."
        confirmLabel="Request pump test"
        onClose={() => setDialog(null)}
        onConfirm={() =>
          void send((key) =>
            pump.mutateAsync({
              channel,
              quantityGrams: "1.000",
              durationMs: 500,
              idempotencyKey: key,
            }),
          )
        }
      />
      <ConfirmDialog
        open={dialog === "estop"}
        title="Request software emergency stop?"
        body="This does not replace the physical emergency stop. The dashboard will not report the pumps as stopped unless the machine confirms it."
        confirmLabel="Send request"
        tone="danger"
        onClose={() => setDialog(null)}
        onConfirm={() => void send((key) => estop.mutateAsync({ idempotencyKey: key }))}
      />
    </Panel>
  );
}
