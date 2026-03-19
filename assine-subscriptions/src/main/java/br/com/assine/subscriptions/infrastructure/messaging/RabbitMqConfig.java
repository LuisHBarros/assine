package br.com.assine.subscriptions.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "assine.events";
    public static final String PAYMENT_CONFIRMED_QUEUE = "assine.subscriptions.payment-confirmed";
    public static final String PAYMENT_FAILED_QUEUE = "assine.subscriptions.payment-failed";

    @Bean
    public TopicExchange assineExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue paymentConfirmedQueue() {
        return new Queue(PAYMENT_CONFIRMED_QUEUE);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE);
    }

    @Bean
    public Binding paymentConfirmedBinding(Queue paymentConfirmedQueue, TopicExchange assineExchange) {
        return BindingBuilder.bind(paymentConfirmedQueue)
            .to(assineExchange)
            .with("assine.payment.confirmed");
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue, TopicExchange assineExchange) {
        return BindingBuilder.bind(paymentFailedQueue)
            .to(assineExchange)
            .with("assine.payment.failed");
    }
}
