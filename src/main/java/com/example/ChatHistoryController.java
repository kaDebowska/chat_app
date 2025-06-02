package com.example;

import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@Controller("/chat")
public class ChatHistoryController {
    private final RedisChatService redisChatService;

    @Inject
    public ChatHistoryController(RedisChatService redisChatService){
        this.redisChatService = redisChatService;
    }

    @Get("/{roomId}")
    public List<Map<String, String>> getChatHistory(@PathVariable String roomId, @QueryValue(defaultValue = "100") int limit){
        return redisChatService.getChatHistory(roomId, limit);
    }

}
