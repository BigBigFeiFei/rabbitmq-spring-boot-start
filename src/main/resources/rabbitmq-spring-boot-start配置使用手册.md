rabbitmq-spring-boot-start配置使用手册

[TOC]



# 1.yaml配置如下

rps中的每一个都可以按照Sping官方的RabbitAutoConfiguration自动装配的RabbitProperties的样式来配置，做到了实现配置多个rabbitMq服务器配置和一个rabbitMq服务器下可以配置多个不同类型的交换机和队列进行绑定，还实现了普通队列发送消息、

一：延迟插件实现延迟队列

交换机类型必须CustomExchange

二：TTL + 死信队列/延迟交换机实现延迟队列

三： 延迟交换机 + 消息设置setHeader("x-delay", xxx)

以下配置了两个不同的rabbitMq服务器，每一个rabbitMq服务器对应多个队列，还配置了两个相同的rabbitMq服务器，只不过两个相同的rabbitMq服务器有不同的交换机，都是一个rabbitMq服务器可以对应相同的一套配置，代码功能服用性强，也方便快捷

```yaml
## 配置需要保证唯一不重复(eqps中的每一的index唯一,一般配置成递增的,队列交换机绑定关系的bean注入都是根据rps的List下标+eqps中index下标注入保证了唯一性)
zlf:
  rabbit:
    rps:
      ## 如果virtual-host不同,在配置一个即可,addresses不同也是可以在配置,eqps的下标以之对应上即可
      - rabbitmq:
        virtual-host: /dyict-uat
        addresses: 192.168.40.61
        port: 5672
        username: "admin"
        password: "admin"
      - rabbitmq:
        virtual-host: /test
        addresses: 192.168.40.60
        port: 5672
        username: "admin"
        password: "admin"
      - rabbitmq:
        virtual-host: /test2
        addresses: 192.168.40.60
        port: 5672
        username: "admin"
        password: "admin"
    eqps:
        ## 下标递增且唯一
        - index: 0
          eqs:
            - function-type: Delay
              delay-type: 1
              exchange-type: custom
              exchange-name: zlf.delay.test1
              queue-name: delay.test1
              routing-key: delay.test1.key
              exchange-args:
                x-delayed-type: direct
              queue-args: {}
            - function-type: Normal
              delay-type: 0
              exchange-type: direct
              exchange-name: zlf.normal.test1
              queue-name: normal.test1
              routing-key: normal.test1.key
              exchange-args: {}
              queue-args: {}
            - function-type: Delay
              delay-type: 2
              exchange-type: direct
              exchange-name: zlf.delay.test2
              queue-name: delay.test2
              ## 不用监听正常的队列,直接根据同一个路由键去路由,然后监听死信队列
              routing-key: zlf.delay-test2-key
              dlx-exchange-name: zlf.dlx-test1
              dlx-exchange-type: direct
              dlx-queue-name: dlx-test1
              dlx-key: zlf.dlx-test1-key
              exchange-args: {}
              queue-args:
                x-dead-letter-exchange: zlf.dlx-test1
                x-dead-letter-routing-key: zlf.dlx-test1-key
                ## 单位毫秒 30s
                x-message-ttl: 30000
            - function-type: Delay
              delay-type: 3
              exchange-type: direct
              exchange-name: zlf.delay.test3
              queue-name: delay.test3
              routing-key: zlf.delay-test3-key
              exchange-args: {}
              queue-args: {}

        - index: 1
          eqs:
            - function-type: Delay
              delay-type: 1
              exchange-type: custom
              exchange-name: zlf.delay.test1
              queue-name: delay.test1
              routing-key: delay.test1.key
              exchange-args:
                x-delayed-type: direct
              queue-args: {}
            - function-type: Normal
              delay-type: 0
              exchange-type: direct
              exchange-name: zlf.normal.test1
              queue-name: normal.test1
              routing-key: normal.test1.key
              exchange-args: {}
              queue-args: {}
            - function-type: Delay
              delay-type: 2
              exchange-type: direct
              exchange-name: zlf.delay.test2
              queue-name: delay.test2
              ## 不用监听正常的队列,直接根据同一个路由键去路由,然后监听死信队列
              routing-key: zlf.delay-test2-key
              dlx-exchange-name: zlf.dlx-test1
              dlx-exchange-type: direct
              dlx-queue-name: dlx-test1
              dlx-key: zlf.dlx-test1-key
              exchange-args: {}
              queue-args:
                x-dead-letter-exchange: zlf.dlx-test1
                x-dead-letter-routing-key: zlf.dlx-test1-key
                ## 单位毫秒 30s
                x-message-ttl: 30000
            - function-type: Delay
              delay-type: 3
              exchange-type: direct
              exchange-name: zlf.delay.test3
              queue-name: delay.test3
              routing-key: zlf.delay-test3-key
              exchange-args: {}
              queue-args: {}
        - index: 2
          eqs:
            - function-type: Delay
              delay-type: 1
              exchange-type: custom
              exchange-name: zlf.delay.test1
              queue-name: delay.test1
              routing-key: delay.test1.key
              exchange-args:
                x-delayed-type: direct
              queue-args: {}
            - function-type: Normal
              delay-type: 0
              exchange-type: direct
              exchange-name: zlf.normal.test1
              queue-name: normal.test1
              routing-key: normal.test1.key
              exchange-args: {}
              queue-args: {}
            - function-type: Delay
              delay-type: 2
              exchange-type: direct
              exchange-name: zlf.delay.test2
              queue-name: delay.test2
              ## 不用监听正常的队列,直接根据同一个路由键去路由,然后监听死信队列
              routing-key: zlf.delay-test2-key
              dlx-exchange-name: zlf.dlx-test1
              dlx-exchange-type: direct
              dlx-queue-name: dlx-test1
              dlx-key: zlf.dlx-test1-key
              exchange-args: {}
              queue-args:
                x-dead-letter-exchange: zlf.dlx-test1
                x-dead-letter-routing-key: zlf.dlx-test1-key
                ## 单位毫秒 30s
                x-message-ttl: 30000
            - function-type: Delay
              delay-type: 3
              exchange-type: direct
              exchange-name: zlf.delay.test3
              queue-name: delay.test3
              routing-key: zlf.delay-test3-key
              exchange-args: {}
              queue-args: {}
```



