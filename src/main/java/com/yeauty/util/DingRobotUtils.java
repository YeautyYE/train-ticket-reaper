package com.yeauty.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yeauty
 * @version 1.0
 * @Description:TODO
 * @date 2018/3/22 10:16
 */
public abstract class DingRobotUtils {

    private static final Logger logger = LoggerFactory.getLogger(DingRobotUtils.class);

    public static void send(String webhookToken, String content,boolean isAtAll) {
        if(StringUtils.isEmpty(webhookToken)){
            return;
        }
        try {
            Map<String, String> heads = new HashMap<>();
            heads.put("Content-Type", "application/json; charset=utf-8");
            String param = "{ \"msgtype\": \"text\", \"text\": {\"content\": \"" + content + " \"} , \"at\": {\"isAtAll\": "+isAtAll+" }}";
            String result = HttpClientUtils.doPostJson(webhookToken, param, heads);
            String errcode = JsonUtils.jsonToJsonNode(result).get("errcode").asText();
            if ("0".equals(errcode)) {
                logger.info("消息发送到钉钉机器人成功，内容：" + content);
            }else {
                logger.error("消息发送到钉钉机器人失败,结果:"+result);
            }
        } catch (Exception e) {
            logger.error("消息发送到钉钉机器人失败", e);
        }
    }
}
