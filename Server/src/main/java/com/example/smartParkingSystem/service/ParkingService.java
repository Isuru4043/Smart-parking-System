package com.example.smartParkingSystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.smartParkingSystem.Model.ParkingSlotMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ParkingService {
    private final ObjectMapper objectMapper;
    private final boolean[] parkingSlots = new boolean[12];
    private final ReentrantLock lock = new ReentrantLock();
    private final ConcurrentHashMap<Integer, ReentrantLock> slotLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final List<ParkingWorkerThread> workerThreads;
    private volatile boolean isRunning = true;

    public ParkingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.workerThreads = new ArrayList<>();

        // Initialize slot locks
        for (int i = 0; i < 12; i++) {
            slotLocks.put(i, new ReentrantLock());
        }

        // Create and start worker threads
        for (int i = 0; i < 4; i++) {
            ParkingWorkerThread worker = new ParkingWorkerThread("ParkingWorker-" + i);
            workerThreads.add(worker);
            worker.start();
        }
    }

    // Custom Worker Thread class
    private class ParkingWorkerThread extends Thread {
        public ParkingWorkerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(100); // Prevent CPU overuse
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
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
        Thread updateThread = new Thread(() -> {
            ReentrantLock slotLock = slotLocks.get(message.getSlotIndex());
            slotLock.lock();
            try {
                parkingSlots[message.getSlotIndex()] = message.isReserved();
                broadcastUpdate(message, sessions);
            } finally {
                slotLock.unlock();
            }
        }, "UpdateThread-" + message.getSlotIndex());

        updateThread.start();
        try {
            updateThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void broadcastUpdate(ParkingSlotMessage message, Collection<WebSocketSession> sessions) {
        List<Thread> broadcastThreads = new ArrayList<>();

        for (WebSocketSession session : sessions) {
            Thread broadcastThread = new Thread(() -> {
                sendMessage(session, message);
            }, "BroadcastThread-" + session.getId());

            broadcastThreads.add(broadcastThread);
            broadcastThread.start();
        }

        // Wait for all broadcast threads to complete
        for (Thread thread : broadcastThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void sendCurrentState(WebSocketSession session) {
        Thread stateThread = new Thread(() -> {
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
        }, "StateThread-" + session.getId());

        stateThread.start();
    }

    private void sendMessage(WebSocketSession session, ParkingSlotMessage message) {
        try {
            if (session != null && session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                }
            }
        } catch (IOException e) {
            sessions.remove(session.getId());
        }
    }

    // Cleanup method for proper thread shutdown
    public void shutdown() {
        isRunning = false;
        for (ParkingWorkerThread worker : workerThreads) {
            worker.interrupt();
        }
    }
}
