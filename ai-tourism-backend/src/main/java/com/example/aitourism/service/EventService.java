package com.example.aitourism.service;

import com.example.aitourism.dto.event.EventModels;
import reactor.core.publisher.Flux;

import java.util.List;

public interface EventService {
    EventModels.Response create(EventModels.UpsertRequest request);
    EventModels.Response update(String eventId, EventModels.UpsertRequest request);
    EventModels.Response changeStatus(String eventId, String status);
    List<EventModels.Response> listAll();
    List<EventModels.Response> listActive();
    Flux<EventModels.Response> stream();
    void reset();
}
