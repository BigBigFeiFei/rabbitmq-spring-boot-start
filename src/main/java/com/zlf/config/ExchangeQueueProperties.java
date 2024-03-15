package com.zlf.config;

import com.zlf.dto.ExchangeQueueDto;
import lombok.Data;

import java.util.List;

@Data
public class ExchangeQueueProperties {

    /**
     * 下标--跟RabbitConfig中List的下标对应
     */
    private Integer index;

    /**
     * 队列交换机配置
     */
    private List<ExchangeQueueDto> eqs;

}
