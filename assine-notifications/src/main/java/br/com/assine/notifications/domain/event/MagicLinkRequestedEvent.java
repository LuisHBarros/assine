package br.com.assine.notifications.domain.event;

public record MagicLinkRequestedEvent(
    String email,
    String token,
    String correlationId
) {}
