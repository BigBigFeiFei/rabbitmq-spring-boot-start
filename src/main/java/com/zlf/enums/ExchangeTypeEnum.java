package com.zlf.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交换机类型枚举
 */
@Getter
@AllArgsConstructor
public enum ExchangeTypeEnum {

    //直连
    DIRECT("direct"),

    //主题
    TOPIC("topic"),

    //广播
    FANOUT("fanout"),

    //HEADERS
    HEADERS("headers"),

    //SYSTEM
    SYSTEM("system"),

    //自定义(延迟插件方式实现延迟队列,交换机类型必须使用CustomExchange)
    CUSTOM("custom");

    private String exchangeType;

}

