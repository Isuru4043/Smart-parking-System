package com.example.smartParkingSystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.smartParkingSystem.Model.ParkingSlotMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import java.util.List;
import java.util.ArrayList;

@Service
public class ParkingService {

    private final ObjectMapper objectMapper;
    private final boolean[] parkingSlots = new boolean[12];
    private final ReentrantLock lock = new ReentrantLock();
    private final ConcurrentHashMap<Integer, ReentrantLock> slotLocks = new ConcurrentHashMap<>();

    public ParkingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        for (int i = 0; i < 12; i++) {
            slotLocks.put(i, new ReentrantLock());
        }
    }

    public List<ParkingSlotMessage> getSlots() {
        lock.lock();
        try {
            List<ParkingSlotMessage> slotMessages = new ArrayList<>();
            for (int i = 0; i < parkingSlots.length; i++) {
                ParkingSlotMessage message = new ParkingSlotMessage();
                message.setSlotIndex(i);
                message.setReserved(parkingSlots[i]);
                slotMessages.add(message);
            }
            return slotMessages;
        } finally {
            lock.unlock();
        }
    }


    public void updateSlotStatus(ParkingSlotMessage message, Collection<WebSocketSession> sessions) {
        ReentrantLock slotLock = slotLocks.get(message.getSlotIndex());
        slotLock.lock();
        try {
            parkingSlots[message.getSlotIndex()] = message.isReserved();
            broadcastUpdate(message, sessions);
        } finally {
            slotLock.unlock();
        }
    }

    public void sendCurrentState(WebSocketSession session) {
        lock.lock();
        try {
            for (int i = 0; i < parkingSlots.length; i++) {
                ParkingSlotMessage message = new ParkingSlotMessage();
                message.setSlotIndex(i);
                message.setReserved(parkingSlots[i]);
                sendMessage(session, message);
            }
        } finally {
            lock.unlock();
        }
    }

    private void broadcastUpdate(ParkingSlotMessage message, Collection<WebSocketSession> sessions) {
        sessions.parallelStream().forEach(session -> sendMessage(session, message));
    }

    private void sendMessage(WebSocketSession session, ParkingSlotMessage message) {
        try {
            if (session != null && session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                }
            }
        } catch (IOException e) {
            // Log error and remove dead session
            sessions.remove(session.getId());
        }
    }


}