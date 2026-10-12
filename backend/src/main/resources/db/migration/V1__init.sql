CREATE TABLE recipes (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    target_color VARCHAR(7) NOT NULL,
    cyan_dose NUMERIC(12, 3) NOT NULL,
    magenta_dose NUMERIC(12, 3) NOT NULL,
    yellow_dose NUMERIC(12, 3) NOT NULL,
    black_dose NUMERIC(12, 3) NOT NULL,
    dose_unit VARCHAR(16) NOT NULL,
    base_paint_quantity NUMERIC(12, 3),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT recipes_color_hex CHECK (target_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT recipes_unit_check CHECK (dose_unit IN ('GRAMS')),
    CONSTRAINT recipes_doses_nonnegative CHECK (
        cyan_dose >= 0
        AND magenta_dose >= 0
        AND yellow_dose >= 0
        AND black_dose >= 0
        AND (base_paint_quantity IS NULL OR base_paint_quantity >= 0)
    )
);

CREATE UNIQUE INDEX recipes_name_lower_unique ON recipes (lower(name));

CREATE TABLE batches (
    id UUID PRIMARY KEY,
    recipe_id UUID REFERENCES recipes (id) ON DELETE SET NULL,
    machine_id VARCHAR(80) NOT NULL,
    state VARCHAR(32) NOT NULL,
    recipe_name VARCHAR(120) NOT NULL,
    recipe_description VARCHAR(2000),
    target_color VARCHAR(7) NOT NULL,
    cyan_target NUMERIC(12, 3) NOT NULL,
    magenta_target NUMERIC(12, 3) NOT NULL,
    yellow_target NUMERIC(12, 3) NOT NULL,
    black_target NUMERIC(12, 3) NOT NULL,
    dose_unit VARCHAR(16) NOT NULL,
    base_paint_quantity NUMERIC(12, 3),
    target_total_quantity NUMERIC(12, 3) NOT NULL,
    actual_total_quantity NUMERIC(12, 3),
    outcome_unknown BOOLEAN NOT NULL DEFAULT FALSE,
    failure_detail VARCHAR(2000),
    cancellation_detail VARCHAR(2000),
    start_command_id UUID,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT batches_state_check CHECK (
        state IN (
            'STARTING',
            'DISPENSING',
            'PAUSED',
            'CANCELLING',
            'COMPLETED',
            'CANCELLED',
            'FAILED',
            'UNKNOWN'
        )
    ),
    CONSTRAINT batches_unit_check CHECK (dose_unit IN ('GRAMS')),
    CONSTRAINT batches_actual_nonnegative CHECK (
        actual_total_quantity IS NULL OR actual_total_quantity >= 0
    )
);

CREATE UNIQUE INDEX batches_one_active_per_machine
    ON batches (machine_id)
    WHERE state IN ('STARTING', 'DISPENSING', 'PAUSED', 'CANCELLING');

CREATE INDEX batches_state_started_idx ON batches (state, started_at DESC);
CREATE INDEX batches_started_at_idx ON batches (started_at DESC);

CREATE TABLE batch_pump_results (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES batches (id) ON DELETE CASCADE,
    channel VARCHAR(16) NOT NULL,
    target_quantity NUMERIC(12, 3) NOT NULL,
    actual_quantity NUMERIC(12, 3),
    dose_unit VARCHAR(16) NOT NULL,
    CONSTRAINT batch_pump_channel_check CHECK (channel IN ('CYAN', 'MAGENTA', 'YELLOW', 'BLACK')),
    CONSTRAINT batch_pump_unit_check CHECK (dose_unit IN ('GRAMS')),
    CONSTRAINT batch_pump_target_nonnegative CHECK (target_quantity >= 0),
    CONSTRAINT batch_pump_actual_nonnegative CHECK (actual_quantity IS NULL OR actual_quantity >= 0),
    CONSTRAINT batch_pump_channel_unique UNIQUE (batch_id, channel)
);

CREATE TABLE machine_commands (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(80) NOT NULL,
    command_type VARCHAR(40) NOT NULL,
    lifecycle VARCHAR(32) NOT NULL,
    batch_id UUID REFERENCES batches (id) ON DELETE SET NULL,
    machine_id VARCHAR(80) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    dispatched_to_hardware BOOLEAN NOT NULL,
    simulated BOOLEAN NOT NULL,
    reconciliation_required BOOLEAN NOT NULL DEFAULT FALSE,
    reconciliation_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT machine_commands_idempotency_unique UNIQUE (idempotency_key),
    CONSTRAINT machine_commands_type_check CHECK (
        command_type IN (
            'START_BATCH',
            'CANCEL_BATCH',
            'TARE',
            'CALIBRATE_SENSOR',
            'PUMP_TEST',
            'EMERGENCY_STOP'
        )
    ),
    CONSTRAINT machine_commands_lifecycle_check CHECK (
        lifecycle IN (
            'PENDING',
            'ACKNOWLEDGED',
            'EXECUTING',
            'SUCCEEDED',
            'REJECTED',
            'TIMED_OUT',
            'FAILED'
        )
    )
);

CREATE INDEX machine_commands_created_idx ON machine_commands (created_at DESC);
CREATE INDEX machine_commands_reconcile_idx
    ON machine_commands (reconciliation_required, reconciliation_resolved);

CREATE TABLE machine_events (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    severity VARCHAR(16) NOT NULL,
    code VARCHAR(80) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    batch_id UUID REFERENCES batches (id) ON DELETE SET NULL,
    command_id UUID REFERENCES machine_commands (id) ON DELETE SET NULL,
    simulated BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT machine_events_severity_check CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL'))
);

CREATE INDEX machine_events_occurred_idx ON machine_events (occurred_at DESC);

CREATE TABLE operator_settings (
    id SMALLINT PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    telemetry_refresh_interval_ms INTEGER NOT NULL,
    show_reading_timestamps BOOLEAN NOT NULL,
    compact_layout BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT operator_settings_singleton CHECK (id = 1),
    CONSTRAINT operator_settings_refresh_range CHECK (
        telemetry_refresh_interval_ms BETWEEN 1000 AND 30000
    )
);

INSERT INTO operator_settings (
    id,
    display_name,
    telemetry_refresh_interval_ms,
    show_reading_timestamps,
    compact_layout,
    updated_at
) VALUES (
    1,
    'Color Tinting Machine',
    2000,
    TRUE,
    FALSE,
    TIMESTAMPTZ '2026-01-01T00:00:00Z'
);
