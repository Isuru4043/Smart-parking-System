package com.example.smartParkingSystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.smartParkingSystem.Model.ParkingSlotMessage;
import com.example.smartParkingSystem.service.ParkingService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ParkingWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ParkingService parkingService;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ParkingWebSocketHandler(ObjectMapper objectMapper, ParkingService parkingService) {
        this.objectMapper = objectMapper;
        this.parkingService = parkingService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        parkingService.sendCurrentState(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ParkingSlotMessage slotMessage = objectMapper.readValue(message.getPayload(), ParkingSlotMessage.class);
        parkingService.updateSlotStatus(slotMessage, sessions.values());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }
}