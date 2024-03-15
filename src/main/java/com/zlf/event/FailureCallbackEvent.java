package com.zlf.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
@Getter
public class FailureCallbackEvent extends ApplicationEvent {

    private static final long serialVersionUID = -3846317394863081319L;

    private Throwable throwable;

    public FailureCallbackEvent(Object source, Throwable throwable) {
        super(source);
        this.throwable = throwable;
    }
}
