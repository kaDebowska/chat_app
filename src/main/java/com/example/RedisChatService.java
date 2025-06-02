package com.example;

import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class RedisChatService {
    private final StatefulRedisConnection<String, String> connection;

    @Inject
    public RedisChatService(StatefulRedisConnection<String, String> connection){
        this.connection = connection;
    }

    public void saveMessage(String message, String roomId, String username){
        RedisCommands<String, String> redis = connection.sync();
        String streamKey = "chat:room:" + roomId;
        redis.xadd(streamKey, Map.of(
                "user", username,
                "message", message,
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    public List<Map<String, String>> getChatHistory(String roomId, int count){
        RedisCommands<String, String> redis = connection.sync();
        String streamKey = "chat:room:" + roomId;
        List<StreamMessage<String, String>> messages = redis.xrevrange(
                streamKey,
                Range.create("-", "+"),
                Limit.from(count)
        );
        Collections.reverse(messages);
        return messages.stream()
                .map(StreamMessage::getBody)
                .collect(Collectors.toList());
    }

}
