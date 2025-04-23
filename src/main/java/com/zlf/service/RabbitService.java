package com.zlf.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.zlf.config.ExchangeQueueConfig;
import com.zlf.config.ExchangeQueueProperties;
import com.zlf.config.RabbitConfig;
import com.zlf.config.RabbitProperties;
import com.zlf.constants.ZlfMqRegistrarBeanNamePrefix;
import com.zlf.dto.ExchangeQueueDto;
import com.zlf.enums.ExchangeTypeEnum;
import com.zlf.event.ConfirmCallbackEvent;
import com.zlf.event.FailureCallbackEvent;
import com.zlf.event.ReturnCallbackEvent;
import com.zlf.event.SuccessCallbackEvent;
import com.zlf.utils.ZlfMqSpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AbstractExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 公共服务api
 */
@Slf4j
@Service
public class RabbitService {

    /**
     * 创建队列
     *
     * @param rabbitAdmin
     * @param queueName
     * @param args
     * @return
     */
    public Queue createQueue(RabbitAdmin rabbitAdmin, String queueName, Map<String, Object> args) {
        Queue queue = new Queue(queueName, true, false, false, args);
        rabbitAdmin.declareQueue(queue);
        return queue;
    }

    /**
     * 删除一个队列
     *
     * @param rabbitAdmin
     * @param queueName
     * @return
     */
    private Boolean deleteQueue(RabbitAdmin rabbitAdmin, String queueName) {
        return rabbitAdmin.deleteQueue(queueName);
    }

    /**
     * 创建交换机
     *
     * @param exchangeName
     * @param exchangeType
     * @param args
     * @return
     */
    public AbstractExchange createExchange(RabbitAdmin rabbitAdmin, String exchangeName, String exchangeType, Map<String, Object> args, Boolean isDelayed) {
        AbstractExchange abstractExchange = null;
        if (ExchangeTypeEnum.DIRECT.getExchangeType().equals(exchangeType)) {
            abstractExchange = new DirectExchange(exchangeName, true, false, args);
        } else if (ExchangeTypeEnum.TOPIC.getExchangeType().equals(exchangeType)) {
            abstractExchange = new TopicExchange(exchangeName, true, false, args);
        } else if (ExchangeTypeEnum.FANOUT.getExchangeType().equals(exchangeType)) {
            abstractExchange = new FanoutExchange(exchangeName, true, false, args);
        } else if (ExchangeTypeEnum.HEADERS.getExchangeType().equals(exchangeType)) {
            abstractExchange = new HeadersExchange(exchangeName, true, false, args);
        } else if (ExchangeTypeEnum.SYSTEM.getExchangeType().equals(exchangeType)) {
            abstractExchange = new HeadersExchange(exchangeName, true, false, args);
        } else if (ExchangeTypeEnum.CUSTOM.getExchangeType().equals(exchangeType)) {
            //延迟插件方式实现延迟队列,交换机类型必须定义为:CustomExchange
            abstractExchange = new CustomExchange(exchangeName, "x-delayed-message", true, false, args);
        }
        if (isDelayed) {
            /**
             * 通过定义交换机位延迟交换机,发送消息设置setHeader("x-delay", 5000)
             * 可以实现发送延迟消息的功能
             */
            //设置延迟交换机
            abstractExchange.setDelayed(isDelayed);
            /**
             * 发消息时指定消息setHeader("x-delay", 5000)来实现延迟队列发消息的功能
             * Message message = MessageBuilder
             *                 .withBody("hello, ttl messsage".getBytes(StandardCharsets.UTF_8))
             *                 .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
             *                 .setHeader("x-delay", 5000)
             *                 .build();
             */
        }
        rabbitAdmin.declareExchange(abstractExchange);
        return abstractExchange;
    }

    /**
     * 删除一个交换机
     *
     * @param rabbitAdmin
     * @param exchangeName
     * @return
     */
    public boolean deleteExchange(RabbitAdmin rabbitAdmin, String exchangeName) {
        return rabbitAdmin.deleteExchange(exchangeName);
    }

    /**
     * 交换机与队列绑定
     *
     * @param rabbitAdmin
     * @param exchange
     * @param queue
     * @param routingKey
     * @return
     */
    public Binding binding(RabbitAdmin rabbitAdmin, AbstractExchange exchange, Queue queue, String routingKey) {
        Binding noargsBinding = BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs();
        rabbitAdmin.declareBinding(noargsBinding);
        return noargsBinding;
    }