# 2.引入pom依赖如下

## 2.1 引入项目resources下libs中的jar包依赖如下

右键点击rabbitmq-spring-boot-start-1.0-SNAPSHOT.jar将该jar包手动导入（add as Library）,复制该jar包到resources下libs，若果maven自动导入就不用右键手动导入

```xml
<dependency>
    <groupId>org.zlf</groupId>
    <artifactId>rabbitmq-spring-boot-start</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>${pom.basedir}/src/main/resources/libs/rabbitmq-spring-boot-start-1.0-SNAPSHOT.jar</systemPath>
</dependency>
```



## 2.2引入maven私服依赖如下

```xml
<dependency>
    <groupId>org.zlf</groupId>
    <artifactId>rabbitmq-spring-boot-start</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```



# 3.启动类配置如下

```java
@EnableZlfRabbitMq
@SpringBootApplication(exclude = {
        RabbitAutoConfiguration.class})
```



# 4.项目中测试发送消息如下

Controller测试可以根据rps的下标 + eqps的index下标来复制多个Controlle类，只需要调用api设置这两个下标对应解析即可发送

 rabbitService.sendMsg6(0, 0, msg);

 rabbitService.sendMsg6(1, 0, msg);

 rabbitService.sendMsg6(2, 0, msg);

，，，，，，，，，，

上面配置了三个下标组合就有以下几种：

0 0 / 01 /11
1 0 / 11 /12

2 0 / 21 / 22

可以复制Controller1、Controller2、Controller3，，，，，，，调用时候只需要指定下标组合即可发送消息

