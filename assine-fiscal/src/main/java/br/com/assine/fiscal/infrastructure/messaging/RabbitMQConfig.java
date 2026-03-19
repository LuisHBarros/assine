package br.com.assine.fiscal.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_CONFIRMED_QUEUE = "assine.fiscal.payment-confirmed";
    public static final String PAYMENT_EXCHANGE = "assine.payment";
    public static final String PAYMENT_CONFIRMED_ROUTING_KEY = "assine.payment.confirmed";

    public static final String INVOICE_EXCHANGE = "assine.invoice";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue paymentConfirmedQueue() {
        return QueueBuilder.durable(PAYMENT_CONFIRMED_QUEUE).build();
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Binding paymentConfirmedBinding(Queue paymentConfirmedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentConfirmedQueue).to(paymentExchange).with(PAYMENT_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange invoiceExchange() {
        return new TopicExchange(INVOICE_EXCHANGE);
    }
}
