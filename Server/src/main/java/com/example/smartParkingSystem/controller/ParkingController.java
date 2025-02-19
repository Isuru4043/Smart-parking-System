package com.example.smartParkingSystem.controller;

import com.example.smartParkingSystem.service.ParkingService;
import com.example.smartParkingSystem.Model.ParkingSlotMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class ParkingController {

    @Autowired
    private ParkingService parkingService;

    @GetMapping("/slots")
    public List<ParkingSlotMessage> getParkingSlots() {
        return parkingService.getSlots();
    }
}