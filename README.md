# rabbitmq-spring-boot-start

rabbitmq-spring-boot-start启动器<br>
1. 项目中引入依赖如下：<br>
```
<dependency>
    <groupId>io.github.bigbigfeifei</groupId>
    <artifactId>rabbitmq-spring-boot-start</artifactId>
    <version>1.0</version>
</dependency>
```        
2. nacos配置如下：<br>
```
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
3. 启动类上加入如下注解：<br>
   @EnableZlfRabbitMq<br>
   @SpringBootApplication(exclude = {<br>
   RabbitAutoConfiguration.class})<br>
4. 功能说明<br>
   4.1 配置可以实现发送非延迟的普通队列消息<br>
   4.2 配置可以实现延迟队列(延迟插件方式)<br>
   4.3 配置可以实现延迟队列(ttl + 死性队列方式)<br>
   4.4 配置可以实现延迟队列(普通设置delayed属性就变成延迟交换机 + 消息设置setHeader("x-delay", xxx))<br>
5. rabbitmq-spring-boot-start配置使用手册<br>
   该使用手册在项目源码的resources目录下,可以参看该手册,写的也非常详细
   
6.文章

https://blog.csdn.net/qq_34905631/article/details/127231242?spm=1001.2014.3001.5501

https://mp.weixin.qq.com/s/vsRhFaCZin-MIlt3ihUBsQ

https://blog.csdn.net/qq_34905631/article/details/136677232?spm=1001.2014.3001.5501

https://mp.weixin.qq.com/s/-A9kuKHA5-4teKoVG0krOg

https://blog.csdn.net/qq_34905631/article/details/136677368?spm=1001.2014.3001.5501

https://mp.weixin.qq.com/s/-A9kuKHA5-4teKoVG0krOg