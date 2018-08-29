package com.yeauty.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yeauty.service.LoginService;
import com.yeauty.service.OrderService;
import com.yeauty.service.ReaperService;
import com.yeauty.service.StationService;
import com.yeauty.util.DingRobotUtils;
import com.yeauty.util.HttpClientUtils;
import com.yeauty.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yeauty
 * @version 1.0
 * @Description:TODO
 * @date 2018/4/7 0:13
 */
@PropertySource(value = "classpath:train.properties", encoding = "UTF-8")
@Service
public class ReaperServiceImpl implements ReaperService {

    private static final Logger logger = LoggerFactory.getLogger(ReaperServiceImpl.class);

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H:mm");

    @Value("${webhook-token}")
    String webhookToken;
    @Value("${from-station}")
    String fromStation;
    @Value("${to-station}")
    String toStation;
    @Value("${just-gd}")
    String justGD;
    @Value("${dept-date}")
    String deptDate;
    @Value("${seat-name}")
    String seatName;
    @Value("${account}")
    String account;
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

    String trainInfoUrl;

    AtomicInteger counter = new AtomicInteger(1);

    @Autowired
    LoginService loginService;
    @Autowired
    OrderService orderService;
    @Autowired
    StationService stationService;


    @Override
    public void monitor() {
        try {
            logger.info("开始进行第 " + counter.getAndIncrement() + " 次检测");

            Map<String, String> headers = new HashMap<>();
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

            //获取列车信息
            JsonNode trainInfosNode = getTrainInfosNode(fromStation, toStation, deptDate, justGD, headers, webhookToken);
            if (trainInfosNode == null) {
                logger.error("获取列车信息失败,请查看参数 from-station 、 to-station 、 dept-date 是否有误。或 www.12306.com是否封了IP");
                DingRobotUtils.send(webhookToken, "获取列车信息失败,请查看参数 from-station 、 to-station 、 dept-date 是否有误。或 www.12306.com是否封了IP", true);
                return;
            }

            //获取适合的列车信息（符合配置文件中配置的）
            List<JsonNode> trainNodes = getApplicableTrainInfoNodes(trainInfosNode, seatName, timeRange, webhookToken);
            if (trainNodes == null) {
                logger.error("没有符合要求的班次,请检查 " + timeRange + " 是否有符合要求的列车信息。目标座位为:" + seatName);
                DingRobotUtils.send(webhookToken, "没有符合要求的班次,请检查 " + timeRange + " 是否有符合要求的列车信息。目标座位为:" + seatName, true);
                return;
            }

            for (JsonNode infoNode : trainNodes) {

                if (!isSellTime(infoNode)) {
                    continue;
                }

                JsonNode classSeatNode = getClassSeatNode(infoNode, seatName);
                JsonNode seatNum = classSeatNode.get("seatNum");

                if ("0".equals(seatNum.asText())) {
                    continue;
                }

                //通过登陆获取accessToken
                String accessToken = getAccessTokenByLogin(account, password, webhookToken);
                if (StringUtils.isEmpty(accessToken)) {
                    DingRobotUtils.send(webhookToken, "登陆失败，请检查username和password", true);
                    logger.error("登陆失败，请检查username和password");
                    continue;
                }

                //构建订单
                JsonNode orderNode = buildOrder(accessToken, passengerName, passportNo, sex, contactMobile, contactName, infoNode);
                if (orderNode == null) {
                    continue;
                }

                //占座(会一直刷到出结果，成功则程序退出，失败则return,进行下一次)
                occupy(accessToken, orderNode, infoNode, seatName, headers, webhookToken);

            }
        } catch (Exception e) {
            logger.error("第 " + counter.get() + " 次检测报错", e);
        }
    }

