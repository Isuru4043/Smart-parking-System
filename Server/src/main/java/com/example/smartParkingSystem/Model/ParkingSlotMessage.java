package com.example.smartParkingSystem.Model;

import lombok.Data;

@Data
public class ParkingSlotMessage {
    private int slotIndex;
    private boolean reserved;

    public void setSlotIndex(int index) {
        this.slotIndex = index;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public boolean isReserved() {
        return reserved;
    }
}
