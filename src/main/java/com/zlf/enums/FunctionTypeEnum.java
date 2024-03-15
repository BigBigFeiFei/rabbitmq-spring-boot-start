package com.zlf.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 功能类型枚举
 */
@Getter
@AllArgsConstructor
public enum FunctionTypeEnum {

    //普通队列
    NORMAL("Normal"),

    //延迟队列
    DELAY("Delay");

    private String functionType;

}

