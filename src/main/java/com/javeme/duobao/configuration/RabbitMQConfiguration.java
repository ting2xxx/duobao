package com.javeme.duobao.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
@Configuration
public class RabbitMQConfiguration {

    /**
     * Flash Sale Order Queue
     * build a queue with rules if a message fails repeatedly and dies, don't delete it
     * Send it to "dlx" exchange with routing key "error"
     * then it will go to dlq for inspection
     *
     * 1.Normal Order -> Main Queue -> MySQL (Success)
     *
     * 2.Crash/Error -> Main Queue -> DLX -> DLQ
     *
     * 3.DLQ -> Message sleeps safely until an Admin investigates it.
     * @return
     */
    @Bean
    public Queue flashSaleOrderQueue() {
        return QueueBuilder.durable("flash.sale.order.queue")
                .withArgument("x-dead-letter-exchange", "flash.sale.order.dlx")
                .withArgument("x-dead-letter-routing-key", "error")
                .build();
    }
    //To translate java objects into JSON before send to RabbitMQ
    @Bean
    public MessageConverter jsonMessageConvert() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * If order fails, it will stay in this queue so you can inspect later without losing data.
     * @return
     */
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
    //bind dlq to dlx, so when error happen, it will route to dl
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("error");
    }

    /**
     * Order auto cancel after 15min, delayed queue
     * 1. Send orderId to normal queue
     * 2. Wait for 15minutes
     * 3. Because no listener is picking the message,rabbitMQ will see the message expired
     * 4. So it will look for exchange, and then it will route to release queue
     * 5. A Listener will grab the same message with orderId and see whether user is paid
     * 6. Based on what the database replies, it either leaves it alone (if paid) or cancels it and returns the stock (if unpaid).
     * @return
     */

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable("order.delay.queue")
                .withArgument("x-message-ttl", 60000)//60 seconds for testing
                .withArgument("x-dead-letter-exchange", "order.release.exchange")
                .withArgument("x-dead-letter-routing-key", "release")
                .build();
    }

    //when message dies here, drop it through into exchange
    @Bean
    public DirectExchange orderReleaseExchange() { return new DirectExchange("order.release.exchange"); }

    //after exchange, ti will land to this release queue
    @Bean
    public Queue orderReleaseQueue() { return new Queue("order.release.queue"); }

    @Bean
    public Binding orderReleaseBinding() {
        return BindingBuilder.bind(orderReleaseQueue()).to(orderReleaseExchange()).with("release");
    }

    @Bean
    public Queue productEvictQueue() {
        return QueueBuilder.durable("product.cache.eviction.queue")
                .withArgument("x-dead-letter-exchange", "product.cache.eviction.dlx")
                .withArgument("x-dead-letter-routing-key", "error")
                .build();
    }

    @Bean
    public DirectExchange productEvictDlx() {
        return new DirectExchange("product.cache.eviction.dlx");
    }

    @Bean
    public Queue productEvictDlq() {
        return new Queue("product.cache.eviction.dlq");
    }

    @Bean
    public Binding productEvictBinding() {
        return BindingBuilder.bind(productEvictDlq()).to(productEvictDlx()).with("error");
    }
}
