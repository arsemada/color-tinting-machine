package com.colortinting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Cors cors = new Cors();
    private final Machine machine = new Machine();

    public Cors getCors() {
        return cors;
    }

    public Machine getMachine() {
        return machine;
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Machine {
        private String id = "machine-1";
        private String displayName = "Color Tinting Machine";
        private String adapter = "http";
        private String baseUrl = "";
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 3000;
        private int staleAfterMs = 10000;
        private int pollIntervalMs = 2000;
        private boolean pollingEnabled = true;
        private boolean commandsEnabled = false;
        private boolean integrationConfirmed = false;
        private int safeRetryCount = 1;
        private final Limits limits = new Limits();
        private final Paths paths = new Paths();

        public boolean realCommandsPermitted() {
            return integrationConfirmed && commandsEnabled && "http".equalsIgnoreCase(adapter);
        }

        public boolean simulatedAdapter() {
            return "simulated".equalsIgnoreCase(adapter);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getAdapter() {
            return adapter;
        }

        public void setAdapter(String adapter) {
            this.adapter = adapter;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public int getStaleAfterMs() {
            return staleAfterMs;
        }

        public void setStaleAfterMs(int staleAfterMs) {
            this.staleAfterMs = staleAfterMs;
        }

        public int getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(int pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public boolean isPollingEnabled() {
            return pollingEnabled;
        }

        public void setPollingEnabled(boolean pollingEnabled) {
            this.pollingEnabled = pollingEnabled;
        }

        public boolean isCommandsEnabled() {
            return commandsEnabled;
        }

        public void setCommandsEnabled(boolean commandsEnabled) {
            this.commandsEnabled = commandsEnabled;
        }

        public boolean isIntegrationConfirmed() {
            return integrationConfirmed;
        }

        public void setIntegrationConfirmed(boolean integrationConfirmed) {
            this.integrationConfirmed = integrationConfirmed;
        }

        public int getSafeRetryCount() {
            return safeRetryCount;
        }

        public void setSafeRetryCount(int safeRetryCount) {
            this.safeRetryCount = safeRetryCount;
        }

        public Limits getLimits() {
            return limits;
        }

        public Paths getPaths() {
            return paths;
        }
    }

    public static class Limits {
        private BigDecimal maxChannelDoseGrams = new BigDecimal("500.000");
        private BigDecimal maxTotalDoseGrams = new BigDecimal("2000.000");
        private BigDecimal maxPumpTestGrams = new BigDecimal("5.000");
        private int minPumpTestDurationMs = 100;
        private int maxPumpTestDurationMs = 2000;

        public BigDecimal getMaxChannelDoseGrams() {
            return maxChannelDoseGrams;
        }

        public void setMaxChannelDoseGrams(BigDecimal maxChannelDoseGrams) {
            this.maxChannelDoseGrams = maxChannelDoseGrams;
        }

        public BigDecimal getMaxTotalDoseGrams() {
            return maxTotalDoseGrams;
        }

        public void setMaxTotalDoseGrams(BigDecimal maxTotalDoseGrams) {
            this.maxTotalDoseGrams = maxTotalDoseGrams;
        }

        public BigDecimal getMaxPumpTestGrams() {
            return maxPumpTestGrams;
        }

        public void setMaxPumpTestGrams(BigDecimal maxPumpTestGrams) {
            this.maxPumpTestGrams = maxPumpTestGrams;
        }

        public int getMinPumpTestDurationMs() {
            return minPumpTestDurationMs;
        }

        public void setMinPumpTestDurationMs(int minPumpTestDurationMs) {
            this.minPumpTestDurationMs = minPumpTestDurationMs;
        }

        public int getMaxPumpTestDurationMs() {
            return maxPumpTestDurationMs;
        }

        public void setMaxPumpTestDurationMs(int maxPumpTestDurationMs) {
            this.maxPumpTestDurationMs = maxPumpTestDurationMs;
        }
    }

    public static class Paths {
        private String telemetry = "/api/v1/telemetry";
        private String status = "/api/v1/status";
        private String batchStart = "/api/v1/commands/batch/start";
        private String batchCancel = "/api/v1/commands/batch/cancel";
        private String tare = "/api/v1/commands/tare";
        private String calibrate = "/api/v1/commands/sensor/calibrate";
        private String pumpTest = "/api/v1/commands/pump/test";
        private String emergencyStop = "/api/v1/commands/emergency-stop";

        public String getTelemetry() {
            return telemetry;
        }

        public void setTelemetry(String telemetry) {
            this.telemetry = telemetry;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getBatchStart() {
            return batchStart;
        }

        public void setBatchStart(String batchStart) {
            this.batchStart = batchStart;
        }

        public String getBatchCancel() {
            return batchCancel;
        }

        public void setBatchCancel(String batchCancel) {
            this.batchCancel = batchCancel;
        }

        public String getTare() {
            return tare;
        }

        public void setTare(String tare) {
            this.tare = tare;
        }

        public String getCalibrate() {
            return calibrate;
        }

        public void setCalibrate(String calibrate) {
            this.calibrate = calibrate;
        }

        public String getPumpTest() {
            return pumpTest;
        }

        public void setPumpTest(String pumpTest) {
            this.pumpTest = pumpTest;
        }

        public String getEmergencyStop() {
            return emergencyStop;
        }

        public void setEmergencyStop(String emergencyStop) {
            this.emergencyStop = emergencyStop;
        }
    }
}
