package br.com.assine.billing.domain.port.in;

public interface ProcessWebhookUseCase {
    void process(String payload, String signature);
}
