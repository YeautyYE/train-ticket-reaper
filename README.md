![](https://img.shields.io/badge/train_ticket-reaper-lightgrey.svg?colorA=d9d0c7&colorB=9fe0f6)

### 吐槽
---
>在正式开始文档前，一定要深深的吐槽一波！！！
>想当年，我也是一个天真的抢票族，常年用去哪儿网的抢票。
>平常抢个票感觉还不错，但一到节假日！他就开始！掉！链！子！！！
>前端页面写着，95%成功率抢票成功，我天真的选了好几个班次。结果发现！我被前端的95%骗了！（虽然我也知道那就是随便写上去的数字）
>然后我的心情就*&……@*&#！*@……（）(*@&$(*）！（@&%（）&*！（￥——*
>实在没办法，只能自己手动撸一段抢票代码，哎 （手动滑稽）

>吐槽归吐槽，去哪儿网毕竟是免费给大家用的，也不可能太快嘛~ 你懂的~ 所以还是要谢谢去哪儿网那些年帮我抢过的票票~

---

## 项目介绍
![](https://img.shields.io/badge/build-passing-brightgreen.svg) ![](https://img.shields.io/badge/downloads-190KB-brightgreen.svg) ![](https://img.shields.io/badge/jdk-1.8-blue.svg) ![](https://img.shields.io/badge/springboot-2.0.1-blue.svg)   ![](https://img.shields.io/badge/maven-3.3.9-blue.svg)  ![](https://img.shields.io/badge/IDEA-2017.2.3-blue.svg)
- `train-ticket-reaper` 的目标是以简单的使用方法、和高效的抢票让小伙伴们安心的抢到回家的票
- 抢票的原理是模拟http请求调用12306API进行查询、下单、确认等（没有付款步奏，保证小伙伴钱包的安全，抢到后自行登陆12306付款即可）。
- 没有复杂的环境配置和依赖，只需要把该填的参数填上，run一下就可以一直放着等他抢到票为止
- 提供3种不同的抢票模式（极速模式、丧心病狂模式、为了抢票不要命模式），后两种非万不得已，不建议使用

## 快速启动

1. 导入项目并在配置文件中填入信息
![](https://i.imgur.com/grYq6ek.png)
2. 启动项目
![](https://i.imgur.com/qQqTDSd.png)
3. 佛性的等待，并在抢票成功后，登陆12306进行付款

## 通知方式

1.钉钉自定义机器人（如果使用阿里巴巴的钉钉）
- 在application.properties中的webhook-token填上钉钉机器人的webhook（一串url）即可
- 不填写则默认不使用钉钉机器人进行通知
- 钉钉机器人开发文档 ：https://open-doc.dingtalk.com/docs/doc.htm?spm=a219a.7629140.0.0.mQpC3N&treeId=257&articleId=105735&docType=1

2.佛性通知
- 当抢票成功后，程序会自动退出（不用担心资源浪费等问题）。
- 当下单成功的15分钟后，12306会主动发信息到你填的 联系人号码 上（因为12306想催你给钱啊~）
- 所以不用担心抢票成功后又被超时取消

## 抢票模式
1.极速模式 （只需按照快速启动，即时极速模式。默认每秒刷一次）
2.丧心病狂模式
- 将com.yeauty.component.TimerComponent中的@Scheduled(cron = "* * * * * ?")注掉
![](https://i.imgur.com/LuDJA4Y.png)
- 将com.yeauty.TrainTicketReaperApplication中丧心病狂模式代码解开（默认是注掉）
![](https://i.imgur.com/yLVpAdO.png)
3.为了抢票不要命模式
- 将com.yeauty.component.TimerComponent中的@Scheduled(cron = "* * * * * ?")注掉
![](https://i.imgur.com/LuDJA4Y.png)
- 将com.yeauty.TrainTicketReaperApplication中为了抢票不要命模式代码解开（默认是注掉）
![](https://i.imgur.com/7kO1DHh.png)