package com.yeauty.component;

import com.yeauty.service.ReaperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Yeauty
 * @version 1.0
 * @Description:定时器
 * @date 2017年5月27日下午3:57:43
 */
@Component
public class TimerComponent {

    private static final Logger logger = LoggerFactory.getLogger(TimerComponent.class);

    @Autowired
    ReaperService reaperService;

    /**
     * 每秒钟进行一次监控
     */
    @Scheduled(cron = "* * * * * ?")
    public void realtimeTuyereCollect() {
        logger.info("开始监控");
        reaperService.monitor();
        logger.info("监控结束");
    }
}
