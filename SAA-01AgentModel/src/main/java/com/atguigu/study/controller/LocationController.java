package com.atguigu.study.controller;

import com.atguigu.study.service.LocationAgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class LocationController {

    private final LocationAgentService locationAgentService;

    public LocationController(LocationAgentService locationAgentService) {
        this.locationAgentService = locationAgentService;
    }

    @GetMapping("/location")
    public String getLocationInfo(@RequestParam("message") String message) {
        return locationAgentService.chat(message);
    }
}