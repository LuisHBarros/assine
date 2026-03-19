package br.com.assine.access.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "assine.events";
    public static final String SUBSCRIPTION_ACTIVATED_QUEUE = "assine.access.subscription-activated";
    public static final String SUBSCRIPTION_CANCELED_QUEUE = "assine.access.subscription-canceled";

    @Bean
    public TopicExchange assineExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue subscriptionActivatedQueue() {
        return new Queue(SUBSCRIPTION_ACTIVATED_QUEUE);
    }

    @Bean
    public Queue subscriptionCanceledQueue() {
        return new Queue(SUBSCRIPTION_CANCELED_QUEUE);
    }

    @Bean
    public Binding subscriptionActivatedBinding(Queue subscriptionActivatedQueue, TopicExchange assineExchange) {
        return BindingBuilder.bind(subscriptionActivatedQueue)
            .to(assineExchange)
            .with("assine.subscription.activated");
    }

    @Bean
    public Binding subscriptionCanceledBinding(Queue subscriptionCanceledQueue, TopicExchange assineExchange) {
        return BindingBuilder.bind(subscriptionCanceledQueue)
            .to(assineExchange)
            .with("assine.subscription.canceled");
    }
}