```java
package xxxx.controller;

import com.dy.corporate.member.utils.SpringUtils;
import com.zlf.constants.ZlfMqRegistrarBeanNamePrefix;
import com.zlf.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("rabbit")
public class RabbitMqTestController {

    @Autowired
    private RabbitService rabbitService;

    @GetMapping("/sendDelayMsg")
    public String sendDelayMsg(@RequestParam(value = "msg") String msg) {
        log.info("sendDelayMsg.msg:{}", msg);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) SpringUtils.getBean(ZlfMqRegistrarBeanNamePrefix.rabbitTemplatePrefix + 0);
        rabbitService.sendDelayed(rabbitTemplate, "zlf.delay.test1", "delay.test1.key", msg, 10);
        return "ok";
    }

    @GetMapping("/sendDelayMsg2")
    public String sendDelayMsg2(@RequestParam(value = "msg") String msg) {
        log.info("sendDelayMsg2.msg:{}", msg);
        rabbitService.sendMsg6(0, 0, msg);
        return "ok";
    }

    @GetMapping("/sendNormalMsg")
    public String sendNormalMsg(@RequestParam(value = "msg") String msg) {
        log.info("sendNormalMsg.msg:{}", msg);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) SpringUtils.getBean(ZlfMqRegistrarBeanNamePrefix.rabbitTemplatePrefix + 0);
        rabbitService.sendMsg(rabbitTemplate, "zlf.delay.test1", "delay.test1.key", msg);
        return "ok";
    }

    @GetMapping("/sendNormalMsg2")
    public String sendNormalMsg2(@RequestParam(value = "msg") String msg) {
        log.info("sendNormalMsg2.msg:{}", msg);
        rabbitService.sendMsg6(0, 1, msg);
        return "ok";
    }

    @GetMapping("/sendDelayDlxMsg")
    public String sendDelayDlxMsg(@RequestParam(value = "msg") String msg) {
        log.info("sendDelayDlxMsg.msg:{}", msg);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) SpringUtils.getBean(ZlfMqRegistrarBeanNamePrefix.rabbitTemplatePrefix + 0);
        //正常发延迟交换机和延迟队列的路由键
        rabbitService.sendDelayed(rabbitTemplate, "zlf.delay.test2", "zlf.delay-test2-key", msg, 10);
        return "ok";
    }

    @GetMapping("/sendDelayDlxMsg2")
    public String sendDelayDlxMsg2(@RequestParam(value = "msg") String msg) {
        log.info("sendDelayDlxMsg2.msg:{}", msg);
        //正常发延迟交换机和延迟队列的路由键
        rabbitService.sendDelayed6(0, 2, msg,10);
        return "ok";
    }

    @GetMapping("/sendDelayMsg3")
    public String sendDelayMsg3(@RequestParam(value = "msg") String msg) {
        log.info("sendDelayMsg3.msg:{}", msg);
        rabbitService.sendDelayed6(0, 3, msg, 10);
        return "ok";
    }

}
```

项目utils下放入SpringUtils类：

```java
package xxx.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author zlf
 * @description spring上下文工具类
 * @date 2024/03/11
 **/
@Component
public class SpringUtils implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(SpringUtils.class);
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        logger.info("应用程序上下文 ： [{}]", "开始初始化");
        SpringUtils.applicationContext = applicationContext;
        logger.info("应用程序上下文 ： [{}]", "初始化完成");
    }

    /**
     * 获取applicationContext
     *发给
     * @return
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 通过name获取 Bean.
     *
     * @param name
     * @return
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过class获取Bean.
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     *
     * @param name
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

}
```

以上测试用例MqConsumer都是可以正常消费到对应队列中的消息的



# 5.项目中消费消息代码示例

消费者中只需要指定对应的消费监听工厂即可，监听工厂配置如下：

ZlfMqRegistrarBeanNamePrefix.simpleRabbitListenerContainerFactory  + rps的下标

可以复制多个MqConsumer1，MqConsumer2，，，，，，，，，

然后指定对应的监听工厂配置下标即可，经过测试上面三个配置发送4中发送消息，监听消费都是正常的

