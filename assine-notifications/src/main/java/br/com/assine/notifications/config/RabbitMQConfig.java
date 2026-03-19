package br.com.assine.notifications.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "assine.events";
    public static final String QUEUE_NAME = "notifications.q";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange assineEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding paymentConfirmedBinding(Queue notificationsQueue, TopicExchange assineEventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(assineEventsExchange).with("assine.payment.confirmed");
    }

    @Bean
    public Binding subscriptionBinding(Queue notificationsQueue, TopicExchange assineEventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(assineEventsExchange).with("assine.subscription.*");
    }

    @Bean
    public Binding invoiceIssuedBinding(Queue notificationsQueue, TopicExchange assineEventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(assineEventsExchange).with("assine.invoice.issued");
    }

    @Bean
    public Binding invoiceFailedBinding(Queue notificationsQueue, TopicExchange assineEventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(assineEventsExchange).with("assine.invoice.failed");
    }

    @Bean
    public Binding authMagicLinkBinding(Queue notificationsQueue, TopicExchange assineEventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(assineEventsExchange).with("assine.auth.magic-link-requested");
    }

    @Bean
    public DirectExchange authExchange() {
        return new DirectExchange("assine.auth");
    }

    @Bean
    public Binding authMagicLinkDirectBinding(Queue notificationsQueue, DirectExchange authExchange) {
        return BindingBuilder.bind(notificationsQueue).to(authExchange).with("magic-link-requested");
    }
}
