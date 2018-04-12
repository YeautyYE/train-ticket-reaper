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
                threadPoolTaskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        logger.info("使用 极速模式 开始检测");
                        reaperService.monitor();
                        logger.info("使用 极速模式 检测结束");
                    }
                }, trigger);
                break;
            case "2":
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            logger.info("使用 丧心病狂模式 开始检测");
                            reaperService.monitor();
                            logger.info("使用 丧心病狂模式 检测结束");
                        }
                    }
                }).start();
                break;
            case "3":
                for (int i = 0; i < 100; i++) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                logger.info("使用 为了抢票不要命模式 开始检测");
                                reaperService.monitor();
                                logger.info("使用 为了抢票不要命模式 检测结束");
                            }
                        }
                    }).start();
                }
                break;
            default:
                logger.warn("不存在此模式 :" + mode + "  将退出程序");
                System.exit(1);
                break;
        }

        logger.info("火车票收割者，正常启动");
    }


}