```java
package xxx.listener;

import com.rabbitmq.client.Channel;
import com.zlf.constants.ZlfMqRegistrarBeanNamePrefix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 手动ack业务demo
 * long deliveryTag = message.getMessageProperties().getDeliveryTag();
 * try {
 * int a = 1/0;
 * User user = JSONObject.parseObject(userStr,User.class);
 * log.info(user.toString());
 * //手动ack  第二个参数为false是表示仅仅确认当前消息 true表示确认之前所有的消息
 * channel.basicAck(deliveryTag,false);
 * } catch (Exception e) {
 * //手动nack 告诉rabbitmq该消息消费失败  第三个参数：如果被拒绝的消息应该被重新请求，而不是被丢弃或变成死信，则为true
 * try {
 * channel.basicNack(deliveryTag,false,false);
 * } catch (IOException ex) {
 * throw new RuntimeException("消息处理失败");
 * }
 * }
 * //channel.basicNack(); 不ack
 * //channel.basicReject(); 拒绝
 */
@Slf4j
@Component
public class MqConsumer {

    /**
     * 延迟插件实现延迟队列监听队列消息
     *
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitHandler
    @RabbitListener(queues = "delay.test1", containerFactory = ZlfMqRegistrarBeanNamePrefix.simpleRabbitListenerContainerFactory + 0)
    public void mqConsumer1(Message message, Channel channel) throws IOException {
        String msg = new String(message.getBody(), "UTF-8");
        try {
            log.info("mqConsumer1=====>msg:{}", msg);
        } catch (Exception e) {
            log.error("mqConsumer1消费异常:{}", e.getMessage());
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
        }
        //channel.basicNack(); 不ack
        //channel.basicReject(); 拒绝
    }

    /**
     * 普通队列监听队列消息
     *
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitHandler
    @RabbitListener(queues = "normal.test1", containerFactory = ZlfMqRegistrarBeanNamePrefix.simpleRabbitListenerContainerFactory + 0)
    public void mqConsumer2(Message message, Channel channel) throws IOException {
        String msg = new String(message.getBody(), "UTF-8");
        try {
            log.info("mqConsumer2=====>msg:{}", msg);
        } catch (Exception e) {
            log.error("mqConsumer2消费异常:{}", e.getMessage());
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    /**
     * TTL + 死信队列实现延迟队列监听延迟队列消息(此处省略)
     * 绑定的那个延迟队列,消息如果正常消费,则不会将消息投递到死信队列上,
     * 只有消息变成死信才会被投递到死信队列上
     */


    /**
     * TTL + 死信队列实现延迟队列监听死信队列消息
     * 成为死信的条件
     * * 1.队列消息长度到达限制。
     * * 2.消费者拒接消费消息，basicNack/basicReject，并且不把消息重新放入原目标队列，requeue=false。
     * * 3.原队列存在消息过期设置，消息到达超时时间未被消费。
     *
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitHandler
    @RabbitListener(
            //监听连接工程指定
            containerFactory = ZlfMqRegistrarBeanNamePrefix.simpleRabbitListenerContainerFactory + 0,
            bindings = @QueueBinding(
                    //延迟交换机
                    exchange = @Exchange(
                            value = "zlf.delay.test2",
                            //持久化参数设置
                            durable = "true",
                            //交换机类型指定
                            type = ExchangeTypes.DIRECT),
                    //延迟交换机路由延迟队列的key
                    key = "zlf.delay-test2-key",
                    //死信队列
                    value = @Queue(
                            value = "dlx-test1",
                            //持久化参数设置
                            durable = "true"
                            //, //autoDelete = "false",
                    )
                    //ignoreDeclarationExceptions = "true"
            )
            //,
            //concurrency = "1", // 指定监听该队列的消费者个数
            //ackMode = "MANUAL"// 手动ack
    )
    public void mqConsumer4(Message message, Channel channel) throws IOException {
        String msg = new String(message.getBody(), "UTF-8");
        try {
            log.info("mqConsumer4=====>msg:{}", msg);
        } catch (Exception e) {
            log.error("mqConsumer4:{}", e.getMessage());
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    /**
     * 延迟交换机 + 消息设置setHeader("x-delay", xxx)
     *
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitHandler
    @RabbitListener(queues = "delay.test3", containerFactory = ZlfMqRegistrarBeanNamePrefix.simpleRabbitListenerContainerFactory + 0)
    public void mqConsumer5(Message message, Channel channel) throws IOException {
        String msg = new String(message.getBody(), "UTF-8");
        try {
            log.info("mqConsumer5=====>msg:{}", msg);
        } catch (Exception e) {
            log.error("mqConsumer5消费异常:{}", e.getMessage());
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
        }
        //channel.basicNack(); 不ack
        //channel.basicReject(); 拒绝
    }

}
```



