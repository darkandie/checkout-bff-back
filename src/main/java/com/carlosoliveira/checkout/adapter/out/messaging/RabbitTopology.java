package com.carlosoliveira.checkout.adapter.out.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "checkout.events", name = "publisher", havingValue = "rabbit")
public class RabbitTopology {

    public static final String EXCHANGE = "checkout.events";
    public static final String PAYMENT_APPROVED_QUEUE = "checkout.payment-approved";
    public static final String PAYMENT_DECLINED_QUEUE = "checkout.payment-declined";
    public static final String PAYMENT_APPROVED_ROUTING_KEY = "payment.approved";
    public static final String PAYMENT_DECLINED_ROUTING_KEY = "payment.declined";

    @Bean
    TopicExchange checkoutEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue paymentApprovedQueue() {
        return QueueBuilder.durable(PAYMENT_APPROVED_QUEUE).build();
    }

    @Bean
    Queue paymentDeclinedQueue() {
        return QueueBuilder.durable(PAYMENT_DECLINED_QUEUE).build();
    }

    @Bean
    Binding paymentApprovedBinding(TopicExchange checkoutEventsExchange, Queue paymentApprovedQueue) {
        return BindingBuilder.bind(paymentApprovedQueue).to(checkoutEventsExchange).with(PAYMENT_APPROVED_ROUTING_KEY);
    }

    @Bean
    Binding paymentDeclinedBinding(TopicExchange checkoutEventsExchange, Queue paymentDeclinedQueue) {
        return BindingBuilder.bind(paymentDeclinedQueue).to(checkoutEventsExchange).with(PAYMENT_DECLINED_ROUTING_KEY);
    }
}
