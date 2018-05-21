package com.yeauty.component;

import com.yeauty.service.ReaperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TaskApplicationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TaskApplicationRunner.class);

    @Value("${mode}")
    String mode;

    @Autowired
    ReaperService reaperService;

    @Override
    public void run(ApplicationArguments var1) throws Exception {

        if (StringUtils.isEmpty(mode)) {
            mode = "1";
        }

        logger.info("您选择的模式为: " + mode);

        switch (mode) {
            case "1":
                ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
                threadPoolTaskScheduler.initialize();
                CronTrigger trigger = new CronTrigger("* * 6-23 * * ?");
                logger.info("使用 极速模式 开始抢票");
                threadPoolTaskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        reaperService.monitor();
                    }
                }, trigger);
                break;
            case "2":
                logger.info("使用 丧心病狂模式 开始抢票");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            reaperService.monitor();
                        }
                    }
                }).start();
                break;
            case "3":
                logger.info("使用 为了抢票不要命模式 开始抢票");
                for (int i = 0; i < 100; i++) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                reaperService.monitor();
                            }
                        }
                    }).start();
                }
                break;
            case "4":
                logger.info("使用 无脑下单模式(不进行是否有票的监控，直接对符合要求的车次进行下单) 开始抢票");
                reaperService.noBrainPlaceOrder();
                break;
            default:
                logger.warn("不存在此模式 :" + mode + "  将退出程序");
                System.exit(1);
                break;
        }

        logger.info("火车票收割者，正常启动");
    }


}