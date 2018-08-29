package com.yeauty.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yeauty.service.OrderService;
import com.yeauty.util.HttpClientUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Yeauty
 * @version 1.0
 * @Description:TODO
 * @date 2018/4/7 11:53
 */
@Service
@PropertySource(value = "classpath:train.properties", encoding = "UTF-8")
public class OrderServiceImpl implements OrderService {

    @Value("${seat-name}")
    String seatName;

    @Override
    public String buildOrder(String accessToken, String passengerName, String passportNo, String sex, String contactMobile, String contactName, JsonNode trainInfoNode) {
        String url = "http://api.12306.com/v1/train/order?access_token=" + accessToken;

        Map headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept-Language", "zh-CN");
        headers.put("Connection", "keep-alive");
        headers.put("Content-Type", "application/json");
        headers.put("Host", "api.12306.com");
        headers.put("Origin", "http://www.12306.com");
        headers.put("Referer", "http://www.12306.com/");
        headers.put("User-Agent", HttpClientUtils.pcUserAgentArray[new Random().nextInt(HttpClientUtils.pcUserAgentArray.length)]);

        //{"deptStationCode":"SNQ","arrStationCode":"IOQ","trainCode":"G6013","deptDate":"2018-04-10","seatPrice":"179.0","runTime":"01:39","deptTime":"09:17","passengers":[{"passengerMobile":null,"passengerName":"张三","passportTypeId":"1","passportNo":"440203198903291234","trainTicketType":"1","policyProductNo":"","passengerId":"","birthday":"","sex":"M","isPassengerSave":true,"insurancePrice":0}],"contactsInfo":{"contactEmail":"","contactMobile":"13726248277","contactName":"李四","contactPassportNo":"","contactPassportType":"1"},"usingTrainAccount":false,"trainZWCode":"O","source":"P2"}
        JsonNode trainCode = trainInfoNode.get("trainCode");

        JsonNode trainStatus = trainInfoNode.get("trainStatus");
        JsonNode trainType = trainInfoNode.get("trainType");
        JsonNode minPrice = trainInfoNode.get("minPrice");
        JsonNode maxPrice = trainInfoNode.get("maxPrice");
        JsonNode deptDate = trainInfoNode.get("deptDate");
        JsonNode arrDate = trainInfoNode.get("arrDate");
        JsonNode deptStationName = trainInfoNode.get("deptStationName");
        JsonNode deptStationCode = trainInfoNode.get("deptStationCode");
        JsonNode arrStationName = trainInfoNode.get("arrStationName");
        JsonNode arrStationCode = trainInfoNode.get("arrStationCode");
        JsonNode deptTime = trainInfoNode.get("deptTime");
        JsonNode arrTime = trainInfoNode.get("arrTime");
        JsonNode runTime = trainInfoNode.get("runTime");
        JsonNode arriveDays = trainInfoNode.get("arriveDays");
        JsonNode reason = trainInfoNode.get("reason");
        JsonNode startSaleTime = trainInfoNode.get("startSaleTime");
        JsonNode source = trainInfoNode.get("source");
        JsonNode ywXiaPrice = trainInfoNode.get("ywXiaPrice");
        JsonNode rwXiaPrice = trainInfoNode.get("rwXiaPrice");

        JsonNode seatList = trainInfoNode.get("seatList");
        JsonNode classSeatNode = null;
        JsonNode seatNameNode = null;
        for (JsonNode seatNode : seatList) {
            seatNameNode = seatNode.get("seatName");
            if (seatNameNode != null && seatName.equals(seatNameNode.asText().trim())) {
                classSeatNode = seatNode;
                break;
            }
        }

        JsonNode seatName = classSeatNode.get("seatName");
        JsonNode seatNum = classSeatNode.get("seatNum");
        JsonNode seatPrice = classSeatNode.get("seatPrice");
        JsonNode seatCode = classSeatNode.get("seatCode");
        JsonNode showButton = classSeatNode.get("showButton");

        String body = "{\"deptStationCode\":\"" + deptStationCode.asText() + "\",\"arrStationCode\":\"" + arrStationCode.asText() + "\",\"trainCode\":\"" + trainCode.asText() + "\",\"deptDate\":\"" + deptDate.asText() + "\",\"seatPrice\":\"" + seatPrice.asText() + "\",\"runTime\":\"" + runTime.asText() + "\",\"deptTime\":\"" + deptTime.asText() + "\",\"passengers\":[{\"passengerMobile\":null,\"passengerName\":\"" + passengerName + "\",\"passportTypeId\":\"1\",\"passportNo\":\"" + passportNo + "\",\"trainTicketType\":\"1\",\"policyProductNo\":\"\",\"passengerId\":\"\",\"birthday\":\"\",\"sex\":\"" + sex + "\",\"isPassengerSave\":true,\"insurancePrice\":0}],\"contactsInfo\":{\"contactEmail\":\"\",\"contactMobile\":\"" + contactMobile + "\",\"contactName\":\"" + contactName + "\",\"contactPassportNo\":\"\",\"contactPassportType\":\"1\"},\"usingTrainAccount\":false,\"trainZWCode\":\"" + seatCode.asText() + "\",\"source\":\"" + source.asText() + "\"}";

        String json = HttpClientUtils.doPostJson(url, body, headers);

        return json;
    }

}
