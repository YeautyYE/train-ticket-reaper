package com.yeauty.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yeauty.service.LoginService;
import com.yeauty.service.OrderService;
import com.yeauty.service.ReaperService;
import com.yeauty.util.DingRobotUtils;
import com.yeauty.util.HttpClientUtils;
import com.yeauty.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Yeauty
 * @version 1.0
 * @Description:TODO
 * @date 2018/4/7 0:13
 */
@Service
public class ReaperServiceImpl implements ReaperService {

    private static final Logger logger = LoggerFactory.getLogger(ReaperServiceImpl.class);

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");

    @Value("${webhook-token}")
    String webhookToken;
    @Value("${train-search-url}")
    String trainSearchUrl;
    @Value("${seat-name}")
    String seatName;
    @Value("${username}")
    String username;
    @Value("${password}")
    String password;
    @Value("${passenger-name}")
    String passengerName;
    @Value("${passport-no}")
    String passportNo;
    @Value("${sex}")
    String sex;
    @Value("${contact-mobile}")
    String contactMobile;
    @Value("${contact-name}")
    String contactName;
    @Value("${time-range:null}")
    String timeRange;

    String trainInfoUrl = null;

    @Autowired
    LoginService loginService;
    @Autowired
    OrderService orderService;


    @Override
    public void monitor() {
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

        //从火车票查询页的url转换为trainInfo接口的url

        if (trainInfoUrl == null) {
            try {
                //火车票查询页url 为:http://www.12306.com/#/train/search/SZQ/SHH/2018-04-08/false
                String[] split = trainSearchUrl.trim().split("/train/search/")[1].split("/");
                //trainInfo接口的url 为:http://api.12306.com/v1/train/trainInfos?arrStationCode=SHH&deptDate=2018-04-08&deptStationCode=SZQ&findGD=false
                String arrStationCode = split[1];
                String deptDate = split[2];
                String deptStationCode = split[0];
                String findGD = split.length == 4 ? split[3] : "false";
                trainInfoUrl = "http://api.12306.com/v1/train/trainInfos?arrStationCode=" + arrStationCode + "&deptDate=" + deptDate + "&deptStationCode=" + deptStationCode + "&findGD=" + findGD;

            } catch (Exception e) {
                logger.error("火车票查询页的url转换trainInfo接口的url出错，请看填写url是否正确", e);
                return;
            }
        }

        if (StringUtils.isEmpty(trainInfoUrl)) {
            logger.error("火车票查询页的url转换trainInfo接口的url出错，请看填写url是否正确");
            return;
        }

        String json = HttpClientUtils.doGet(trainInfoUrl, null, headers);

        if (StringUtils.isEmpty(json)) {
            DingRobotUtils.send(webhookToken,"返回车次信息json数据为空，请看是否被封ip,url:"+trainInfoUrl,true);
            logger.error("返回车次信息json数据为空，请看是否被封ip,url:" + trainInfoUrl);
            return;
        }

        JsonNode jsonNode = JsonUtils.jsonToJsonNode(json);
        if (jsonNode == null) {
            DingRobotUtils.send(webhookToken, "解析车次信息json数据为空，请看返回json是否有误，或者IP是否被封, url:"+trainInfoUrl+" ,json:"+json, true);
            logger.error("解析车次信息json数据为空，请看返回json是否有误，或者IP是否被封, url:" + trainInfoUrl + " ,json:" + json);
            return;
        }

        JsonNode dataNode = jsonNode.get("data");
        if (dataNode == null) {
            DingRobotUtils.send(webhookToken, "解析车次信息json内的data数据为空，请看返回json是否有误，或者IP是否被封, url:"+trainInfoUrl+" ,json:"+json, true);
            logger.error("解析车次信息json内的data数据为空，请看返回json是否有误，或者IP是否被封, url:" + trainInfoUrl + " ,json:" + json);
            return;
        }

        JsonNode trainInfosNode = dataNode.get("trainInfos");
        if (trainInfosNode == null) {
            DingRobotUtils.send(webhookToken, "解析车次信息json内data下面的trainInfos数据为空，请看返回json是否有误，或者IP是否被封, url:"+trainInfoUrl+" ,json:"+json, true);
            logger.error("解析车次信息json内data下面的trainInfos数据为空，请看返回json是否有误，或者IP是否被封, url:" + trainInfoUrl + " ,json:" + json);
            return;
        }

        for (JsonNode infoNode : trainInfosNode) {
            JsonNode trainCodeNode = infoNode.get("trainCode");
            if (trainCodeNode == null) {
                DingRobotUtils.send(webhookToken, "解析车次trainCode为空，请注意,info:"+infoNode.toString(), true);
                logger.error("解析车次trainCode为空，请注意,info:" + infoNode.toString());
                continue;
            }
            JsonNode deptTimeNode = infoNode.get("deptTime");
            if (deptTimeNode == null) {
                DingRobotUtils.send(webhookToken, "解析车次deptTime为空，请注意,info:"+deptTimeNode.toString(), true);
                logger.error("解析车次deptTime为空，请注意,info:" + deptTimeNode.toString());
                continue;
            }

            //不是设定时间范围内的车次过滤
            if (!isTimeRange(deptTimeNode.asText(), timeRange)) {
                continue;
            }

            JsonNode seatList = infoNode.get("seatList");
            if (seatList == null) {
                DingRobotUtils.send(webhookToken, "解析车次:"+trainCodeNode.asText()+" 出发时间:"+deptTimeNode.asText()+" 的座位列表有误，info:"+deptTimeNode.toString(), true);
                logger.error("解析车次:" + trainCodeNode.asText() + " 出发时间:" + deptTimeNode.asText() + " 的座位列表有误，info:" + deptTimeNode.toString());
                continue;
            }

            JsonNode classSeatNode = null;
            JsonNode seatNameNode = null;
            for (JsonNode seatNode : seatList) {
                seatNameNode = seatNode.get("seatName");
                if (seatNameNode != null && seatName.equals(seatNameNode.asText().trim())) {
                    classSeatNode = seatNode;
                    break;
                }
            }

            if (classSeatNode == null) {
                logger.warn("车次:" + trainCodeNode.asText() + " 出发时间:" + deptTimeNode.asText() + "无对应的座位:" + seatName);
                continue;
            }
            JsonNode seatNum = classSeatNode.get("seatNum");
            if (seatNum == null) {
                logger.warn("车次:" + trainCodeNode.asText() + " 出发时间:" + deptTimeNode.asText() + "无对应的座位号码");
                continue;
            }
            if ("0".equals(seatNum.asText())) {
                continue;
            }

            Map<String, String> loginInfo = loginService.login(username, password);

            String accessToken = loginInfo.get("access_token");
            if (StringUtils.isEmpty(accessToken)) {
                DingRobotUtils.send(webhookToken, "登陆失败，请检查username和password", true);
                logger.error("登陆失败，请检查username和password");
                return;
            }

            String resultJson = orderService.buildOrder(accessToken, passengerName, passportNo, sex, contactMobile, contactName, infoNode);
            if (StringUtils.isEmpty(resultJson)) {
                DingRobotUtils.send(webhookToken, "构建订单失败，请检查passengerName, passportNo, sex, contactMobile, contactName。重点检查passengerName和passportNo是否对应", true);
                logger.warn("构建订单失败，请检查passengerName, passportNo, sex, contactMobile, contactName。重点检查passengerName和passportNo是否对应。或者是网络问题");
                break;
            }
            //{"message":"","code":"00000","data":{"orderNo":"T201804071229380152344","orderDate":1523075378841}}
            JsonNode resultNode = JsonUtils.jsonToJsonNode(resultJson);
            JsonNode codeNode = resultNode.get("code");

            if (codeNode.asText().equals("00005")) {
                logger.warn("存在未支付订单，请先去12306取消订单");
                System.exit(0);
            }

            if (codeNode.asText().equals("00000")) {
                JsonNode orderDataNode = resultNode.get("data");
                JsonNode orderNoNode = orderDataNode.get("orderNo");
                JsonNode orderDateNode = orderDataNode.get("orderDate");

                //http://api.12306.com/v1/train/order-detail/T201804071229380152344?access_token=82489e76-09fd-407c-bb26-31a660b00014
                String occupyUrl = "http://api.12306.com/v1/train/order-detail/" + orderNoNode.asText() + "?access_token=" + accessToken;
                while (true) {
                    String occupyJson = HttpClientUtils.doGet(occupyUrl, null, headers);
                    JsonNode occupyJsonNode = JsonUtils.jsonToJsonNode(occupyJson);
                    JsonNode occupyDataNode = occupyJsonNode.get("data");
                    JsonNode statusTextNode = occupyDataNode.get("statusText");
                    if (!"占座中".equals(statusTextNode.asText())) {
                        DingRobotUtils.send(webhookToken, deptTimeNode.asText() + "\r" + trainCodeNode.asText() + "\r 票数为:" + seatNum.asText() + " \r " + seatNameNode.asText() + " \r状态为:" + statusTextNode.asText(), true);
                        logger.info(deptTimeNode.asText() + " " + trainCodeNode.asText() + " 票数为:" + seatNum.asText() + "  " + seatNameNode.asText() + " 状态为:" + statusTextNode.asText());
                        if (statusTextNode.asText().contains("占座成功")) {
                            System.exit(0);
                            return;
                        }
                        break;
                    }
                }
            }


        }


    }

    private boolean isTimeRange(String deptTime, String timeRange) {
        //如果timeRange为空，则不限制时间
        if (StringUtils.isEmpty(timeRange) || "null".equals(timeRange)) {
            return true;
        }
        try {

            //当timeRange格式不对时，也认为不限制时间
            String[] split = timeRange.split("-");
            if (split.length != 2) {
                logger.warn("timeRange格式不对，不进行时间限制");
                return true;
            }
            Date deptTimeDate = simpleDateFormat.parse(deptTime);
            Date startDate = simpleDateFormat.parse(split[0]);
            Date endDate = simpleDateFormat.parse(split[1]);
            if (deptTimeDate.getTime() >= startDate.getTime() && deptTimeDate.getTime() <= endDate.getTime()) {
                return true;
            }
        } catch (ParseException e) {
            logger.warn("解析12306返回的车次时间或传入的timeRange出错", e);
            return true;
        }
        return false;
    }
}
