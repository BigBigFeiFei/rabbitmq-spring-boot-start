package com.zlf.constants;

public interface ZlfMqRegistrarBeanNamePrefix {

    String cachingConnectionFactoryPrefix = "org.springframework.amqp.rabbit.connection.CachingConnectionFactory";

    String rabbitTemplatePrefix = "org.springframework.amqp.rabbit.core.RabbitTemplate";

    String rabbitAdmin = "org.springframework.amqp.rabbit.core.RabbitAdmin";

    String simpleRabbitListenerContainerFactory = "org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory";

    String directRabbitListenerContainerFactory = "org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory";

    String rabbitService = "rabbitService";

}
