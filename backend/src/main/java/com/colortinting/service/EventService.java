package com.colortinting.service;

import com.colortinting.domain.ModelEnums.EventSeverity;
import com.colortinting.persistence.MachineEventEntity;
import com.colortinting.persistence.MachineEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class EventService {
    private final MachineEventRepository events;
    private final Clock clock;

    public EventService(MachineEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void record(
            EventSeverity severity,
            String code,
            String message,
            UUID batchId,
            UUID commandId,
            boolean simulated
    ) {
        MachineEventEntity event = new MachineEventEntity();
        event.setId(UUID.randomUUID());
        event.setOccurredAt(clock.instant());
        event.setSeverity(severity);
        event.setCode(code);
        event.setMessage(trim(message));
        event.setBatchId(batchId);
        event.setCommandId(commandId);
        event.setSimulated(simulated);
        events.save(event);
    }

    private static String trim(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
