"use client";

import { useState } from "react";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Panel } from "@/components/ui/Panel";
import { Button } from "@/components/ui/Button";
import { useCommand } from "@/hooks/use-api";
import type { CommandResult, MachineStatus } from "@/lib/api/types";
import { createCommandGuard } from "@/lib/utils/telemetry";
import {
  Play,
  XCircle,
  Scale,
  Sparkles,
  FlaskConical,
  OctagonAlert,
  ShieldCheck,
  ShieldAlert,
  Info,
} from "lucide-react";

export function MachineControls({
  status,
  recipeId,
}: {
  status?: MachineStatus;
  recipeId?: string;
}) {
  const controls = status?.controls;
  const start = useCommand("/api/v1/commands/batches/start");
  const cancel = useCommand(
    status?.trackedBatch
      ? `/api/v1/commands/batches/${status.trackedBatch.id}/cancel`
      : "/api/v1/commands/batches/cancel"
  );
  const tare = useCommand("/api/v1/commands/tare");
  const calibrate = useCommand("/api/v1/commands/sensor/calibrate");
  const pump = useCommand("/api/v1/commands/pumps/test");
  const estop = useCommand("/api/v1/commands/emergency-stop");
  const [guard] = useState(() => createCommandGuard());
  const [dialog, setDialog] = useState<string | null>(null);
  const [result, setResult] = useState<CommandResult | null>(null);
  const [channel, setChannel] = useState("CYAN");

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

  const busy =
    start.isPending ||
    cancel.isPending ||
    tare.isPending ||
    calibrate.isPending ||
    pump.isPending ||
    estop.isPending;

  const isVerified = Boolean(status?.hardwareVerified);

  return (
    <Panel title="Dispenser Controls">
      {!isVerified ? (
        <div className="mb-4 flex items-start gap-3 rounded-xl border border-amber-200/90 bg-amber-50/70 p-3.5 text-xs text-amber-900">
          <ShieldAlert size={18} className="shrink-0 text-amber-600 mt-0.5" />
          <div className="leading-relaxed">
            <span className="font-semibold block mb-0.5">Safety Lock Active</span>
            Hardware dispensing actions stay locked until a verified ESP32 machine is connected and reporting ready.
          </div>
        </div>
      ) : (
        <div className="mb-4 flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50/70 p-3 text-xs text-emerald-900">
          <ShieldCheck size={16} className="text-emerald-600" />
          <span>Hardware verified & live. Controls are armed.</span>
        </div>
      )}

      {/* Primary Operation Grid */}
      <div className="grid gap-2.5 sm:grid-cols-2">
        <Button
          type="button"
          tone="primary"
          disabled={!controls?.startBatch || !recipeId || busy}
          onClick={() => setDialog("start")}
          className="py-3 font-semibold text-sm shadow-md"
        >
          <Play size={16} className="fill-current" />
          Start Batch
        </Button>
        <Button
          type="button"
          disabled={!controls?.cancelBatch || busy}
          onClick={() => setDialog("cancel")}
          className="py-3"
        >
          <XCircle size={16} />
          Cancel Batch
        </Button>
        <Button
          type="button"
          disabled={!controls?.tare || busy}
          onClick={() => setDialog("tare")}
          className="py-3"
        >
          <Scale size={16} />
          Tare Scale
        </Button>
        <Button
          type="button"
          disabled={!controls?.calibrateSensor || busy}
          onClick={() => setDialog("calibrate")}
          className="py-3"
        >
          <Sparkles size={16} />
          Calibrate Sensor
        </Button>
      </div>

      {/* Pump Diagnostic Mode */}
      <div className="mt-4 rounded-xl border border-slate-200/80 bg-slate-50/70 p-3.5">
        <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">
          Diagnostic Pump Prime
        </p>
        <div className="grid gap-2.5 sm:grid-cols-[1fr_auto]">
          <div className="relative">
            <select
              className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 shadow-xs focus:border-teal-600 focus:outline-none"
              value={channel}
              onChange={(event) => setChannel(event.target.value)}
              aria-label="Pump test channel"
            >
              <option value="CYAN">Cyan Pump (Channel 1)</option>
              <option value="MAGENTA">Magenta Pump (Channel 2)</option>
              <option value="YELLOW">Yellow Pump (Channel 3)</option>
              <option value="BLACK">Black Pump (Channel 4)</option>
            </select>
          </div>
          <Button
            className="self-end px-5 py-2"
            type="button"
            disabled={!controls?.pumpTest || busy}
            onClick={() => setDialog("pump")}
          >
            <FlaskConical size={16} />
            Test 1g Dose
          </Button>
        </div>
      </div>

      {/* Emergency Stop Button */}
      <div className="mt-5 border-t border-slate-100 pt-4">
        <Button
          className="w-full py-3.5 font-bold tracking-wide uppercase shadow-md shadow-rose-600/20"
          type="button"
          tone="danger"
          disabled={!controls?.emergencyStop || busy}
          onClick={() => setDialog("estop")}
        >
          <OctagonAlert size={18} />
          Software Emergency Stop
        </Button>
        <div className="mt-2 flex items-center justify-center gap-1.5 text-center text-xs text-slate-400">
          <Info size={13} />
          <span>Physical red E-Stop button on the machine casing works unconditionally.</span>
        </div>
      </div>

      {/* Last Command Audit Output */}
      {result ? (
        <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-3.5 text-xs">
          <div className="flex items-center justify-between font-bold text-slate-700">
            <span>COMMAND RESULT</span>
            <span className="rounded-md bg-white border border-slate-200 px-2 py-0.5 text-[10px]">
              {result.lifecycle}
            </span>
          </div>
          <p className="mt-1 font-medium text-slate-800">{result.message}</p>
          <p className="mt-1 text-slate-500">
            {result.dispatchedToHardware ? "✓ Dispatched to hardware adapter." : "Notice: Not sent to physical hardware."}
            {result.simulated ? " (Executed in simulated test harness)" : ""}
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
            })
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
