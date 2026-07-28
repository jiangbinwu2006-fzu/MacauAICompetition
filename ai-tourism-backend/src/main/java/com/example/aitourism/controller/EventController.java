package com.example.aitourism.controller;

import com.example.aitourism.dto.BaseResponse;
import com.example.aitourism.dto.event.EventModels;
import com.example.aitourism.service.EventService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public BaseResponse<List<EventModels.Response>> active() {
        return BaseResponse.success(eventService.listActive());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EventModels.Response>> stream() {
        Flux<ServerSentEvent<EventModels.Response>> events = eventService.stream()
                .map(event -> ServerSentEvent.builder(event).event("tourism-event").id(event.eventId() + "-" + event.version()).build());
        Flux<ServerSentEvent<EventModels.Response>> heartbeat = Flux.interval(Duration.ofSeconds(20))
                .map(index -> ServerSentEvent.<EventModels.Response>builder().comment("heartbeat").build());
        return Flux.merge(events, heartbeat);
    }
}
