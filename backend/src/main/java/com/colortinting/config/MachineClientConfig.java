package com.colortinting.config;

import com.colortinting.machine.HttpEsp32MachineClient;
import com.colortinting.machine.SimulatedEsp32MachineClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class MachineClientConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(name = "app.machine.adapter", havingValue = "simulated")
    SimulatedEsp32MachineClient simulatedEsp32MachineClient(Clock clock) {
        return new SimulatedEsp32MachineClient(clock);
    }

    @Bean
    @ConditionalOnProperty(name = "app.machine.adapter", havingValue = "http", matchIfMissing = true)
    HttpEsp32MachineClient httpEsp32MachineClient(AppProperties properties, Clock clock) {
        return HttpEsp32MachineClient.create(properties.getMachine(), clock);
    }
}
