package br.com.assine.billing.domain.port.out;

public interface WebhookEventRepository {
    boolean exists(String eventId);
    void save(String eventId);
}
