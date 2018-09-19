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

    public static void send(String webhookToken, String content, boolean isAtAll) {
        if (StringUtils.isEmpty(webhookToken)) {
            return;
        }
        try {
            Map<String, String> heads = new HashMap<>();
            heads.put("Content-Type", "application/json; charset=utf-8");
            DingRobotVO dingRobotVO = new DingRobotVO("text", content, isAtAll);
            String result = HttpClientUtils.doPostJson(webhookToken, JsonUtils.objectToJson(dingRobotVO), heads);
            String errcode = JsonUtils.jsonToJsonNode(result).get("errcode").asText();
            if ("0".equals(errcode)) {
                logger.info("消息发送到钉钉机器人成功，内容：" + content);
            } else {
                logger.error("消息发送到钉钉机器人失败,结果:" + result);
            }
        } catch (Exception e) {
            logger.error("消息发送到钉钉机器人失败", e);
        }
    }
}

class DingRobotVO {
    private String msgtype;
    private Map<String, String> text;
    private Map<String, String> at;

    public DingRobotVO(String msgtype, String content, boolean isAtAll) {
        this.msgtype = msgtype;
        this.text = new HashMap<String, String>() {{
            put("content", content);
        }};
        this.at = new HashMap<String, String>() {{
            put("isAtAll", isAtAll + "");
        }};
    }

    public String getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }

    public Map<String, String> getText() {
        return text;
    }

    public void setText(Map<String, String> text) {
        this.text = text;
    }

    public Map<String, String> getAt() {
        return at;
    }

    public void setAt(Map<String, String> at) {
        this.at = at;
    }
}
