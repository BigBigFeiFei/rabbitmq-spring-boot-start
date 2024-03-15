package com.zlf.event;

import lombok.Getter;
import org.springframework.amqp.core.Message;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReturnCallbackEvent extends ApplicationEvent {

    private static final long serialVersionUID = 4451965802494959288L;

    private Message message;

    private Integer replyCode;

    private String replyText;

    private String exchange;

    private String routingKey;

    public ReturnCallbackEvent(Object source, Message message, Integer replyCode, String replyText, String exchange, String routingKey) {
        super(source);
        this.message = message;
        this.replyCode = replyCode;
        this.replyText = replyText;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

}
