package com.zlf.dto;

import lombok.Data;

import java.util.HashMap;

@Data
public class ExchangeQueueDto {
    /**
     * 功能类型：
     * Normal:普通队列
     * Delay:延迟队列
     */
    private String functionType;

    /**
     * 延迟类型实现方式：
     * 0：无延迟
     * 1：延迟插件（交换机类型必须CustomExchange）
     * 2：TTL + 死信队列
     */
    private Integer DelayType;

    /**
     * 交换机类型
     */
    private String exchangeType;

    /**
     * 交换机名称
     */
    private String exchangeName;

    /**
     * 队列名称
     */
    private String queueName;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 死信交换机名称
     */
    private String dlxExchangeName = "";

    /**
     * 死信队列名称
     */
    private String dlxQueueName = "";

    /**
     * 死信交换机类型
     */
    private String dlxExchangeType = "";

    /**
     * 消息投递死信路由键
     */
    private String dlxKey = "";

    /**
     * 交换机参数
     */
    private HashMap<String, Object> exchangeArgs = new HashMap<>();

    /**
     * 队列参数
     */
    private HashMap<String, Object> queueArgs = new HashMap<>();

}
