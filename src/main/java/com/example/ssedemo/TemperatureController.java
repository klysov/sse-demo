package com.example.ssedemo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@RestController
public class TemperatureController {
    private final Set<SseEmitter> clients = new CopyOnWriteArraySet<>();

    @GetMapping(
            value = "/temperature-stream"
    )
    public SseEmitter events(HttpServletRequest request) {
        var emitter = new SseEmitter();
        clients.add(emitter);
        emitter.onTimeout(() -> clients.remove(emitter));
        emitter.onCompletion(() -> clients.remove(emitter));
        return emitter;
    }

    @Async
    @EventListener
    public void handleMessage(Temperature temperature) {
        var deadEmitters = new ArrayList<>();
        clients.forEach(emitter -> {
                try {
                    emitter.send(temperature, MediaType.APPLICATION_JSON);
                } catch (Exception ignore) {
                    deadEmitters.add(emitter);
                }
            }
        );
        deadEmitters.forEach(clients::remove);
    }
}
