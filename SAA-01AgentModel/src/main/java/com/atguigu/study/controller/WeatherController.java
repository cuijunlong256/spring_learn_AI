package com.atguigu.study.controller;

import com.atguigu.study.service.WeatherAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class WeatherController {

    @Autowired
    private WeatherAgentService weatherAgentService;

    @GetMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        // 直接将用户输入传给 Agent，Agent 会根据 Markdown 指令自动判断是否调用天气工具
        return weatherAgentService.chat(message);
    }
}