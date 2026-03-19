package br.com.assine.content.application.service;

import br.com.assine.content.domain.event.ContentReadyEvent;
import br.com.assine.content.domain.model.NewsletterContent;
import br.com.assine.content.domain.port.in.RetrieveContentUseCase;
import br.com.assine.content.domain.port.in.TriggerNewsletterRetryUseCase;
import br.com.assine.content.domain.port.out.ContentSourcePort;
import br.com.assine.content.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ContentService implements RetrieveContentUseCase, TriggerNewsletterRetryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ContentService.class);

    private final ContentSourcePort contentSourcePort;
    private final EventPublisherPort eventPublisherPort;

    public ContentService(ContentSourcePort contentSourcePort, EventPublisherPort eventPublisherPort) {
        this.contentSourcePort = contentSourcePort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public Optional<NewsletterContent> getTodayContent() {
        log.info("Retrieving today's content from source");
        return contentSourcePort.fetchTodayContent();
    }

    @Override
    public void triggerRetry() {
        log.info("Manual retry triggered for today's newsletter");
        processDailyNewsletter();
    }

    public void processDailyNewsletter() {
        String correlationId = Optional.ofNullable(MDC.get("correlationId"))
                .orElse(UUID.randomUUID().toString());
        MDC.put("correlationId", correlationId);

        try {
            log.info("Starting daily newsletter processing");
            contentSourcePort.fetchTodayContent().ifPresentOrElse(
                content -> {
                    ContentReadyEvent event = new ContentReadyEvent(
                        content.title(),
                        content.bodyHtml(),
                        correlationId
                    );
                    log.info("Content found. Publishing ContentReadyEvent for: {}", content.title());
                    eventPublisherPort.publish(event);
                },
                () -> log.warn("No ready content found for today. Skipping newsletter.")
            );
        } finally {
            MDC.remove("correlationId");
        }
    }
}
