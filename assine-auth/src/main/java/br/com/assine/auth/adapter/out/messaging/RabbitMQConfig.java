package br.com.assine.auth.adapter.out.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "assine.auth";
    public static final String QUEUE = "auth.magic-link-requested.queue";
    public static final String ROUTING_KEY = "magic-link-requested";

    @Bean
    public DirectExchange authExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue magicLinkQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding magicLinkBinding(Queue magicLinkQueue, DirectExchange authExchange) {
        return BindingBuilder.bind(magicLinkQueue).to(authExchange).with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
