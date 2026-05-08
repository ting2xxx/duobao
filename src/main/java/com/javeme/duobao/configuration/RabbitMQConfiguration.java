package com.javeme.duobao.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
@Configuration
public class RabbitMQConfiguration {

    @Bean
    public Queue flashSaleOrderQueue() {
        return QueueBuilder.durable("flash.sale.order.queue")
                .withArgument("x-dead-letter-exchange", "flash.sale.order.dlx")
                .withArgument("x-dead-letter-routing-key", "error")
                .build();
    }
    @Bean
    public MessageConverter jsonMessageConvert() {
        return new Jackson2JsonMessageConverter();
    }

    //DLQ
    @Bean
    public Queue deadLetterQueue() {
        return new Queue("flash.sale.order.dlq");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("flash.sale.order.dlx");
    }

    //Connect DLQ to DLX
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("error");
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable("order.delay.queue")
                .withArgument("x-message-ttl", 60000)
                .withArgument("x-dead-letter-exchange", "order.release.exchange")
                .withArgument("x-dead-letter-routing-key", "release")
                .build();
    }

    @Bean
    public DirectExchange orderReleaseExchange() { return new DirectExchange("order.release.exchange"); }

    @Bean
    public Queue orderReleaseQueue() { return new Queue("order.release.queue"); }

    @Bean
    public Binding orderReleaseBinding() {
        return BindingBuilder.bind(orderReleaseQueue()).to(orderReleaseExchange()).with("release");
    }

}
