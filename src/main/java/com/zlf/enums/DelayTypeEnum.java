package com.zlf.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 延迟类型枚举
 */
@Getter
@AllArgsConstructor
public enum DelayTypeEnum {

    //0 无延迟
    ZERO(0),

    //1 延迟插件（交换机类型必须CustomExchange）
    ONE(1),

    //2 TTL + 死信队列
    TWO(2),

    //3 延迟交换机 + 消息设置setHeader("x-delay", xxx)
    THREE(3);

    private Integer DelayType;

}

