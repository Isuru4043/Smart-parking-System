// src/App.js
import React, { useState, useEffect } from "react";
import "bootstrap/dist/css/bootstrap.min.css";

const SOCKET_URL = "ws://localhost:8080/websocket-endpoint"; // Replace with your backend WebSocket URL

function App() {
  // Initialize 12 parking slots: false means available (green), true means reserved (red)
  const [slots, setSlots] = useState(Array(12).fill(false));
  const [ws, setWs] = useState(null);

  useEffect(() => {
    // Establish WebSocket connection
    const socket = new WebSocket(SOCKET_URL);
    setWs(socket);

    socket.onopen = () => {
      console.log("Connected to WebSocket");
    };

    socket.onmessage = (message) => {
      // Expected data format: { slotIndex: number, reserved: boolean }
      const data = JSON.parse(message.data);
      setSlots((prevSlots) => {
        const updatedSlots = [...prevSlots];
        updatedSlots[data.slotIndex] = data.reserved;
        return updatedSlots;
      });
    };

    socket.onclose = () => {
      console.log("WebSocket connection closed");
    };

    socket.onerror = (error) => {
      console.error("WebSocket error:", error);
    };

    // Clean up the WebSocket connection when the component unmounts
    return () => {
      socket.close();
    };
  }, []);

  // Toggle slot status and notify backend
  const handleSlotClick = (index) => {
    const newStatus = !slots[index];
    setSlots((prevSlots) => {
      const updatedSlots = [...prevSlots];
      updatedSlots[index] = newStatus;
      return updatedSlots;
    });

    // Send update via WebSocket if open
    if (ws && ws.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({ slotIndex: index, reserved: newStatus });
      ws.send(message);
    }
  };

  return (
    <div className="container-fluid p-3">
      <h1 className="text-center mb-4">Smart Parking System</h1>
      <div className="container">
        <div className="row">
          {slots.map((reserved, index) => (
            <div key={index} className="col-6 col-md-3 mb-4">
              <div
                onClick={() => handleSlotClick(index)}
                className="d-flex justify-content-center align-items-center text-white fw-bold"
                style={{
                  backgroundColor: reserved ? "red" : "green",
                  height: "150px", // Adjust height to make the squares larger if needed
                  cursor: "pointer",
                  borderRadius: "8px",
                }}
              >
                Slot {index + 1}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default App;
