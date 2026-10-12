package com.colortinting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.machine.adapter=simulated",
        "app.machine.polling-enabled=false",
        "app.machine.commands-enabled=false",
        "app.machine.integration-confirmed=false"
})
class ApiIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReportsDatabaseWithoutClaimingVerifiedHardware() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.hardwareVerified").value(false))
                .andExpect(jsonPath("$.simulated").value(true));
    }

    @Test
    void recipeValidationAndBatchSnapshotStayIndependent() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","targetColor":"red","cyanDose":"-1","magentaDose":"0","yellowDose":"0","blackDose":"0","doseUnit":"LITERS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        MvcResult created = mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Teal sample","description":"Demo","targetColor":"#208094","cyanDose":"12.5","magentaDose":"1","yellowDose":"0.25","blackDose":"0","doseUnit":"GRAMS","basePaintQuantity":"1000"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalColorant").value("13.750"))
                .andReturn();
        String recipeId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"teal sample","targetColor":"#208094","cyanDose":"1","magentaDose":"0","yellowDose":"0","blackDose":"0","doseUnit":"GRAMS"}
                                """))
                .andExpect(status().isConflict());

        MvcResult started = mockMvc.perform(post("/api/v1/commands/batches/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipeId":"%s","idempotencyKey":"start-key-001"}
                                """.formatted(recipeId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.lifecycle").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.simulated").value(true))
                .andExpect(jsonPath("$.lifecycle", not("SUCCEEDED")))
                .andReturn();
        String batchId = com.jayway.jsonpath.JsonPath.read(started.getResponse().getContentAsString(), "$.batchId");
        String commandId = com.jayway.jsonpath.JsonPath.read(started.getResponse().getContentAsString(), "$.commandId");

        mockMvc.perform(post("/api/v1/commands/batches/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipeId":"%s","idempotencyKey":"start-key-001"}
                                """.formatted(recipeId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.commandId").value(commandId));

        mockMvc.perform(get("/api/v1/batches/" + batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("STARTING"))
                .andExpect(jsonPath("$.cyanTarget").value("12.500"))
                .andExpect(jsonPath("$.actualTotalGrams").value(nullValue()));

        mockMvc.perform(put("/api/v1/recipes/" + recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Teal sample","description":"Edited","targetColor":"#000000","cyanDose":"1","magentaDose":"0","yellowDose":"0","blackDose":"0","doseUnit":"GRAMS"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/batches/" + batchId))
                .andExpect(jsonPath("$.recipeName").value("Teal sample"))
                .andExpect(jsonPath("$.cyanTarget").value("12.500"))
                .andExpect(jsonPath("$.targetColor").value("#208094"));

        mockMvc.perform(post("/api/v1/machine/fixture/batch-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"COMPLETED","actualTotalGrams":"13.5","cyanActualGrams":"12.25","magentaActualGrams":"1","yellowActualGrams":"0.25","blackActualGrams":"0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulated").value("true"));

        mockMvc.perform(get("/api/v1/batches/" + batchId))
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.actualTotalGrams").value("13.500"))
                .andExpect(jsonPath("$.pumps[0].actualGrams").value("12.250"));

        mockMvc.perform(get("/api/v1/batches").param("state", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void simulatedCommunicationErrorIsNotPresentedAsLiveData() throws Exception {
        mockMvc.perform(post("/api/v1/machine/fixture/scenario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"COMMUNICATION_ERROR\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/machine/telemetry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.hardwareVerified").value(false))
                .andExpect(jsonPath("$.color.available").value(false))
                .andExpect(jsonPath("$.color.red").value(nullValue()))
                .andExpect(jsonPath("$.loadCell.weightGrams").value(nullValue()))
                .andExpect(jsonPath("$.message", containsString("Simulated")));
        mockMvc.perform(post("/api/v1/machine/fixture/scenario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"NOMINAL\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void defaultStatusDoesNotClaimAConnectedMachineBeforeAcknowledgedHardware() throws Exception {
        mockMvc.perform(get("/api/v1/machine/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hardwareVerified").value(false))
                .andExpect(jsonPath("$.controls.startBatch").value(false))
                .andExpect(jsonPath("$.controls.emergencyStop").value(false));
    }
}
