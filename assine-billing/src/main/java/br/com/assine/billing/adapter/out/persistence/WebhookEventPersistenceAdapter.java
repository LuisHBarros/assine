package br.com.assine.billing.adapter.out.persistence;

import br.com.assine.billing.domain.port.out.WebhookEventRepository;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventPersistenceAdapter implements WebhookEventRepository {

    private final ProcessedWebhookEventJpaRepository repository;

    public WebhookEventPersistenceAdapter(ProcessedWebhookEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(String eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public void save(String eventId) {
        repository.save(new ProcessedWebhookEventEntity(eventId));
    }
}
