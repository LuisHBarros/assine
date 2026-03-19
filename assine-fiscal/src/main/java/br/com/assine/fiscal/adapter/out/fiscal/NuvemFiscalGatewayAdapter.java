package br.com.assine.fiscal.adapter.out.fiscal;

import br.com.assine.fiscal.domain.port.out.FiscalGateway;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NuvemFiscalGatewayAdapter implements FiscalGateway {

    private static final Logger log = LoggerFactory.getLogger(NuvemFiscalGatewayAdapter.class);

    @Override
    public FiscalResponse issue(PayerData payer, Integer amountCents, UUID paymentId) {
        log.info("Issuing invoice via Nuvem Fiscal for paymentId: {}", paymentId);

        // In a real implementation, this would call the Nuvem Fiscal API using a Feign Client or RestTemplate.
        // For this prototype/exercise, we will simulate a successful response.

        String externalId = UUID.randomUUID().toString();
        String series = "1";
        String number = String.valueOf(System.currentTimeMillis()).substring(7);
        byte[] pdfContent = "SIMULATED PDF CONTENT".getBytes();
        String rawResponse = "{\"status\":\"success\", \"id\":\"" + externalId + "\"}";

        return new FiscalResponse(externalId, series, number, pdfContent, rawResponse);
    }
}
