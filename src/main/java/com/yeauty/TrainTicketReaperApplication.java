package com.yeauty;

import com.yeauty.service.ReaperService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

//开启定时任务
@EnableScheduling
@SpringBootApplication
public class TrainTicketReaperApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(TrainTicketReaperApplication.class, args);

		//----------- 丧心病狂模式 ----------
		/*ReaperService reaperService = context.getBean(ReaperService.class);
		while (true){
			reaperService.monitor();
		}*/

		//----------- 为了抢票不要命模式 ----------
		/*ReaperService reaperService = context.getBean(ReaperService.class);
		for (int i = 0 ; i < 100 ; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						reaperService.monitor();
					}
				}
			}).start();
		}*/
	}
}
