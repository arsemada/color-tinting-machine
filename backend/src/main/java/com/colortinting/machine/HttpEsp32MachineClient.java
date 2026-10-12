package com.colortinting.machine;

import com.colortinting.config.AppProperties;
import com.colortinting.domain.ModelEnums.ConnectionState;
import com.colortinting.domain.ModelEnums.DeviceCallStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP adapter for the provisional ESP32 contract.
 * Command requests are not sent unless integration is explicitly confirmed.
 */
public class HttpEsp32MachineClient implements Esp32MachineClient {
    private static final Logger log = LoggerFactory.getLogger(HttpEsp32MachineClient.class);

    private final AppProperties.Machine machine;
    private final RestClient restClient;
    private final Clock clock;

    public HttpEsp32MachineClient(AppProperties.Machine machine, RestClient restClient, Clock clock) {
        this.machine = machine;
        this.restClient = restClient;
        this.clock = clock;
    }

    public static HttpEsp32MachineClient create(AppProperties.Machine machine, Clock clock) {
        if (machine.getBaseUrl() == null || machine.getBaseUrl().isBlank()) {
            return new HttpEsp32MachineClient(machine, null, clock);
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(machine.getConnectTimeoutMs()))
                .build();
        org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(machine.getReadTimeoutMs()));
        RestClient client = RestClient.builder()
                .baseUrl(trimTrailingSlash(machine.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();
        return new HttpEsp32MachineClient(machine, client, clock);
    }

    @Override
    public Observation probe() {
        if (restClient == null) {
            return Observation.unavailable(
                    ConnectionState.DISCONNECTED,
                    "ESP32 base URL is not configured. No live readings are available.",
                    List.of(new Observation.FaultSignal(
                            "ESP32_NOT_CONFIGURED",
                            "The machine base URL is empty.",
                            "WARNING"
                    ))
            );
        }
        int attempts = Math.max(1, machine.getSafeRetryCount() + 1);
        RestClientException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Esp32TelemetryPayload payload = restClient.get()
                        .uri(machine.getPaths().getTelemetry())
                        .retrieve()
                        .body(Esp32TelemetryPayload.class);
                return TelemetryInterpreter.interpret(payload, false, clock.instant());
            } catch (RestClientException ex) {
                last = ex;
                log.warn("ESP32 telemetry probe failed on attempt {}: {}", attempt, safeMessage(ex));
            }
        }
        return Observation.unavailable(
                ConnectionState.COMMUNICATION_ERROR,
                "The ESP32 did not return telemetry. " + safeMessage(last),
                List.of(new Observation.FaultSignal(
                        "ESP32_COMMUNICATION",
                        "Telemetry request failed: " + safeMessage(last),
                        "ERROR"
                ))
        );
    }

    @Override
    public Observation.DeviceAck dispatch(Observation.DeviceCommand command) {
        if (!machine.realCommandsPermitted()) {
            return Observation.DeviceAck.notSent(
                    "Hardware commands are disabled until the ESP32 API and safety behavior are confirmed by the hardware developer."
            );
        }
        if (restClient == null) {
            return new Observation.DeviceAck(
                    DeviceCallStatus.FAILED,
                    "ESP32 base URL is not configured. The command was not sent.",
                    false
            );
        }
        try {
            Esp32CommandPayload response = restClient.post()
                    .uri(pathFor(command.type()))
                    .body(bodyFor(command))
                    .retrieve()
                    .body(Esp32CommandPayload.class);
            return mapResponse(response);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status >= 400 && status < 500) {
                return new Observation.DeviceAck(
                        DeviceCallStatus.REJECTED,
                        "The device rejected the command with HTTP " + status + ".",
                        true
                );
            }
            return new Observation.DeviceAck(
                    DeviceCallStatus.FAILED,
                    "The device returned HTTP " + status + ". The command was not retried.",
                    true
            );
        } catch (RestClientException ex) {
            if (isTimeout(ex)) {
                return new Observation.DeviceAck(
                        DeviceCallStatus.TIMED_OUT,
                        "The device did not respond before the timeout. The command was not retried because it may have been received.",
                        true
                );
            }
            return new Observation.DeviceAck(
                    DeviceCallStatus.FAILED,
                    "The device could not be reached. The command was not retried.",
                    false
            );
        }
    }

    @Override
    public boolean simulated() {
        return false;
    }

    @Override
    public String adapterName() {
        return "http";
    }

    private Observation.DeviceAck mapResponse(Esp32CommandPayload response) {
        if (response == null) {
            return new Observation.DeviceAck(
                    DeviceCallStatus.FAILED,
                    "The device returned an empty command response.",
                    true
            );
        }
        if (Boolean.FALSE.equals(response.accepted())) {
            String message = response.message() == null ? "The device rejected the command." : response.message();
            return new Observation.DeviceAck(DeviceCallStatus.REJECTED, message, true);
        }
        DeviceCallStatus status = parseStatus(response.state());
        if (status == null) {
            status = DeviceCallStatus.ACKNOWLEDGED;
        }
        if (status == DeviceCallStatus.NOT_SENT) {
            status = DeviceCallStatus.ACKNOWLEDGED;
        }
        String message = response.message() == null ? "The device acknowledged the command." : response.message();
        return new Observation.DeviceAck(status, message, true);
    }

    private static DeviceCallStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return DeviceCallStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String pathFor(String type) {
        AppProperties.Paths paths = machine.getPaths();
        return switch (type) {
            case "START_BATCH" -> paths.getBatchStart();
            case "CANCEL_BATCH" -> paths.getBatchCancel();
            case "TARE" -> paths.getTare();
            case "CALIBRATE_SENSOR" -> paths.getCalibrate();
            case "PUMP_TEST" -> paths.getPumpTest();
            case "EMERGENCY_STOP" -> paths.getEmergencyStop();
            default -> throw new IllegalArgumentException("Unsupported command type");
        };
    }

    private Map<String, Object> bodyFor(Observation.DeviceCommand command) {
        return Map.of(
                "commandId", command.commandId().toString(),
                "type", command.type(),
                "batchId", command.batchId() == null ? "" : command.batchId().toString(),
                "channel", command.pumpChannel() == null ? "" : command.pumpChannel().name(),
                "quantityGrams", command.pumpQuantity() == null ? "" : command.pumpQuantity().toPlainString(),
                "durationMs", command.durationMs() == null ? 0 : command.durationMs(),
                "targetTotalGrams", command.targetTotal() == null ? "" : command.targetTotal().toPlainString()
        );
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof HttpTimeoutException || current instanceof java.net.SocketTimeoutException) {
                return true;
            }
            if (current instanceof ResourceAccessException && current.getCause() != null && current.getCause() != current) {
                current = current.getCause();
                continue;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String safeMessage(Throwable ex) {
        if (ex == null || ex.getMessage() == null) {
            return "communication failed";
        }
        return ex.getMessage().replaceAll("(?i)(password|secret|token)=\\S+", "$1=(redacted)");
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
