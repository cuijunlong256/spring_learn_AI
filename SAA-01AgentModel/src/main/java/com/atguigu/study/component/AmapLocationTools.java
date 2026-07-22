package com.atguigu.study.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AmapLocationTools {

    private final String amapApiKey = "cc9053c06e4d3b8687269e43f067abaa";

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(description = "获取指定地点的经纬度、所属省市及详细地址信息。")
    public String getLocationDetails(
            @ToolParam(description = "需要查询的目标地点名称，如'张家界'") String locationName) {
        String url = String.format(
                "https://restapi.amap.com/v3/geocode/geo?address=%s&key=%s",
                locationName, amapApiKey
        );
        return restTemplate.getForObject(url, String.class);
    }

    @Tool(description = "计算从默认起点（北京）到目标地点的路线、距离及耗时。")
    public String calculateRoute(
            @ToolParam(description = "目的地名称") String destination) {
        try {
            String originCoords = geocode("北京");
            String destCoords = geocode(destination);

            if (originCoords == null || destCoords == null) {
                return "无法获取地点坐标，请检查地点名称是否正确。";
            }

            String url = String.format(
                    "https://restapi.amap.com/v3/direction/driving?origin=%s&destination=%s&key=%s&extensions=base",
                    originCoords, destCoords, amapApiKey
            );
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            return "路线规划请求失败: " + e.getMessage();
        }
    }

    private String geocode(String address) {
        String url = String.format(
                "https://restapi.amap.com/v3/geocode/geo?address=%s&key=%s",
                address, amapApiKey
        );
        String response = restTemplate.getForObject(url, String.class);
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode geocodes = root.path("geocodes");
            if (geocodes.isArray() && geocodes.size() > 0) {
                return geocodes.get(0).path("location").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
