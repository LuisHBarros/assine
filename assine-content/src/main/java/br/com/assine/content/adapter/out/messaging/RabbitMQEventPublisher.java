package br.com.assine.content.adapter.out.messaging;

import br.com.assine.content.domain.event.ContentReadyEvent;
import br.com.assine.content.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitMQEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange:assine.events}") String exchange,
            @Value("${app.rabbitmq.routing-key:assine.content.ready}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publish(ContentReadyEvent event) {
        log.info("Publishing ContentReadyEvent to exchange {} with routing key {}: {}", exchange, routingKey, event.title());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
