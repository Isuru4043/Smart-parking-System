import React, { useState, useEffect } from "react";
import "bootstrap/dist/css/bootstrap.min.css";

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
      <h1 className="text-center mb-4">Smart Parking System</h1>
      <div className="text-center mb-3">
        <span
          className={`badge ${
            connectionStatus === "Connected" ? "bg-success" : "bg-warning"
          }`}
        >
          {connectionStatus}
        </span>
      </div>
      <div className="container">
        <div className="row">
          {slots.map((reserved, index) => (
            <div key={index} className="col-6 col-md-3 mb-4">
              <div
                onClick={() => handleSlotClick(index)}
                className="d-flex justify-content-center align-items-center text-white fw-bold"
                style={{
                  backgroundColor: reserved ? "#dc3545" : "#198754",
                  height: "150px",
                  cursor: "pointer",
                  borderRadius: "8px",
                  transition: "all 0.3s ease",
                  transform: `scale(${reserved ? "0.95" : "1"})`,
                }}
              >
                <div>
                  <div>Slot {index + 1}</div>
                  <div className="small mt-2">
                    {reserved ? "Reserved" : "Available"}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default App;
