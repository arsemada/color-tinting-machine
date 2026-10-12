package com.colortinting.machine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Esp32CommandPayload(
        String commandId,
        Boolean accepted,
        String state,
        String message
) {
}
