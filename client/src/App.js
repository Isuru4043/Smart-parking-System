import React, { useState, useEffect } from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import carImage from './assets/car1.png';

const SOCKET_URL = "ws://localhost:8080/websocket-endpoint";

function App() {
  const [slots, setSlots] = useState(Array(12).fill(false));
  const [ws, setWs] = useState(null);
  const [connectionStatus, setConnectionStatus] = useState("Connecting...");

  useEffect(() => {
    const connectWebSocket = () => {
      const socket = new WebSocket(SOCKET_URL);

      socket.onopen = () => {
        console.log("Connected to WebSocket");
        setConnectionStatus("Connected");
        setWs(socket);
      };

      socket.onmessage = (message) => {
        try {
          const data = JSON.parse(message.data);
          setSlots((prevSlots) => {
            const updatedSlots = [...prevSlots];
            updatedSlots[data.slotIndex] = data.reserved;
            return updatedSlots;
          });
        } catch (error) {
          console.error("Error parsing message:", error);
        }
      };

      socket.onclose = () => {
        console.log("Connection closed, attempting to reconnect...");
        setConnectionStatus("Reconnecting...");
        setTimeout(connectWebSocket, 3000);
      };

      socket.onerror = (error) => {
        console.error("WebSocket error:", error);
        setConnectionStatus("Connection Error");
      };
    };

    connectWebSocket();

    return () => {
      if (ws) {
        ws.close();
      }
    };
  }, []);

  const handleSlotClick = (index) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      const newStatus = !slots[index];
      const message = JSON.stringify({
        slotIndex: index,
        reserved: newStatus,
      });

      ws.send(message);
    } else {
      console.log("WebSocket is not connected");
    }
  };

  return (
    <div className="container-fluid p-3">
      <h1 className="text-center mb-4 boxy-text">
        Smart Parking System
      </h1>
      <div className="text-center mb-3">
        <span
          className={`badge ${
            connectionStatus === "Connected" ? "bg-success" : "bg-warning"
          }`}
          style={{ fontFamily: "Arial, sans-serif" }}
        >
          {connectionStatus}
        </span>
      </div>
      <div className="container">
        <div className="row">
          {slots.map((reserved, index) => (
            <div
              key={index}
              onClick={() => handleSlotClick(index)}
              className="col-12 col-sm-6 col-md-4 col-lg-3 d-flex justify-content-center align-items-center text-white fw-bold mb-1" // Responsive column classes
              style={{
                backgroundColor: reserved ? "#f7c4c9" : "#f9d327", // Gray color for parking space
                height: "150px",
                cursor: "pointer",
                borderRadius: "8px",
                transition: "all 0.3s ease",
                transform: `scale(${reserved ? "0.90" : "0.95"})`,
                position: "relative",
                display: "flex",
                flexDirection: "column",
                justifyContent: "center",
                alignItems: "center",
                border: "2px solid #343a40", // Darker gray border
                fontFamily: "Arial, sans-serif"
              }}
            >
              <div>Slot {index + 1}</div>
              {reserved && (
                <img
                  src={carImage}
                  alt="Car"
                  style={{
                    width: "200px", // Scaled up width
                    height: "100px", // Scaled up height
                  }}
                />
              )}
              <div className="small mb-1">
                {reserved ? "Reserved" : "Available"}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default App;
