package com.zlf.starter;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.zlf.config.ExchangeQueueConfig;
import com.zlf.config.ExchangeQueueProperties;
import com.zlf.config.RabbitConfig;
import com.zlf.config.RabbitProperties;
import com.zlf.config.RabbitProperties.AmqpContainer;
import com.zlf.config.RabbitProperties.Cache;
import com.zlf.config.RabbitProperties.Cache.Connection;
import com.zlf.config.RabbitProperties.ContainerType;
import com.zlf.config.RabbitProperties.DirectContainer;
import com.zlf.config.RabbitProperties.ListenerRetry;
import com.zlf.config.RabbitProperties.Retry;
import com.zlf.config.RabbitProperties.SimpleContainer;
import com.zlf.config.RabbitProperties.Template;
import com.zlf.constants.ErrorExchangeQueueInfo;
import com.zlf.dto.ExchangeQueueDto;
import com.zlf.enums.DelayTypeEnum;
import com.zlf.enums.ExchangeTypeEnum;
import com.zlf.enums.FunctionTypeEnum;
import com.zlf.service.RabbitService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AbstractExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.rabbit.support.ValueExpression;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * spring:
 * rabbitmq:
 * listener:
 * simple:
 * acknowledge-mode: auto #由spring监测listener代码是否出现异常，没有异常则返回ack；抛出异常则返回nack
 * manual：手动ack，需要在业务代码结束后，调用api发送ack。
 * auto：自动ack，由spring监测listener代码是否出现异常，没有异常则返回ack；抛出异常则返回nack
 * none：关闭ack，MQ假定消费者获取消息后会成功处理，因此消息投递后立即被删除(此时，消息投递是不可靠的，可能丢失)
 * <p>
 * 原文链接：https://blog.csdn.net/m0_53142956/article/details/127792054
 *
 * @author zlf
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({RabbitTemplate.class, Channel.class})
@EnableConfigurationProperties(RabbitConfig.class)
public class ZlfRabbitMqRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    private BeanFactory beanFactory;
    private RabbitConfig rabbitConfig;
    private ExchangeQueueConfig exchangeQueueConfig;

    @SneakyThrows
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        List<RabbitProperties> rps = rabbitConfig.getRps();
        if (CollectionUtils.isEmpty(rps)) {
            throw new RuntimeException("rabbitMq的rps配置不为空,请检查配置!");
        }
        log.info("zlf.registerBeanDefinitions:rps.size:{},rps:{}", rps.size(), JSON.toJSONString(rps));
        List<ExchangeQueueProperties> eqps = exchangeQueueConfig.getEqps();
        if (CollectionUtils.isEmpty(eqps)) {
            throw new RuntimeException("rabbitMq的eqps配置不为空,请检查配置!");
        }
        log.info("zlf.registerBeanDefinitions:eqps.size:{},rps:{}", eqps.size(), JSON.toJSONString(eqps));
        for (int i = 0; i < rps.size(); i++) {
            this.checkRabbitProperties(rps.get(i));
            CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(getRabbitConnectionFactoryBean(rps.get(i)).getObject());
            cachingConnectionFactory.setAddresses(rps.get(i).determineAddresses());
            cachingConnectionFactory.setPublisherReturns(rps.get(i).getPublisherReturns());
            cachingConnectionFactory.setPublisherConfirmType(rps.get(i).getPublisherConfirmType());
            Cache.Channel channel = rps.get(i).getCache().getChannel();
            if (Objects.nonNull(channel.getSize())) {
                cachingConnectionFactory.setChannelCacheSize(channel.getSize());
            }
            if (Objects.nonNull(channel.getCheckoutTimeout())) {
                Duration checkoutTimeout = channel.getCheckoutTimeout();
                cachingConnectionFactory.setChannelCheckoutTimeout(checkoutTimeout.toMillis());
            }
            Connection connection = rps.get(i).getCache().getConnection();
            if (Objects.nonNull(connection.getMode())) {
                cachingConnectionFactory.setCacheMode(connection.getMode());
            }
            if (Objects.nonNull(connection.getSize())) {
                cachingConnectionFactory.setConnectionCacheSize(connection.getSize());
            }
            // 注册cachingConnectionFactory的bean
            ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(CachingConnectionFactory.class.getName() + i, cachingConnectionFactory);
            log.info("zlf.ConfigurableBeanFactory注册完成,beanName:{}", CachingConnectionFactory.class.getName() + i);
            // 注册RabbitAdmin的bean
            RabbitAdmin rabbitAdmin = new RabbitAdmin(cachingConnectionFactory);
            ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(RabbitAdmin.class.getName() + i, rabbitAdmin);
            log.info("zlf.RabbitAdmin注册完成,beanName:{}", RabbitAdmin.class.getName() + i);
            //构建发送的RabbitTemplate实例关联连接工厂
            Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
            RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
            rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
            Template template = rps.get(i).getTemplate();
            ConfirmType publisherConfirmType = rps.get(i).getPublisherConfirmType();
            log.info("第{}个配置的publisherConfirmType:{}", i, JSON.toJSONString(publisherConfirmType));
            //生产者confirm
            /**
             * publish-confirm-type：开启publisher-confirm，这里支持两种类型：
             * simple：【同步】等待confirm结果，直到超时（可能引起代码阻塞）
             * correlated：【异步】回调，定义ConfirmCallback，MQ返回结果时会回调这个ConfirmCallback
             * publish-returns：开启publish-return功能，同样是基于callback机制，不过是定义ReturnCallback
             * template.mandatory：
             * 定义当消息从交换机路由到队列失败时的策略。
             * 【true，则调用ReturnCallback；false：则直接丢弃消息】
             */
            if (ConfirmType.CORRELATED.equals(publisherConfirmType)) {
                rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                    if (Objects.nonNull(correlationData)) {
                        if (Objects.nonNull(ack) && ack) {
                            log.info("消息发送成功->correlationData:{}", JSON.toJSONString(correlationData));
                        } else if (StringUtils.isNotBlank(cause)) {
                            log.error("消息->correlationData:{}->发送失败原因->{}", JSON.toJSONString(correlationData), cause);
                        }
                    }
                    if (Objects.nonNull(ack) && ack) {
                        log.info("消息发送成功ack:{}", ack);
                    }
                    if (StringUtils.isNotBlank(cause)) {
                        log.error("消息发送失败原因->cause:{}", cause);
                    }
                    if (Objects.isNull(correlationData) && Objects.isNull(ack) && StringUtils.isEmpty(cause)) {
                        log.info("消息发送成功,收到correlationData,ack,cause都是null");
                    }
                });
            }
            Boolean publisherReturns = rps.get(i).getPublisherReturns();
            Boolean mandatory = template.getMandatory();
            log.info("第{}个配置的publisherReturns:{},mandatory:{}", i, publisherReturns, mandatory);
            //消息回调
            //开启强制回调
            if (mandatory && publisherReturns) {
                rabbitTemplate.setMandatory(template.getMandatory());
                rabbitTemplate.setMandatoryExpression(new ValueExpression<>(template.getMandatory()));
                //设置消息回调
                rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
                    log.error("消息->{}路由失败", message);
                    // 如果有需要的话，重发消息
                });
            }
            Retry retry = rps.get(i).getTemplate().getRetry();
            if (retry.isEnabled()) {
                RetryTemplate retryTemplate = new RetryTemplate();
                SimpleRetryPolicy policy = new SimpleRetryPolicy();
                retryTemplate.setRetryPolicy(policy);
                policy.setMaxAttempts(retry.getMaxAttempts());
                ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
                backOffPolicy.setMultiplier(retry.getMultiplier());
                if (Objects.nonNull(retry.getMaxInterval())) {
                    backOffPolicy.setMaxInterval(retry.getMaxInterval().toMillis());
                }
                rabbitTemplate.setRetryTemplate(retryTemplate);
            }
            Duration receiveTimeout = template.getReceiveTimeout();
            if (Objects.nonNull(receiveTimeout)) {
                rabbitTemplate.setReceiveTimeout(receiveTimeout.toMillis());
            }
            Duration replyTimeout = template.getReplyTimeout();
            if (Objects.nonNull(replyTimeout)) {
                rabbitTemplate.setReplyTimeout(replyTimeout.toMillis());
            }
            String exchange = template.getExchange();
            if (StringUtils.isNotBlank(exchange)) {
                rabbitTemplate.setExchange(exchange);
            }
            String routingKey = template.getRoutingKey();
            if (StringUtils.isNotBlank(routingKey)) {
                rabbitTemplate.setRoutingKey(routingKey);
            }
            String defaultReceiveQueue = template.getDefaultReceiveQueue();
            if (StringUtils.isNotBlank(defaultReceiveQueue)) {
                rabbitTemplate.setDefaultReceiveQueue(defaultReceiveQueue);
            }
            ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(RabbitTemplate.class.getName() + i, rabbitTemplate);
            log.info("zlf.RabbitTemplate注册完成,beanName:{}", RabbitTemplate.class.getName() + i);
            // 不注册RabbitService
            RabbitService rabbitService = new RabbitService();
            //构建监听工厂实例并注入
            ContainerType type = rps.get(i).getListener().getType();
            if (ContainerType.SIMPLE.equals(type)) {
                Map<String, String> errorExchangeQueueRelationship = this.createErrorExchangeQueueRelationship(String.valueOf(i), rabbitService, rabbitAdmin);
                SimpleContainer simple = rps.get(i).getListener().getSimple();
                ConstructorArgumentValues cas3 = new ConstructorArgumentValues();
                MutablePropertyValues values3 = new MutablePropertyValues();
                this.getAmqpContainer(simple, values3, cachingConnectionFactory, jackson2JsonMessageConverter, rabbitTemplate, errorExchangeQueueRelationship);
                if (Objects.nonNull(simple.getConcurrency())) {
                    values3.add("concurrentConsumers", simple.getConcurrency());
                }
                if (Objects.nonNull(simple.getMaxConcurrency())) {
                    values3.add("maxConcurrentConsumers", simple.getMaxConcurrency());
                }
                if (Objects.nonNull(simple.getBatchSize())) {
                    values3.add("batchSize", simple.getBatchSize());
                }
                RootBeanDefinition rootBeanDefinition3 = new RootBeanDefinition(SimpleRabbitListenerContainerFactory.class, cas3, values3);
                beanDefinitionRegistry.registerBeanDefinition(SimpleRabbitListenerContainerFactory.class.getName() + i, rootBeanDefinition3);
                log.info("zlf.SimpleRabbitListenerContainerFactory注册完成,beanName:{}", SimpleRabbitListenerContainerFactory.class.getName() + i);
            } else if (ContainerType.DIRECT.equals(type)) {
                Map<String, String> errorExchangeQueueRelationship = this.createErrorExchangeQueueRelationship(String.valueOf(i), rabbitService, rabbitAdmin);
                DirectContainer direct = rps.get(i).getListener().getDirect();
                ConstructorArgumentValues cas4 = new ConstructorArgumentValues();
                MutablePropertyValues values4 = new MutablePropertyValues();
                this.getAmqpContainer(direct, values4, cachingConnectionFactory, jackson2JsonMessageConverter, rabbitTemplate, errorExchangeQueueRelationship);
                if (Objects.nonNull(direct.getConsumersPerQueue())) {
                    values4.add("consumersPerQueue", direct.getConsumersPerQueue());
                }
                RootBeanDefinition rootBeanDefinition4 = new RootBeanDefinition(DirectRabbitListenerContainerFactory.class, cas4, values4);
                beanDefinitionRegistry.registerBeanDefinition(DirectRabbitListenerContainerFactory.class.getName() + i, rootBeanDefinition4);
                log.info("zlf.DirectRabbitListenerContainerFactory注册完成,beanName:{}", DirectRabbitListenerContainerFactory.class.getName() + i);
            }
            //解析注册交换机、队列和绑定关系
            ExchangeQueueProperties exchangeQueueProperties = eqps.get(i);
            log.info("zlf.exchangeQueueProperties:{}", JSON.toJSONString(exchangeQueueProperties));
            Integer index = exchangeQueueProperties.getIndex();
            log.info("zlf.exchangeQueueProperties.index:{}", index);
            if (Objects.isNull(index)) {
                throw new RuntimeException("exchangeQueueProperties.index不为空");
            }
            if (Objects.nonNull(exchangeQueueProperties)) {
                log.info("zlf.exchangeQueueProperties:{}", JSON.toJSONString(exchangeQueueProperties));
                List<ExchangeQueueDto> eqs = exchangeQueueProperties.getEqs();
                if (CollectionUtil.isNotEmpty(eqs)) {
                    for (int k = 0; k < eqs.size(); k++) {
                        String bindingIndex = index.toString() + k;
                        log.info("zlf.bindingIndex:{}", bindingIndex);
                        ExchangeQueueDto exchangeQueueDto = eqs.get(k);
                        log.info("zlf.exchangeQueueDto:{}", JSON.toJSONString(exchangeQueueDto));
                        String functionType = exchangeQueueDto.getFunctionType();
                        log.info("zlf.functionType:{}", functionType);
                        if (FunctionTypeEnum.NORMAL.getFunctionType().equals(functionType)) {
                            this.createRelationship(FunctionTypeEnum.NORMAL, exchangeQueueDto, rabbitService, rabbitAdmin, bindingIndex, false);
                        } else if (FunctionTypeEnum.DELAY.getFunctionType().equals(functionType)) {
                            Integer delayType = exchangeQueueDto.getDelayType();
                            log.info("zlf.delayType:{}", delayType);
                            if (DelayTypeEnum.ONE.getDelayType().equals(delayType)) {
                                //延迟插件实现延迟队列
                                String exchangeType = exchangeQueueDto.getExchangeType();
                                if (!ExchangeTypeEnum.CUSTOM.getExchangeType().equals(exchangeType)) {
                                    throw new RuntimeException("延迟插件实现延迟队列交换机类型exchangeType必须定义为:custom");
                                }
                                this.createRelationship(FunctionTypeEnum.DELAY, exchangeQueueDto, rabbitService, rabbitAdmin, bindingIndex, false);
                            } else if (DelayTypeEnum.TWO.getDelayType().equals(delayType)) {
                                //TTL + 死信队列实现延迟队列
                                this.createRelationship(FunctionTypeEnum.DELAY, exchangeQueueDto, rabbitService, rabbitAdmin, bindingIndex, false);
                            } else if (DelayTypeEnum.THREE.getDelayType().equals(delayType)) {
                                //延迟交换机 + 消息设置setHeader("x-delay", xxx)
                                this.createRelationship(FunctionTypeEnum.DELAY, exchangeQueueDto, rabbitService, rabbitAdmin, bindingIndex, true);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查rabbitProperties配置的主要参数
     *
     * @param rabbitProperties
     */
    private void checkRabbitProperties(RabbitProperties rabbitProperties) {
        String virtualHost = rabbitProperties.getVirtualHost();
        if (StringUtils.isEmpty(virtualHost)) {
            throw new RuntimeException("RabbitProperties.virtualHost不为空");
        }
        String addresses = rabbitProperties.getAddresses();
        if (StringUtils.isEmpty(addresses)) {
            throw new RuntimeException("RabbitProperties.addresses不为空");
        }
        Integer port = rabbitProperties.getPort();
        if (Objects.isNull(port)) {
            throw new RuntimeException("RabbitProperties.port不为空");
        }
        String username = rabbitProperties.getUsername();
        if (StringUtils.isEmpty(username)) {
            throw new RuntimeException("RabbitProperties.username不为空");
        }
        String password = rabbitProperties.getPassword();
        if (StringUtils.isEmpty(password)) {
            throw new RuntimeException("RabbitProperties.password不为空");
        }
    }

    /**
     * 创建关系
     *
     * @param functionTypeEnum
     * @param exchangeQueueDto
     * @param rabbitService
     * @param rabbitAdmin
     * @param bindingIndex
     */
    private void createRelationship(FunctionTypeEnum functionTypeEnum, ExchangeQueueDto exchangeQueueDto, RabbitService rabbitService, RabbitAdmin rabbitAdmin, String bindingIndex, Boolean isDelayed) {
        String exchangeName = exchangeQueueDto.getExchangeName();
        String exchangeType = exchangeQueueDto.getExchangeType();
        HashMap<String, Object> exchangeArgs = exchangeQueueDto.getExchangeArgs();
        log.info("zlf" + functionTypeEnum.getFunctionType() + ".exchangeName:{},exchangeType:{},exchangeArgs:{}", exchangeName, exchangeType, JSON.toJSONString(exchangeArgs));
        AbstractExchange exchange1 = rabbitService.createExchange(rabbitAdmin, exchangeName, exchangeType, exchangeArgs, isDelayed);
        exchangeName = exchangeName + bindingIndex;
        ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(exchangeName, exchange1);
        log.info("zlf." + functionTypeEnum.getFunctionType() + ".Exchange注册完成,beanName:{}", exchangeName);
        String queueName = exchangeQueueDto.getQueueName();
        HashMap<String, Object> queueArgs = exchangeQueueDto.getQueueArgs();
        String routingKey1 = exchangeQueueDto.getRoutingKey();
        log.info("zlf." + functionTypeEnum.getFunctionType() + ".queueName:{},queueArgs:{},routingKey1:{}", queueName, JSON.toJSONString(queueArgs), routingKey1);
        Queue queue = rabbitService.createQueue(rabbitAdmin, queueName, queueArgs);
        queueName = queueName + bindingIndex;
        ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(queueName, queue);
        log.info("zlf." + functionTypeEnum.getFunctionType() + ".Queue注册完成,beanName:{}", queueName);
        Binding binding = rabbitService.binding(rabbitAdmin, exchange1, queue, routingKey1);
        ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(Binding.class.getName() + bindingIndex, binding);
        log.info("zlf." + functionTypeEnum.getFunctionType() + ".Binding注册完成bindingIndex:{},beanName:{}", bindingIndex, Binding.class.getName() + bindingIndex);
        Integer delayType = exchangeQueueDto.getDelayType();
        if (DelayTypeEnum.TWO.getDelayType().equals(delayType)) {
            String dlxExchangeName = exchangeQueueDto.getDlxExchangeName();
            if (StringUtils.isEmpty(dlxExchangeName)) {
                throw new RuntimeException("TTL + 死信队列实现延迟队列配置参数dlxExchangeName不为空!");
            }
            String dlxExchangeType = exchangeQueueDto.getDlxExchangeType();
            if (StringUtils.isEmpty(dlxExchangeType)) {
                throw new RuntimeException("TTL + 死信队列实现延迟队列配置参数dlxExchangeType不为空!");
            }
            AbstractExchange exchange2 = rabbitService.createExchange(rabbitAdmin, dlxExchangeName, dlxExchangeType, exchangeArgs, false);
            dlxExchangeName = dlxExchangeName + bindingIndex;
            ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(dlxExchangeName, exchange2);
            log.info("zlf.TTL + 死信队列实现延迟队列,死信交换机注册完成,beanName:{}", dlxExchangeName);
            String dlxQueueName = exchangeQueueDto.getDlxQueueName();
            Queue queue2 = rabbitService.createQueue(rabbitAdmin, dlxQueueName, null);
            dlxQueueName = dlxQueueName + bindingIndex;
            ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(dlxQueueName, queue2);
            log.info("zlf.TTL + 死信队列实现延迟队列,死信队列注册完成,beanName:{}", dlxQueueName);
            String dlxKey = exchangeQueueDto.getDlxKey();
            Binding binding2 = rabbitService.binding(rabbitAdmin, exchange2, queue2, dlxKey);
            String dlxBeanName = "dlx" + Binding.class.getName() + bindingIndex + 1;
            ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(dlxBeanName, binding2);
            log.info("zlf.TTL + 死信队列实现延迟队列,死信交换机绑定队列的绑定关系注册完成,beanName:{}", dlxBeanName);
        }
    }

    private Map<String, String> createErrorExchangeQueueRelationship(String index, RabbitService rabbitService, RabbitAdmin rabbitAdmin) {
        Map<String, String> resultMap = new HashMap<>();
        String exchangeName = ErrorExchangeQueueInfo.ERROR_EXCHANGE_PREFIX + index;
        AbstractExchange exchange = rabbitService.createExchange(rabbitAdmin, exchangeName, ErrorExchangeQueueInfo.ERROR_EXCHANGE_TYPE, null, false);
        ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(exchangeName, exchange);
        log.info("zlf.ErrorExchange注册完成,beanName:{}", exchangeName);
        String queueName = ErrorExchangeQueueInfo.ERROR_QUEUE_PREFIX + index;
        Queue queue = rabbitService.createQueue(rabbitAdmin, queueName, null);
        ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(queueName, queue);
        log.info("zlf.ErrorQueue注册完成,beanName:{}", queueName);
        String errorRoutingKey = ErrorExchangeQueueInfo.ERROR_KEY_PREFIX + index;
        log.info("zlf.errorRoutingKey:{}", errorRoutingKey);
        Binding errorBinding = rabbitService.binding(rabbitAdmin, exchange, queue, errorRoutingKey);
        String errorBingBeanName = ErrorExchangeQueueInfo.ERROR_BINDING_BANE_NAME_PREFIX + Binding.class.getSimpleName() + index;
        ((ConfigurableBeanFactory) this.beanFactory).registerSingleton(errorBingBeanName, errorBinding);
        log.info("zlf.ErrorBing注册完成,beanName:{}", errorBingBeanName);
        resultMap.put("errorExchange", exchangeName);
        resultMap.put("errorRoutingKey", errorRoutingKey);
        return resultMap;
    }

    private void getAmqpContainer(AmqpContainer amqpContainer, MutablePropertyValues values, CachingConnectionFactory cachingConnectionFactory, Jackson2JsonMessageConverter jackson2JsonMessageConverter, RabbitTemplate rabbitTemplate, Map<String, String> errorExchangeQueueRelationship) {
        values.add("connectionFactory", cachingConnectionFactory);
        values.add("autoStartup", amqpContainer.isAutoStartup());
        values.add("messageConverter", jackson2JsonMessageConverter);
        if (Objects.nonNull(amqpContainer.getAcknowledgeMode())) {
            values.add("acknowledgeMode", amqpContainer.getAcknowledgeMode());
        }
        if (Objects.nonNull(amqpContainer.getPrefetch())) {
            values.add("prefetch", amqpContainer.getPrefetch());
        }
        if (Objects.nonNull(amqpContainer.getDefaultRequeueRejected())) {
            values.add("defaultRequeueRejected", amqpContainer.getDefaultRequeueRejected());
        }
        if (Objects.nonNull(amqpContainer.getIdleEventInterval())) {
            values.add("idleEventInterval", amqpContainer.getIdleEventInterval());
        }
        values.add("missingQueuesFatal", amqpContainer.isMissingQueuesFatal());
        ListenerRetry retry2 = amqpContainer.getRetry();
        if (retry2.isEnabled()) {
            RetryInterceptorBuilder<?, ?> builder = (retry2.isStateless()) ? RetryInterceptorBuilder.stateless()
                    : RetryInterceptorBuilder.stateful();
            RetryTemplate retryTemplate = new RetryTemplate();
            SimpleRetryPolicy policy = new SimpleRetryPolicy();
            retryTemplate.setRetryPolicy(policy);
            policy.setMaxAttempts(retry2.getMaxAttempts());
            ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
            backOffPolicy.setMultiplier(retry2.getMultiplier());
            if (Objects.nonNull(retry2.getMaxInterval())) {
                backOffPolicy.setMaxInterval(retry2.getMaxInterval().toMillis());
            }
            builder.retryOperations(retryTemplate);
            /**
             * 在开启重试模式后，重试次数耗尽，如果消息依然失败，则需要有MessageRecovery接口来处理，它包含三种不同的实现：
             *
             * RejectAndDontRequeueRecoverer：重试耗尽后，直接reject，【丢弃消息】【默认】就是这种方式
             * ImmediateRequeueMessageRecoverer：重试耗尽后，返回nack，消息重新入队（Immediate立刻重入队）（但是频率比没有配置消费失败重载机制低一些）
             * RepublishMessageRecoverer（推荐）：重试耗尽后，将失败消息投递到指定的交换机
             */
            //消息接受拒绝后发送到异常队列
            String errorExchange = errorExchangeQueueRelationship.get("errorExchange");
            String errorRoutingKey = errorExchangeQueueRelationship.get("errorRoutingKey");
            MessageRecoverer recoverer = new RepublishMessageRecoverer(rabbitTemplate, errorExchange, errorRoutingKey);
            log.info("zlf.MessageRecoverer.errorExchange:{},errorRoutingKey:{}", errorExchange, errorRoutingKey);
            builder.recoverer(recoverer);
            values.add("adviceChain", builder.build());
        }
    }

    private RabbitConnectionFactoryBean getRabbitConnectionFactoryBean(RabbitProperties properties)
            throws Exception {
        PropertyMapper map = PropertyMapper.get();
        RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
        map.from(properties::determineHost).whenNonNull().to(factory::setHost);
        map.from(properties::determinePort).to(factory::setPort);
        map.from(properties::determineUsername).whenNonNull().to(factory::setUsername);
        map.from(properties::determinePassword).whenNonNull().to(factory::setPassword);
        map.from(properties::determineVirtualHost).whenNonNull().to(factory::setVirtualHost);
        map.from(properties::getRequestedHeartbeat).whenNonNull().asInt(Duration::getSeconds)
                .to(factory::setRequestedHeartbeat);
        map.from(properties::getRequestedChannelMax).to(factory::setRequestedChannelMax);
        RabbitProperties.Ssl ssl = properties.getSsl();
        if (ssl.determineEnabled()) {
            factory.setUseSSL(true);
            map.from(ssl::getAlgorithm).whenNonNull().to(factory::setSslAlgorithm);
            map.from(ssl::getKeyStoreType).to(factory::setKeyStoreType);
            map.from(ssl::getKeyStore).to(factory::setKeyStore);
            map.from(ssl::getKeyStorePassword).to(factory::setKeyStorePassphrase);
            map.from(ssl::getTrustStoreType).to(factory::setTrustStoreType);
            map.from(ssl::getTrustStore).to(factory::setTrustStore);
            map.from(ssl::getTrustStorePassword).to(factory::setTrustStorePassphrase);
            map.from(ssl::isValidateServerCertificate)
                    .to((validate) -> factory.setSkipServerCertificateValidation(!validate));
            map.from(ssl::getVerifyHostname).to(factory::setEnableHostnameVerification);
        }
        map.from(properties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
                .to(factory::setConnectionTimeout);
        factory.afterPropertiesSet();
        return factory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        // 通过Binder将environment中的值转成对象
        rabbitConfig = Binder.get(environment).bind(getPropertiesPrefix(RabbitConfig.class), RabbitConfig.class).get();
        exchangeQueueConfig = Binder.get(environment).bind(getPropertiesPrefix(ExchangeQueueConfig.class), ExchangeQueueConfig.class).get();
    }

    private String getPropertiesPrefix(Class<?> tClass) {
        return Objects.requireNonNull(AnnotationUtils.getAnnotation(tClass, ConfigurationProperties.class)).prefix();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
