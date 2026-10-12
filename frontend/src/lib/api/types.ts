export type ConnectionState =
  | "CONNECTED"
  | "CONNECTING"
  | "DISCONNECTED"
  | "STALE"
  | "COMMUNICATION_ERROR";

export type DataSource = "LIVE" | "SIMULATED" | "UNAVAILABLE";

export type BatchState =
  | "STARTING"
  | "DISPENSING"
  | "PAUSED"
  | "CANCELLING"
  | "COMPLETED"
  | "CANCELLED"
  | "FAILED"
  | "UNKNOWN";

export type PumpState = "ON" | "OFF" | "STARTING" | "STOPPING" | "FAULT" | "UNKNOWN";

export type CommandLifecycle =
  | "PENDING"
  | "ACKNOWLEDGED"
  | "EXECUTING"
  | "SUCCEEDED"
  | "REJECTED"
  | "TIMED_OUT"
  | "FAILED";

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  timestamp?: string;
  status: number;
  errorCode: string;
  message: string;
  correlationId?: string;
  fieldErrors?: FieldError[];
  lifecycle?: CommandLifecycle;
  commandId?: string;
}

export interface Recipe {
  id: string;
  name: string;
  description: string | null;
  targetColor: string;
  cyanDose: string;
  magentaDose: string;
  yellowDose: string;
  blackDose: string;
  doseUnit: string;
  basePaintQuantity: string | null;
  totalColorant: string;
  createdAt: string;
  updatedAt: string;
}

export interface RecipeInput {
  name: string;
  description: string;
  targetColor: string;
  cyanDose: string;
  magentaDose: string;
  yellowDose: string;
  blackDose: string;
  doseUnit: "GRAMS";
  basePaintQuantity: string;
}

export interface PumpResult {
  channel: string;
  targetGrams: string;
  actualGrams: string | null;
  doseUnit: string;
}

export interface Batch {
  id: string;
  recipeId: string | null;
  machineId: string;
  state: BatchState;
  recipeName: string;
  recipeDescription: string | null;
  targetColor: string;
  cyanTarget: string;
  magentaTarget: string;
  yellowTarget: string;
  blackTarget: string;
  doseUnit: string;
  basePaintQuantity: string | null;
  targetTotalGrams: string;
  actualTotalGrams: string | null;
  outcomeUnknown: boolean;
  failureDetail: string | null;
  cancellationDetail: string | null;
  startCommandId: string | null;
  startedAt: string | null;
  completedAt: string | null;
  pumps: PumpResult[];
}

export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Fault {
  code: string;
  message: string;
  severity: string;
}

export interface TrackedPump {
  channel: string;
  targetGrams: string;
  actualGrams: string | null;
}

export interface TrackedBatch {
  id: string;
  recipeId: string | null;
  recipeName: string;
  targetColor: string;
  state: BatchState;
  targetTotalGrams: string;
  actualTotalGrams: string | null;
  outcomeUnknown: boolean;
  failureDetail: string | null;
  cancellationDetail: string | null;
  startedAt: string | null;
  completedAt: string | null;
  pumps: TrackedPump[];
}

export interface Controls {
  startBatch: boolean;
  cancelBatch: boolean;
  tare: boolean;
  calibrateSensor: boolean;
  pumpTest: boolean;
  emergencyStop: boolean;
  reasons: string[];
}

export interface MachineStatus {
  machineId: string;
  displayName: string;
  connectionState: ConnectionState;
  dataSource: DataSource;
  simulated: boolean;
  hardwareVerified: boolean;
  commandsEnabled: boolean;
  integrationConfirmed: boolean;
  baseUrlConfigured: boolean;
  reconciliationRequired: boolean;
  operatingState: string;
  firmwareVersion: string | null;
  lastSuccessfulTelemetryAt: string | null;
  stale: boolean;
  probed: boolean;
  message: string;
  faults: Fault[];
  trackedBatch: TrackedBatch | null;
  controls: Controls;
}

export interface ColorReading {
  available: boolean;
  calibrationState: string | null;
  red: number | null;
  green: number | null;
  blue: number | null;
  readAt: string | null;
}

export interface LoadReading {
  available: boolean;
  weightGrams: string | null;
  tareState: string | null;
  readAt: string | null;
}

export interface PumpStatus {
  channel: string;
  state: PumpState;
  dispensedGrams: string | null;
  targetGrams: string | null;
  fault: string | null;
}

export interface LastKnownTelemetry {
  observedAt: string | null;
  dataSource: DataSource;
  simulated: boolean;
  color: ColorReading;
  loadCell: LoadReading;
  pumps: PumpStatus[];
  message: string;
}

export interface Telemetry {
  dataSource: DataSource;
  simulated: boolean;
  stale: boolean;
  observedAt: string | null;
  hardwareVerified: boolean;
  operatingState: string;
  message: string;
  activeBatchId: string | null;
  color: ColorReading;
  loadCell: LoadReading;
  pumps: PumpStatus[];
  lastKnown: LastKnownTelemetry | null;
}

export interface CommandResult {
  commandId: string;
  idempotencyKey: string;
  type: string;
  lifecycle: CommandLifecycle;
  batchId: string | null;
  message: string;
  dispatchedToHardware: boolean;
  simulated: boolean;
  reconciliationRequired: boolean;
  reconciliationResolved: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MachineEvent {
  id: string;
  occurredAt: string;
  severity: string;
  code: string;
  message: string;
  batchId: string | null;
  commandId: string | null;
  simulated: boolean;
}

export interface Settings {
  displayName: string;
  machineId: string;
  telemetryRefreshIntervalMs: number;
  showReadingTimestamps: boolean;
  compactLayout: boolean;
  doseUnit: string;
  pumps: string[];
  softwareLimits: {
    maxChannelDoseGrams: string;
    maxTotalDoseGrams: string;
    maxPumpTestGrams: string;
    minPumpTestDurationMs: number;
    maxPumpTestDurationMs: number;
    note: string;
  };
  hardware: {
    adapter: string;
    commandsEnabled: boolean;
    integrationConfirmed: boolean;
    baseUrlConfigured: boolean;
    hardwareVerified: boolean;
  };
}
