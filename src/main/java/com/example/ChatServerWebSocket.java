package com.example;

import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ServerWebSocket("/ws/chat/{room_id}")
public class ChatServerWebSocket {
    private static final Logger LOG = LoggerFactory.getLogger(ChatServerWebSocket.class);
    private final WebSocketBroadcaster broadcaster;
    private final RedisChatService redisChatService;

    @Inject
    public ChatServerWebSocket(WebSocketBroadcaster broadcaster, RedisChatService redisChatService){
        this.broadcaster = broadcaster;
        this.redisChatService = redisChatService;
    }

    @OnOpen
    public Publisher<String> onOpen(String room_id, WebSocketSession session) {
        String username = session.getRequestParameters().get("username");
        if (username == null || username.isBlank()) {
            username = "anonymous";
        }

        log("onOpen", session, username, room_id);

        List<Map<String, String>> history = redisChatService.getChatHistory(room_id, 10);

        List<Publisher<String>> historyMessages = history.stream()
                .map(msg -> {
                    String formatted = String.format("[%s] %s", msg.get("user"), msg.get("message"));
                    return session.send(formatted);
                })
                .collect(Collectors.toList());

        String joinMsg = String.format("[%s] Joined room %s", username, room_id);
        Publisher<String> broadcastJoin = broadcaster.broadcast(joinMsg, isValid(room_id));
        historyMessages.add(broadcastJoin);

        return Flowable.concat(historyMessages);
    }

    @OnMessage
    public Publisher<String> onMessage(String room_id, String message, WebSocketSession session){
        String username = session.getRequestParameters().get("username");
        if (username == null || username.isBlank()){
            username = "anonymous";
        }
        log("onMessage", session, username, room_id);

        redisChatService.saveMessage(message, room_id, username);

        return broadcaster.broadcast(
                String.format("[%s] %s", username, message),
                isValid(room_id)
        );

    }

    @OnClose
    public Publisher<String> onClose(String room_id, WebSocketSession session){
        String username = session.getRequestParameters().get("username");
        if (username == null || username.isBlank()){
            username = "anonymous";
        }
        log("onClose", session, username, room_id);

        return broadcaster.broadcast(
                String.format("[%s] Leaving room %s", username, room_id),
                isValid(room_id)
        );

    }

    private void log(String event, WebSocketSession session, String username, String room_id){
        LOG.info("* Websocket: {} received for session {} from '{}' regarding {}",
                event, session.getId(), username, room_id);
    }

    private Predicate<WebSocketSession> isValid(String room_id){
        return s -> room_id.equalsIgnoreCase(
                s.getUriVariables().get("room_id", String.class, null));
    }
}
