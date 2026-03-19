package br.com.assine.content.domain.event;

public record ContentReadyEvent(
    String title,
    String bodyHtml,
    String correlationId
) {
}