    private boolean isSellTime(JsonNode infoNode) {
        JsonNode reasonNode = infoNode.get("reason");
        if (reasonNode != null && "不在服务时间".equals(reasonNode.asText())) {
            //当23:00到6:00时是不能买票的，缓一缓
            JsonNode trainCodeNode = infoNode.get("trainCode");
            logger.info(trainCodeNode.asText() + "\t 不在服务期间内 （23:00到6:00时是不能买票的），sleep 100ms  ，跳过");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    @Override
    public void noBrainPlaceOrder() {
        try {
            Map<String, String> headers = new HashMap<>();
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

            //获取列车信息
            JsonNode trainInfosNode = getTrainInfosNode(fromStation, toStation, deptDate, justGD, headers, webhookToken);
            if (trainInfosNode == null) {
                logger.error("获取列车信息失败");
                return;
            }

            //获取适合的列车信息（符合配置文件中配置的）
            List<JsonNode> trainNodes = getApplicableTrainInfoNodes(trainInfosNode, seatName, timeRange, webhookToken);
            if (trainNodes == null) {
                logger.error("没有符合要求的班次,请检查 " + timeRange + " 是否有符合要求的列车信息");
                DingRobotUtils.send(webhookToken, "没有符合要求的班次,请检查 " + timeRange + " 是否有符合要求的列车信息", true);
                return;
            }

            while (true) {
                for (JsonNode infoNode : trainNodes) {

                    if (!isSellTime(infoNode)) {
                        continue;
                    }

                    JsonNode classSeatNode = getClassSeatNode(infoNode, seatName);
                    JsonNode seatNum = classSeatNode.get("seatNum");

                    //通过登陆获取accessToken
                    String accessToken = getAccessTokenByLogin(account, password, webhookToken);
                    if (StringUtils.isEmpty(accessToken)) {
                        DingRobotUtils.send(webhookToken, "登陆失败，请检查username和password", true);
                        logger.error("登陆失败，请检查username和password");
                        continue;
                    }

                    //构建订单
                    JsonNode orderNode = buildOrder(accessToken, passengerName, passportNo, sex, contactMobile, contactName, infoNode);
                    if (orderNode == null) {
                        continue;
                    }
                    //占座(会一直刷到出结果，成功则程序退出，失败则return,进行下一次)
                    occupy(accessToken, orderNode, infoNode, seatName, headers, webhookToken);


                }
            }
        } catch (Exception e) {
            logger.error("无脑下单模式报错", e);
        }

    }

    private JsonNode buildOrder(String accessToken, String passengerName, String passportNo, String sex, String contactMobile, String contactName, JsonNode infoNode) {
        String orderJson = orderService.buildOrder(accessToken, passengerName, passportNo, sex, contactMobile, contactName, infoNode);
        if (StringUtils.isEmpty(orderJson)) {
            DingRobotUtils.send(webhookToken, "构建订单失败，请检查passengerName, passportNo, sex, contactMobile, contactName。重点检查passengerName和passportNo是否对应", true);
            logger.error("构建订单失败，请检查passengerName, passportNo, sex, contactMobile, contactName。重点检查passengerName和passportNo是否对应。或者是网络问题");
            return null;
        }
        JsonNode orderNode = JsonUtils.jsonToJsonNode(orderJson);
        JsonNode codeNode = orderNode.get("code");

        //{"message":"","code":"00000","data":{"orderNo":"T201804071229380152344","orderDate":1523075378841}}

        if (codeNode.asText().equals("00007")) {
            logger.warn("存在正在占座的订单 , sleep 100 ms , continue");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        if (codeNode.asText().equals("00005")) {
            logger.warn("存在未支付订单，请先去 www.12306.com 取消订单");
            System.exit(0);
        }

        if (codeNode.asText().equals("00000")) {
            return orderNode;
        }
        return null;
    }

    private void occupy(String accessToken, JsonNode orderNode, JsonNode infoNode, String seatName, Map<String, String> headers, String webhookToken) {
        JsonNode trainCodeNode = infoNode.get("trainCode");
        JsonNode deptTimeNode = infoNode.get("deptTime");
        JsonNode classSeatNode = getClassSeatNode(infoNode, seatName);
        JsonNode seatNum = classSeatNode.get("seatNum");

        JsonNode orderDataNode = orderNode.get("data");
        JsonNode orderNoNode = orderDataNode.get("orderNo");
        JsonNode orderDateNode = orderDataNode.get("orderDate");

        //http://api.12306.com/v1/train/order-detail/T201804071229380152344?access_token=82489e76-09fd-407c-bb26-31a660b00014
        String occupyUrl = "http://api.12306.com/v1/train/order-detail/" + orderNoNode.asText() + "?access_token=" + accessToken;
        while (true) {
            String occupyJson = HttpClientUtils.doGet(occupyUrl, null, headers);
            if (StringUtils.isEmpty(occupyJson)) {
                DingRobotUtils.send(webhookToken, "占座时返回空，请检查是否被封，或是否超时", true);
                logger.warn("占座时返回空，请检查是否被封，或是否超时");
                continue;
            }
            JsonNode occupyJsonNode = JsonUtils.jsonToJsonNode(occupyJson);
            if (occupyJsonNode == null) {
                DingRobotUtils.send(webhookToken, "占座时返回数据不为json，内容:" + occupyJson, true);
                logger.warn("占座时返回数据不为json，内容:" + occupyJson);
                continue;
            }
            JsonNode occupyDataNode = occupyJsonNode.get("data");
            JsonNode statusTextNode = occupyDataNode.get("statusText");
            String deptStationName = infoNode.get("deptStationName") == null ? "" : infoNode.get("deptStationName").asText();
            String arrStationName = infoNode.get("arrStationName") == null ? "" : infoNode.get("arrStationName").asText();

            //如果不为占座中，则判断是什么状态  （占座中时，需要重复刷这个接口，直到拿到成功或失败的信息）
            if (!"占座中".equals(statusTextNode.asText())) {
                DingRobotUtils.send(webhookToken, "出发时间:" + deptDate + " " + deptTimeNode.asText() + "\r车次:" + trainCodeNode.asText() + " [" + deptStationName + " 开往 " + arrStationName + "]\r票数:" + seatNum.asText() + " [" + seatName + " ￥" + classSeatNode.get("seatPrice").asText() + "] \r状态:" + statusTextNode.asText(), false);
                logger.info("出发时间:" + deptDate + " " + deptTimeNode.asText());
                logger.info("车次:" + trainCodeNode.asText() + " [从 " + deptStationName + " 开往 " + arrStationName + "]");
                logger.info("票数:" + seatNum.asText() + " [" + seatName + " ￥" + classSeatNode.get("seatPrice").asText() + "]");
                logger.info("状态:" + statusTextNode.asText());
                if (statusTextNode.asText().contains("占座成功")) {
                    DingRobotUtils.send(webhookToken, "出发时间:" + deptDate + " " + deptTimeNode.asText() + "\r车次:" + trainCodeNode.asText() + " [" + deptStationName + " 开往 " + arrStationName + "]\r票数:" + seatNum.asText() + " [" + seatName + " ￥" + classSeatNode.get("seatPrice").asText() + "] \r状态:" + statusTextNode.asText() + "\r", true);
                    System.exit(0);
                    return;
                }
                //不是占座成功一般都是占座失败，跳出循环
                break;
            }

        }

    }

    private String getAccessTokenByLogin(String account, String password, String webhookToken) {
        Map<String, String> loginInfo = loginService.login(account, password);
        if (loginInfo == null) {
            return null;
        }
        String accessToken = loginInfo.get("access_token");
        if (StringUtils.isEmpty(accessToken)) {
            return null;
        }
        return accessToken;
    }

    private List<JsonNode> getApplicableTrainInfoNodes(JsonNode trainInfosNode, String seatName, String timeRange, String webhookToken) {
        List<JsonNode> trainInfoNodes = new ArrayList<>();
        for (JsonNode infoNode : trainInfosNode) {
            JsonNode trainCodeNode = infoNode.get("trainCode");
            if (trainCodeNode == null) {
                DingRobotUtils.send(webhookToken, "解析车次trainCode为空，请注意,info:" + infoNode.toString(), true);
                logger.error("解析车次trainCode为空，请注意,info:" + infoNode.toString());
                continue;
            }

            JsonNode deptTimeNode = infoNode.get("deptTime");
            if (deptTimeNode == null) {
                DingRobotUtils.send(webhookToken, "解析车次deptTime为空，请注意,info:" + deptTimeNode.toString(), true);
                logger.error("解析车次deptTime为空，请注意,info:" + deptTimeNode.toString());
                continue;
            }

            //不是设定时间范围内的车次过滤
            if (!isTimeRange(deptTimeNode.asText(), timeRange)) {
                continue;
            }

            JsonNode seatList = infoNode.get("seatList");
            if (seatList == null) {
                DingRobotUtils.send(webhookToken, "解析车次:" + trainCodeNode.asText() + " 出发时间:" + deptTimeNode.asText() + " 的座位列表有误，info:" + deptTimeNode.toString(), true);
                logger.error("解析车次:" + trainCodeNode.asText() + " 出发时间:" + deptTimeNode.asText() + " 的座位列表有误，info:" + deptTimeNode.toString());
                continue;
            }

            JsonNode classSeatNode = getClassSeatNode(infoNode, seatName);

            if (classSeatNode == null) {
                logger.warn("车次:" + trainCodeNode.asText() + " 出发时间:" + deptTimeNode.asText() + "无对应的座位:" + seatName);
                continue;
            }
            JsonNode seatNum = classSeatNode.get("seatNum");
            if (seatNum == null) {
                logger.warn("车次:" + trainCodeNode.asText() + " 出发时间:" + deptTimeNode.asText() + "无对应的座位号码");
                continue;
            }

            trainInfoNodes.add(infoNode);
        }
        if (trainInfoNodes.size() > 0) {
            return trainInfoNodes;
        }
        return null;
    }

    private JsonNode getClassSeatNode(JsonNode infoNode, String seatName) {
        JsonNode seatList = infoNode.get("seatList");
        if (seatList == null) {
            return null;
        }
        JsonNode seatNameNode;
        for (JsonNode seatNode : seatList) {
            seatNameNode = seatNode.get("seatName");
            if (seatNameNode != null && seatName.equals(seatNameNode.asText().trim())) {
                return seatNode;
            }
        }
        return null;
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

    public JsonNode getTrainInfosNode(String fromStation, String toStation, String deptDate, String justGD, Map<String, String> headers, String webhookToken) {
        //从火车票查询页的url转换为trainInfo接口的url

        if (trainInfoUrl == null) {
            String fromStationCode = stationService.findCodeByCityName(fromStation);
            String toStationCode = stationService.findCodeByCityName(toStation);
            try {
                //trainInfo接口的url 为:http://api.12306.com/v1/train/trainInfos?arrStationCode=SHH&deptDate=2018-04-08&deptStationCode=SZQ&findGD=false
                trainInfoUrl = "http://api.12306.com/v1/train/trainInfos?arrStationCode=" + toStationCode + "&deptDate=" + deptDate + "&deptStationCode=" + fromStationCode + "&findGD=" + justGD.toLowerCase();

            } catch (Exception e) {
                logger.error("火车票查询页的url转换trainInfo接口的url出错，请看填写url是否正确", e);
                return null;
            }
        }

        if (StringUtils.isEmpty(trainInfoUrl)) {
            logger.error("火车票查询页的url转换trainInfo接口的url出错，请看填写url是否正确");
            return null;
        }

        String json = HttpClientUtils.doGet(trainInfoUrl, null, headers);

        if (StringUtils.isEmpty(json)) {
            DingRobotUtils.send(webhookToken, "返回车次信息json数据为空，请看是否被封ip,url:" + trainInfoUrl, false);
            logger.error("返回车次信息json数据为空，请看是否被封ip,url:" + trainInfoUrl);
            return null;
        }

        JsonNode jsonNode = JsonUtils.jsonToJsonNode(json);
        if (jsonNode == null) {
            DingRobotUtils.send(webhookToken, "解析车次信息json数据为空，请看返回json是否有误，或者IP是否被封, url:" + trainInfoUrl + " ,json:" + json, true);
            logger.error("解析车次信息json数据为空，请看返回json是否有误，或者IP是否被封, url:" + trainInfoUrl + " ,json:" + json);
            return null;
        }

        JsonNode dataNode = jsonNode.get("data");
        if (dataNode == null) {
            DingRobotUtils.send(webhookToken, "解析车次信息json内的data数据为空，请看返回json是否有误，或者IP是否被封, url:" + trainInfoUrl + " ,json:" + json, true);
            logger.error("解析车次信息json内的data数据为空，请看返回json是否有误，或者IP是否被封, url:" + trainInfoUrl + " ,json:" + json);
            return null;
        }

        JsonNode trainInfosNode = dataNode.get("trainInfos");
        if (trainInfosNode == null) {
            DingRobotUtils.send(webhookToken, "解析车次信息json内data下面的trainInfos数据为空，请看返回json是否有误，或者IP是否被封, url:" + trainInfoUrl + " ,json:" + json, true);
            logger.error("解析车次信息json内data下面的trainInfos数据为空，请看返回json是否有误，或者IP是否被封, url:" + trainInfoUrl + " ,json:" + json);
            return null;
        }
        return trainInfosNode;
    }
}


