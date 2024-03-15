package com.zlf.event;

import lombok.Getter;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.context.ApplicationEvent;

@Getter
public class ConfirmCallbackEvent extends ApplicationEvent {

    private static final long serialVersionUID = 3474924646717138488L;

    private CorrelationData correlationData;

    private Boolean ack;

    private String cause;

    public ConfirmCallbackEvent(Object source, CorrelationData correlationData, Boolean ack, String cause) {
        super(source);
        this.correlationData = correlationData;
        this.ack = ack;
        this.cause = cause;
    }

}
