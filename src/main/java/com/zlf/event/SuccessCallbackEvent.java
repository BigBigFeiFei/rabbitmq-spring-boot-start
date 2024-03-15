package com.zlf.event;

import lombok.Getter;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.context.ApplicationEvent;

@Getter
public class SuccessCallbackEvent extends ApplicationEvent {

    private static final long serialVersionUID = 964670212789938080L;

    private CorrelationData.Confirm confirm;

    public SuccessCallbackEvent(Object source, CorrelationData.Confirm confirm) {
        super(source);
        this.confirm = confirm;
    }

}
