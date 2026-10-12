package com.colortinting.machine;

import com.colortinting.config.AppProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpEsp32MachineClientTest {
    @Test
    void reportsDisconnectedWhenTheBaseUrlIsMissing() {
        AppProperties.Machine machine = new AppProperties.Machine();
        machine.setBaseUrl("");
        machine.setCommandsEnabled(true);
        machine.setIntegrationConfirmed(true);
        HttpEsp32MachineClient client = HttpEsp32MachineClient.create(machine, Clock.systemUTC());
        Observation observation = client.probe();
        assertFalse(observation.success());
        assertEquals("DISCONNECTED", observation.connectionState().name());
        assertNull(observation.color().red());
        assertNull(observation.load().weightGrams());
        Observation.DeviceAck ack = client.dispatch(sampleCommand());
        assertFalse(ack.dispatched());
    }

    @Test
    void doesNotSendCommandsUntilIntegrationIsConfirmed() throws Exception {
        AtomicInteger posts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/telemetry", exchange -> {
            byte[] body = """
                    {"connectionState":"CONNECTED","operatingState":"IDLE","color":{"available":false},"loadCell":{"available":false}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/api/v1/commands/emergency-stop", exchange -> {
            posts.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            AppProperties.Machine machine = new AppProperties.Machine();
            machine.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            machine.setCommandsEnabled(true);
            machine.setIntegrationConfirmed(false);
            machine.setSafeRetryCount(0);
            HttpEsp32MachineClient client = HttpEsp32MachineClient.create(machine, Clock.systemUTC());
            Observation observation = client.probe();
            assertEquals("LIVE", observation.dataSource().name());
            assertFalse(observation.color().available());
            Observation.DeviceAck ack = client.dispatch(sampleCommand());
            assertEquals("NOT_SENT", ack.status().name());
            assertFalse(ack.dispatched());
            assertEquals(0, posts.get());
        } finally {
            server.stop(0);
        }
    }

    private static Observation.DeviceCommand sampleCommand() {
        return new Observation.DeviceCommand(
                java.util.UUID.randomUUID(),
                "EMERGENCY_STOP",
                null,
                null,
                java.util.Map.of(),
                null,
                null,
                null,
                null
        );
    }
}