    /**
     * 移除绑定关系
     *
     * @param rabbitAdmin
     * @param binding
     */
    public void removeBinding(RabbitAdmin rabbitAdmin, Binding binding) {
        rabbitAdmin.removeBinding(binding);
    }

    private void checkRabbitTemplate(RabbitTemplate rabbitTemplate) {
        if (Objects.isNull(rabbitTemplate)) {
            throw new RuntimeException("rabbitTemplate不为空");
        }
    }

    /**
     * 普通队列消息发送
     *
     * @param rabbitTemplate
     * @param exchangeName
     * @param routingKey
     * @param msg
     */
    public void sendMsg(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter4(exchangeName, routingKey, msg);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg);
    }

    /**
     * 普通队列发送消息默认消息id设置
     *
     * @param rabbitTemplate
     * @param exchangeName
     * @param routingKey
     * @param msg
     */
    public void sendMsg2(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter4(exchangeName, routingKey, msg);
        //消息ID
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, correlationData);
    }

    /**
     * 普通队列发送消息,自定义消息CorrelationData
     *
     * @param rabbitTemplate
     * @param exchangeName
     * @param routingKey
     * @param msg
     */
    public void sendMsg3(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg, String msgId) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter1(exchangeName, routingKey);
        this.checkParameter3(msg, msgId);
        if (StringUtils.isEmpty(msgId)) {
            throw new RuntimeException("msgId不为空");
        }
        //消息ID
        CorrelationData correlationData = new CorrelationData(msgId);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, correlationData);
    }

    /**
     * 普通队列发送消息,自定义消息CorrelationData
     *
     * @param rabbitTemplate
     * @param exchangeName
     * @param routingKey
     * @param msg
     * @param msgId
     */
    public void sendMsg4(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg, String msgId, String returnedMsg) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter1(exchangeName, routingKey);
        this.checkParameter2(msg, msgId, returnedMsg);
        //消息ID
        CorrelationData correlationData = new CorrelationData(msgId);
        Message rtMsg = new Message(returnedMsg.getBytes(StandardCharsets.UTF_8));
        correlationData.setReturnedMessage(rtMsg);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, correlationData);
    }

    public void sendMsg5(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg, CorrelationData correlationData) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter5(exchangeName, routingKey, msg);
        this.checkParameter6(correlationData);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, correlationData);
    }

    public void sendMsg6(Integer eqpsIndex, Integer eqsIndex, Object msg) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendMsg(rabbitTemplate, exchangeName, routingKey, msg);
    }

    public void sendMsg7(Integer eqpsIndex, Integer eqsIndex, Object msg) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendMsg2(rabbitTemplate, exchangeName, routingKey, msg);
    }

    public void sendMsg8(Integer eqpsIndex, Integer eqsIndex, Object msg, String msgId) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendMsg3(rabbitTemplate, exchangeName, routingKey, msg, msgId);
    }

    public void sendMsg9(Integer eqpsIndex, Integer eqsIndex, Object msg, String msgId, String returnedMsg) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendMsg4(rabbitTemplate, exchangeName, routingKey, msg, msgId, returnedMsg);
    }

    public void sendMsg10(Integer eqpsIndex, Integer eqsIndex, Object msg, CorrelationData correlationData) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendMsg5(rabbitTemplate, exchangeName, routingKey, msg, correlationData);
    }

    /**
     * 发送延迟消息
     * 一：如果队列设置了：x-message-ttl 参数 是针对整个队列的ttl的设置
     * 二：也可以针对单条消息设置ttl,有以下两种方式：
     * 1.消息头设置x-delay属性---setHeader
     * message.getMessageProperties().setHeader("x-delay", expTime)
     * 2.消息设置过期时间属性----setExpiration
     * message.getMessageProperties().setExpiration(expTime.toString());
     * 一和二方式二选一
     * 如果都配置了,则优先生效的是值最小的一个
     * <p>
     * 成为死信的条件
     * 1.队列消息长度到达限制。
     * 2.消费者拒接消费消息，basicNack/basicReject，并且不把消息重新放入原目标队列，requeue=false。
     * 3.原队列存在消息过期设置，消息到达超时时间未被消费。
     *
     * @param rabbitTemplate
     * @param exchangeName
     * @param routingKey
     * @param msg
     * @param time
     */
    public void sendDelayed(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg, long time) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter7(exchangeName, routingKey, msg, time);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, message -> {
            //过期时间(单位毫秒)
            Long expTime = 1000 * time;
            message.getMessageProperties().setHeader("x-delay", expTime);
            //message.getMessageProperties().setExpiration(expTime.toString());
            return message;
        });
    }

    public void sendDelayed2(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg, long time) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter7(exchangeName, routingKey, msg, time);
        //消息ID
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, message -> {
            //过期时间(单位毫秒)
            Long expTime = 1000 * time;
            message.getMessageProperties().setHeader("x-delay", expTime);
            //message.getMessageProperties().setExpiration(expTime.toString());
            return message;
        }, correlationData);
    }

    public void sendDelayed3(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg, long time, String msgId) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter10(exchangeName, routingKey, msg, time, msgId);
        //消息ID
        CorrelationData correlationData = new CorrelationData(msgId);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, message -> {
            //过期时间(单位毫秒)
            Long expTime = 1000 * time;
            message.getMessageProperties().setHeader("x-delay", expTime);
            //message.getMessageProperties().setExpiration(expTime.toString());
            return message;
        }, correlationData);
    }

    public void sendDelayed4(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg, long time, String msgId, String returnedMsg) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter8(exchangeName, routingKey, msg, time, msgId, returnedMsg);
        //消息ID
        CorrelationData correlationData = new CorrelationData(msgId);
        Message rtMsg = new Message(returnedMsg.getBytes(StandardCharsets.UTF_8));
        correlationData.setReturnedMessage(rtMsg);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, message -> {
            //过期时间(单位毫秒)
            Long expTime = 1000 * time;
            message.getMessageProperties().setHeader("x-delay", expTime);
            //message.getMessageProperties().setExpiration(expTime.toString());
            return message;
        }, correlationData);
    }

    public void sendDelayed5(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Object msg, long time, CorrelationData correlationData) {
        this.checkRabbitTemplate(rabbitTemplate);
        this.checkParameter6(correlationData);
        this.checkParameter9(exchangeName, routingKey, msg, time, correlationData);
        //消息ID
        rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, message -> {
            //过期时间(单位毫秒)
            Long expTime = 1000 * time;
            message.getMessageProperties().setHeader("x-delay", expTime);
            //message.getMessageProperties().setExpiration(expTime.toString());
            return message;
        }, correlationData);
    }

    public void sendDelayed6(Integer eqpsIndex, Integer eqsIndex, Object msg, long time) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendDelayed(rabbitTemplate, exchangeName, routingKey, msg, time);
    }

    public void sendDelayed7(Integer eqpsIndex, Integer eqsIndex, Object msg, long time) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendDelayed2(rabbitTemplate, exchangeName, routingKey, msg, time);
    }

    public void sendDelayed8(Integer eqpsIndex, Integer eqsIndex, Object msg, long time, String msgId) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendDelayed3(rabbitTemplate, exchangeName, routingKey, msg, time, msgId);
    }

    /**
     * 设置RabbitTemplate是否需要设置两个回调函数,发布event时间
     *
     * @param eqpsIndex
     * @param hasConfirmCallback
     * @param hasReturnCallback
     * @return
     */
    public RabbitTemplate setCallback(Integer eqpsIndex, Boolean hasConfirmCallback, Boolean hasReturnCallback) {
        HashMap<String, Object> map = this.getMapByEqpsIndex(eqpsIndex);
        ConfirmType confirmType = (ConfirmType) map.get("publisherConfirmType");
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        if (ConfirmType.CORRELATED.equals(confirmType) && hasConfirmCallback) {
            rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                if (Objects.nonNull(correlationData)) {
                    if (Objects.nonNull(ack) && ack) {
                        log.info("setCallback===>消息发送成功->correlationData:{}", JSON.toJSONString(correlationData));
                    } else if (StringUtils.isNotBlank(cause)) {
                        log.error("setCallback===>消息->correlationData:{}->发送失败原因->{}", JSON.toJSONString(correlationData), cause);
                    }
                }
                if (Objects.nonNull(ack) && ack) {
                    log.info("setCallback===>消息发送成功ack:{}", ack);
                }
                if (StringUtils.isNotBlank(cause)) {
                    log.error("setCallback===>消息发送失败原因->cause:{}", cause);
                }
                if (Objects.isNull(correlationData) && Objects.isNull(ack) && StringUtils.isEmpty(cause)) {
                    log.info("setCallback===>消息发送成功,收到correlationData,ack,cause都是null");
                }
                ZlfMqSpringUtils.getApplicationContext().publishEvent(new ConfirmCallbackEvent(this, correlationData, ack, cause));
                log.info("setCallback===>发布ConfirmCallbackEvent消息完成");
            });
        }
        Boolean publisherReturns = (Boolean) map.get("publisherReturns");
        Boolean mandatory = (Boolean) map.get("mandatory");
        if (mandatory && publisherReturns && hasReturnCallback) {
            //设置消息回调
            rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
                log.error("setCallback===>消息->{}路由失败", message);
                ZlfMqSpringUtils.getApplicationContext().publishEvent(new ReturnCallbackEvent(this, message, replyCode, replyText, exchange, routingKey));
                log.info("setCallback===>发布ReturnCallbackEvent消息完成");
                // 如果有需要的话，重发消息
            });
        }

        return rabbitTemplate;
    }

    public void sendDelayed9(Integer eqpsIndex, Integer eqsIndex, Object msg, long time, String msgId, String returnedMsg) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendDelayed4(rabbitTemplate, exchangeName, routingKey, msg, time, msgId, returnedMsg);
    }

    public void sendDelayed10(Integer eqpsIndex, Integer eqsIndex, Object msg, long time, CorrelationData correlationData) {
        HashMap<String, Object> map = this.getMapByIndex(eqpsIndex, eqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) map.get("rabbitTemplate");
        String exchangeName = (String) map.get("exchangeName");
        String routingKey = (String) map.get("routingKey");
        this.sendDelayed5(rabbitTemplate, exchangeName, routingKey, msg, time, correlationData);
    }

    public RabbitProperties getRabbitPropertiesByEqpsIndex(Integer eqpsIndex) {
        RabbitConfig rabbitConfig = ZlfMqSpringUtils.getBean(RabbitConfig.class);
        List<RabbitProperties> rps = rabbitConfig.getRps();
        if (CollectionUtil.isNotEmpty(rps)) {
            return rps.get(eqpsIndex);
        }
        return null;
    }

    public ExchangeQueueProperties getExchangeQueuePropertiesByEqpsIndex(Integer eqpsIndex) {
        ExchangeQueueConfig eqcBean = ZlfMqSpringUtils.getBean(ExchangeQueueConfig.class);
        List<ExchangeQueueProperties> eqps = eqcBean.getEqps();
        if (CollectionUtil.isNotEmpty(eqps)) {
            return eqps.get(eqpsIndex);
        }
        return null;
    }

    public ExchangeQueueDto getExchangeQueueDtoByEqsIndex(ExchangeQueueProperties exchangeQueueProperties, Integer eqsIndex) {
        List<ExchangeQueueDto> eqs = exchangeQueueProperties.getEqs();
        if (CollectionUtil.isNotEmpty(eqs)) {
            return eqs.get(eqsIndex);
        }
        return null;
    }

    public CorrelationData createCorrelationData(String msgId, String returnedMsg) {
        this.checkMsgIdAndReturnedMsg(msgId, returnedMsg);
        CorrelationData correlationData = new CorrelationData(msgId);
        Message rtMsg = new Message(returnedMsg.getBytes(StandardCharsets.UTF_8));
        correlationData.setReturnedMessage(rtMsg);
        SuccessCallback<Confirm> successCallback = confirm -> {
            ZlfMqSpringUtils.getApplicationContext().publishEvent(new SuccessCallbackEvent(this, confirm));
        };
        FailureCallback failureCallback = throwable -> {
            ZlfMqSpringUtils.getApplicationContext().publishEvent(new FailureCallbackEvent(this, throwable));
        };
        correlationData.getFuture().addCallback(successCallback, failureCallback);
        return correlationData;
    }

    public CorrelationData createCorrelationData2(String msgId) {
        this.checkMsgId(msgId);
        CorrelationData correlationData = new CorrelationData(msgId);
        SuccessCallback<Confirm> successCallback = confirm -> {
            ZlfMqSpringUtils.getApplicationContext().publishEvent(new SuccessCallbackEvent(this, confirm));
        };
        FailureCallback failureCallback = throwable -> {
            ZlfMqSpringUtils.getApplicationContext().publishEvent(new FailureCallbackEvent(this, throwable));
        };
        correlationData.getFuture().addCallback(successCallback, failureCallback);
        return correlationData;
    }

    public CorrelationData createCorrelationData3(String msgId, String returnedMsg, SuccessCallback<Confirm> successCallback, FailureCallback failureCallback) {
        this.checkMsgIdAndReturnedMsg(msgId, returnedMsg);
        CorrelationData correlationData = new CorrelationData(msgId);
        Message rtMsg = new Message(returnedMsg.getBytes(StandardCharsets.UTF_8));
        correlationData.setReturnedMessage(rtMsg);
        correlationData.getFuture().addCallback(successCallback, failureCallback);
        return correlationData;
    }

    public CorrelationData createCorrelationData4(String msgId, SuccessCallback<Confirm> successCallback, FailureCallback failureCallback) {
        this.checkMsgId(msgId);
        CorrelationData correlationData = new CorrelationData(msgId);
        correlationData.getFuture().addCallback(successCallback, failureCallback);
        return correlationData;
    }

    private void checkMsgId(String msgId) {
        if (StringUtils.isEmpty(msgId)) {
            throw new RuntimeException("创建的CorrelationData中的msgId不为空");
        }
    }

    private void checkMsgIdAndReturnedMsg(String msgId, String returnedMsg) {
        this.checkMsgId(msgId);
        if (StringUtils.isEmpty(returnedMsg)) {
            throw new RuntimeException("创建的CorrelationData中的returnedMsg不为空");
        }
    }

    public HashMap<String, Object> getMapByEqpsIndex(Integer eqpsIndex) {
        HashMap<String, Object> map = new HashMap<>();
        RabbitProperties rabbitProperties = this.getRabbitPropertiesByEqpsIndex(eqpsIndex);
        if (Objects.isNull(rabbitProperties)) {
            throw new RuntimeException("对应第" + eqpsIndex + "个的RabbitProperties未配置请检查");
        }
        map.put("publisherConfirmType", rabbitProperties.getPublisherConfirmType());
        map.put("publisherReturns", rabbitProperties.getPublisherReturns());
        map.put("mandatory", rabbitProperties.getTemplate().getMandatory());
        RabbitTemplate rabbitTemplate = (RabbitTemplate) ZlfMqSpringUtils.getApplicationContext().getBean(ZlfMqRegistrarBeanNamePrefix.rabbitTemplatePrefix + eqpsIndex);
        if (Objects.isNull(rabbitTemplate)) {
            throw new RuntimeException("获取对应第" + eqpsIndex + "个的RabbitTemplate的bean未配置请检查");
        }
        map.put("rabbitTemplate", rabbitTemplate);
        return map;
    }


    private HashMap<String, Object> getMapByIndex(Integer eqpsIndex, Integer eqsIndex) {
        HashMap<String, Object> map = new HashMap<>();
        ExchangeQueueProperties exchangeQueueProperties = this.getExchangeQueuePropertiesByEqpsIndex(eqpsIndex);
        if (Objects.isNull(exchangeQueueProperties)) {
            throw new RuntimeException("对应第" + eqpsIndex + "个的ExchangeQueueProperties未配置请检查");
        }
        map.put("exchangeQueueProperties", exchangeQueueProperties);
        ExchangeQueueDto exchangeQueueDtoByEqsIndex = this.getExchangeQueueDtoByEqsIndex(exchangeQueueProperties, eqsIndex);
        if (Objects.isNull(exchangeQueueDtoByEqsIndex)) {
            throw new RuntimeException("对应第" + eqpsIndex + "-" + eqsIndex + "个的ExchangeQueueDto未配置请检查");
        }
        map.put("exchangeQueueDtoByEqsIndex", exchangeQueueDtoByEqsIndex);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) ZlfMqSpringUtils.getApplicationContext().getBean(ZlfMqRegistrarBeanNamePrefix.rabbitTemplatePrefix + eqpsIndex);
        if (Objects.isNull(rabbitTemplate)) {
            throw new RuntimeException("获取对应第" + eqpsIndex + "个的RabbitTemplate的bean未配置请检查");
        }
        map.put("rabbitTemplate", rabbitTemplate);
        String exchangeName = exchangeQueueDtoByEqsIndex.getExchangeName();
        if (StringUtils.isEmpty(exchangeName)) {
            throw new RuntimeException("对应第" + eqpsIndex + "-" + eqsIndex + "个的exchangeName未配置请检查");
        }
        map.put("exchangeName", exchangeName);
        String routingKey = exchangeQueueDtoByEqsIndex.getRoutingKey();
        if (StringUtils.isEmpty(routingKey)) {
            throw new RuntimeException("对应第" + eqpsIndex + "-" + eqsIndex + "个的routingKey未配置请检查");
        }
        map.put("routingKey", routingKey);
        return map;
    }

    private void checkParameter1(String exchangeName, String queueName) {
        if (StringUtils.isEmpty(exchangeName)) {
            throw new RuntimeException("exchangeName不为空");
        }
        if (StringUtils.isEmpty(queueName)) {
            throw new RuntimeException("queueName不为空");
        }
    }

    private void checkParameter2(Object msg, String msgId, String returnedMsg) {
        if (Objects.isNull(msg)) {
            throw new RuntimeException("msg不为空");
        }
        if (StringUtils.isEmpty(msgId)) {
            throw new RuntimeException("msgId不为空");
        }
        if (StringUtils.isEmpty(returnedMsg)) {
            throw new RuntimeException("returnedMsg不为空");
        }
    }

    private void checkParameter3(Object msg, String msgId) {
        if (Objects.isNull(msg)) {
            throw new RuntimeException("msg不为空");
        }
        if (StringUtils.isEmpty(msgId)) {
            throw new RuntimeException("msgId不为空");
        }
    }

    private void checkParameter4(String exchangeName, String routingKey, Object msg) {
        if (StringUtils.isEmpty(exchangeName)) {
            throw new RuntimeException("exchangeName不为空");
        }
        if (StringUtils.isEmpty(routingKey)) {
            throw new RuntimeException("routingKey不为空");
        }
        if (Objects.isNull(msg)) {
            throw new RuntimeException("msg不为空");
        }
    }

    private void checkParameter5(String exchangeName, String queueName, Object msg) {
        if (StringUtils.isEmpty(exchangeName)) {
            throw new RuntimeException("exchangeName不为空");
        }
        if (StringUtils.isEmpty(queueName)) {
            throw new RuntimeException("queueName不为空");
        }
        if (Objects.isNull(msg)) {
            throw new RuntimeException("msg不为空");
        }
    }

    private void checkParameter6(CorrelationData correlationData) {
        if (Objects.isNull(correlationData)) {
            throw new RuntimeException("correlationData不为空");
        }
    }

    private void checkParameter7(String exchangeName, String routingKey, Object msg, long time) {
        if (StringUtils.isEmpty(exchangeName)) {
            throw new RuntimeException("exchangeName不为空");
        }
        if (StringUtils.isEmpty(routingKey)) {
            throw new RuntimeException("routingKey不为空");
        }
        if (Objects.isNull(msg)) {
            throw new RuntimeException("msg不为空");
        }
        if (Objects.isNull(time)) {
            throw new RuntimeException("time不为空");
        }
        if (time <= 0) {
            throw new RuntimeException("time需大于0(单位是秒)");
        }
    }

    private void checkParameter8(String exchangeName, String routingKey, Object msg, long time, String msgId, String returnedMsg) {
        this.checkParameter7(exchangeName, routingKey, msg, time);
        if (StringUtils.isEmpty(msgId)) {
            throw new RuntimeException("msgId不为空");
        }
        if (StringUtils.isEmpty(returnedMsg)) {
            throw new RuntimeException("returnedMsg不为空");
        }
    }

    private void checkParameter9(String exchangeName, String routingKey, Object msg, long time, CorrelationData correlationData) {
        this.checkParameter7(exchangeName, routingKey, msg, time);
        this.checkParameter6(correlationData);
    }

    private void checkParameter10(String exchangeName, String routingKey, Object msg, long time, String msgId) {
        this.checkParameter7(exchangeName, routingKey, msg, time);
        if (StringUtils.isEmpty(msgId)) {
            throw new RuntimeException("msgId不为空");
        }
    }

}
