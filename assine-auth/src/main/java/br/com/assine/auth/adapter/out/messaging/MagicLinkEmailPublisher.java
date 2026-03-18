package br.com.assine.auth.adapter.out.messaging;

import br.com.assine.auth.domain.port.out.MagicLinkEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MagicLinkEmailPublisher implements MagicLinkEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "assine.auth";
    public static final String ROUTING_KEY = "magic-link-requested";

    public MagicLinkEmailPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishRequestedEvent(String email, String token) {
        Map<String, String> event = Map.of(
                "email", email,
                "token", token
        );

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
