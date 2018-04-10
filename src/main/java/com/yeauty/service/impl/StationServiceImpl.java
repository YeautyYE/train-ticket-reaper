package com.yeauty.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yeauty.service.StationService;
import com.yeauty.util.DingRobotUtils;
import com.yeauty.util.HttpClientUtils;
import com.yeauty.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Yeauty
 * @version 1.0
 * @Description:TODO
 * @date 2018/4/10 15:59
 */
@Service
public class StationServiceImpl implements StationService {

    private static final Logger logger = LoggerFactory.getLogger(StationServiceImpl.class);

    Map<String, String> cityMap;

    @Value("${webhook-token}")
    String webhookToken;

    @Override
    public String findCodeByCityName(String cityName) {

        if (cityMap == null || cityMap.size() == 0) {

            cityMap = new HashMap<>();

            Map headers = new HashMap<>();
            headers.put("Accept", "application/json, text/plain");
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Accept-Language", "zh-CN");
            headers.put("Connection", "keep-alive");
            headers.put("Content-Type", "application/json");
            headers.put("DNT", "1");
            headers.put("Host", "api.12306.com");
            headers.put("Origin", "http://www.12306.com");
            headers.put("Referer", "http://www.12306.com/");
            headers.put("User-Agent", HttpClientUtils.pcUserAgentArray[new Random().nextInt(HttpClientUtils.pcUserAgentArray.length)]);

            String stationsUrl = "http://api.12306.com/v1/train/stations";

            String json = HttpClientUtils.doGet(stationsUrl, null, headers);

            if (StringUtils.isEmpty(json)) {
                DingRobotUtils.send(webhookToken, "返回城市信息json数据为空，请看是否被封ip,url:" + stationsUrl, true);
                logger.error("返回城市信息json数据为空，请看是否被封ip,url:" + stationsUrl);
                return null;
            }

            JsonNode jsonNode = JsonUtils.jsonToJsonNode(json);

            if (jsonNode == null) {
                DingRobotUtils.send(webhookToken, "解析城市信息json数据为空，请看返回json是否有误，或者IP是否被封, url:" + stationsUrl + " ,json:" + json, true);
                logger.error("解析城市信息json数据为空，请看返回json是否有误，或者IP是否被封, url:" + stationsUrl + " ,json:" + json);
                return null;
            }

            JsonNode dataNode = jsonNode.get("data");
            if (dataNode == null) {
                DingRobotUtils.send(webhookToken, "解析城市信息json内的data数据为空，请看返回json是否有误，或者IP是否被封, url:" + stationsUrl + " ,json:" + json, true);
                logger.error("解析城市信息json内的data数据为空，请看返回json是否有误，或者IP是否被封, url:" + stationsUrl + " ,json:" + json);
                return null;
            }

            for (JsonNode node : dataNode) {
                String name = node.get("cityName").asText();
                String cityCode = node.get("cityCode").asText();
                cityMap.put(name, cityCode);
            }
        }

        return cityMap.get(cityName);
    }
}