# 6.mq管理后台交换机队列创建及路由绑定关系如下

以下交换机和队列定义的时候都是持久化的,上面三个配置示都在rabbitMq的管理后台生成了相同的队列、交换机和绑定关系（唯一不一样的是错误对了是根据rps的List的下标来的，保证唯一），以下截图只展示一个即rps[0]的交换机、队列和绑定关系的创建，其它两个都是一样的（唯一不同就是错误队列交换机是后缀角标是递增的）。

zlf.delay.test1交换机（延迟插件）

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtRcNn6OdGNEj3VVoukVHlnR41k5hKAic5Z9EhF0ow1pL5CWB5GLQZpUQ/640?wx_fmt=png&amp;from=appmsg)

zlf.delay.test1交换和delay.test1队列的绑定关系：

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtP82icohNEmykgDu7fic0OVc499GQ0l8aWLsSpDdSVWTwrc0ia42ftyd1w/640?wx_fmt=png&amp;from=appmsg)

zlf.normal.test1交换机（普通交换机）

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtZTu8jtfJiaqVDBIFR4u46RqQib1AQuv1aW1K6cNOo7Q7TeiaB1MfsBiaoQ/640?wx_fmt=png&amp;from=appmsg)

zlf.normal.test1交换机和normal.test1的绑定关系：

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1Ujwt9CHeiaGk1nI1cM4365Vlec0blSrqT1LUFsXJSqI89g3d6jEbLvlNxibw/640?wx_fmt=png&amp;from=appmsg)

zlf.delay.test2交换机：(ttl + 死信队列)

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtXD2O9tL7mt5LRc9MyYcxACpQYlDe9pbQkaPpQFiaqvCkwnQicD8lapDA/640?wx_fmt=png&amp;from=appmsg)

zlf.delay.test2交换机和delay.test2绑定关系：

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtppHakCYK3CXNnUDRhu315l5AAd78MHFibNw1DdUuxpXHD8RJNOHs1Ww/640?wx_fmt=png&amp;from=appmsg)

zlf.dlx-test1交换机：(死信交换机)

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtxnLNjGblLlxAv1eNkxIKhribHTlsXn86fvyrHgQGcEjatj08ne5VnGg/640?wx_fmt=png&amp;from=appmsg)

zlf.dlx-test1死信交换机和dlx-test1绑定关系：

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtCdXiakkEbibI8tJYCmLCFAibIOUhM2aSNRolSre5VYC96sgK9Kl99T1Zw/640?wx_fmt=png&amp;from=appmsg)

zlf.delay.test3交换机：



![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtDbDicsjOpcEUzBUI4Ar9nw77VRZVES3lLU0UuU94tHz4UqL8HS09Jcg/640?wx_fmt=png&amp;from=appmsg)

zlf.delay.test3交换机delay.test3的绑定关系：

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtIicLVMhXvjmFw5OapwG3Uj2RgpuvibZjEvfN6Bn8GBRqesGiaJEwnacww/640?wx_fmt=png&amp;from=appmsg)

error交换机

error.direct + 一个下标，类型、路由键是固定

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwthaEfxibIMZbxCxtES031HQtfgbkgG3iaRWL8RticFes7Sug4Tia8e8d49Q/640?wx_fmt=png&amp;from=appmsg)

error.direct + 一个下标 和 error.queue+ 一个下标 的绑定关系：

![img](https://mmbiz.qpic.cn/mmbiz_png/hlks8yUe011s5SSNkicN0ZUCE3Sa1UjwtCJTA4HQnic5djmsRkic1qGiccwo4JSy2tPNxqf0UpYINMN5jjbicclSwFA/640?wx_fmt=png&amp;from=appmsg)

