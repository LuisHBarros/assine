package br.com.assine.fiscal.domain.port.out;

import java.util.UUID;

public interface FiscalGateway {
    FiscalResponse issue(PayerData payer, Integer amountCents, UUID paymentId);

    record FiscalResponse(
        String externalId,
        String series,
        String number,
        byte[] pdfContent,
        String rawResponse
    ) {}

    record PayerData(
        String name,
        String taxId, // CPF/CNPJ
        String email,
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state
    ) {}
}
