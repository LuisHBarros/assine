package br.com.assine.fiscal.domain.port.out;

import java.util.UUID;

public interface InvoiceStorageGateway {
    String store(UUID invoiceId, byte[] pdfContent);
}
