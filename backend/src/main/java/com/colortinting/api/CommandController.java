package com.colortinting.api;

import com.colortinting.domain.ModelEnums.CommandLifecycle;
import com.colortinting.persistence.MachineCommandEntity;
import com.colortinting.service.CommandService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/commands")
public class CommandController {
    private final CommandService commands;
    private final ApiPresenter presenter;

    public CommandController(CommandService commands, ApiPresenter presenter) {
        this.commands = commands;
        this.presenter = presenter;
    }

    @PostMapping("/batches/start")
    public ResponseEntity<ApiPresenter.CommandResponse> start(@Valid @RequestBody Requests.StartBatchBody body) {
        return respond(commands.startBatch(body.recipeId(), body.idempotencyKey()));
    }

    @PostMapping("/batches/{batchId}/cancel")
    public ResponseEntity<ApiPresenter.CommandResponse> cancel(
            @PathVariable UUID batchId,
            @Valid @RequestBody Requests.IdempotentBody body
    ) {
        return respond(commands.cancelBatch(batchId, body.idempotencyKey()));
    }

    @PostMapping("/tare")
    public ResponseEntity<ApiPresenter.CommandResponse> tare(@Valid @RequestBody Requests.IdempotentBody body) {
        return respond(commands.tare(body.idempotencyKey()));
    }

    @PostMapping("/sensor/calibrate")
    public ResponseEntity<ApiPresenter.CommandResponse> calibrate(@Valid @RequestBody Requests.IdempotentBody body) {
        return respond(commands.calibrate(body.idempotencyKey()));
    }

    @PostMapping("/pumps/test")
    public ResponseEntity<ApiPresenter.CommandResponse> pumpTest(@Valid @RequestBody Requests.PumpTestBody body) {
        return respond(commands.pumpTest(body.channel(), body.quantityGrams(), body.durationMs(), body.idempotencyKey()));
    }

    @PostMapping("/emergency-stop")
    public ResponseEntity<ApiPresenter.CommandResponse> emergencyStop(@Valid @RequestBody Requests.IdempotentBody body) {
        return respond(commands.emergencyStop(body.idempotencyKey()));
    }

    @PostMapping("/{commandId}/resolve-unknown")
    public ResponseEntity<ApiPresenter.CommandResponse> resolve(
            @PathVariable UUID commandId,
            @Valid @RequestBody Requests.ResolveUnknownBody body
    ) {
        return respond(commands.resolveUnknown(commandId, body.note()));
    }

    @GetMapping("/{commandId}")
    public ApiPresenter.CommandResponse get(@PathVariable UUID commandId) {
        return presenter.command(commands.get(commandId));
    }

    private ResponseEntity<ApiPresenter.CommandResponse> respond(MachineCommandEntity command) {
        CommandLifecycle lifecycle = command.getLifecycle();
        return ResponseEntity.status(presenter.commandStatus(lifecycle)).body(presenter.command(command));
    }
}
