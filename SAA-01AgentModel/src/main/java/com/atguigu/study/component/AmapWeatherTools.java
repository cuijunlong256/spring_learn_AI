package com.atguigu.study.component;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AmapWeatherTools {

    private final RestTemplate restTemplate = new RestTemplate();
    // 建议将 apiKey 放入配置文件中，此处为演示写死
    private final String apiKey = "cc9053c06e4d3b8687269e43f067abaa";

    @Tool(name = "amap_weather_tool", description = "获取指定城市的实时天气信息，包括温度、天气状况、风向等。")
    public String getWeather(
            @ToolParam(description = "需要查询天气的城市名称，例如：北京、杭州") String city) {
        try {
            String url = String.format(
                "https://restapi.amap.com/v3/weather/weatherInfo?city=%s&key=%s&extensions=base", 
                city, apiKey
            );
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            return "调用高德天气API失败：" + e.getMessage();
        }
    }
}