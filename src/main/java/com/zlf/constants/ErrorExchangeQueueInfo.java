package com.zlf.constants;

import com.zlf.enums.ExchangeTypeEnum;

public interface ErrorExchangeQueueInfo {

    String ERROR_EXCHANGE_PREFIX = "error.direct";

    String ERROR_EXCHANGE_TYPE = ExchangeTypeEnum.DIRECT.getExchangeType();

    String ERROR_QUEUE_PREFIX = "error.queue";

    String ERROR_KEY_PREFIX = "error.key";

    String ERROR_BINDING_BANE_NAME_PREFIX = "error";

}
