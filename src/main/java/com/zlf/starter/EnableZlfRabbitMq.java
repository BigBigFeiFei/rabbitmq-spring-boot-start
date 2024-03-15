package com.zlf.starter;

import com.zlf.service.RabbitService;
import com.zlf.utils.ZlfMqSpringUtils;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用需要在启动类上加入@EnableZlfRabbit注解
 * 启动类上排除默认的自动装配RabbitAutoConfiguration
 *
 * @author zlf
 * 启动类上加入如下配置
 * @SpringBootApplication(exclude = {
 * RabbitAutoConfiguration.class})
 * @Import(value = {RabbitService.class, ZlfMqSpringUtils.class})
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({ZlfRabbitMqRegistrar.class, RabbitService.class, ZlfMqSpringUtils.class})
public @interface EnableZlfRabbitMq {

}
