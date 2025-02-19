package com.example.smartParkingSystem.Model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ParkingSlotMessage {
    private int slotIndex;
    private boolean reserved;
}