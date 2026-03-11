# Agent Guidelines for assine-fiscal

## Service Overview
Dedicated service for NFS-e (Nota Fiscal de Serviço Eletrônica) issuance using Nuvem Fiscal integration. Listens to PaymentConfirmed events, fetches payer data, issues invoices, and publishes InvoiceIssued/InvoiceFailed events.

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
mvn flyway:migrate
```

## Service Structure
- **domain/**: Invoice logic
  - model/: Invoice (aggregate root), InvoiceId, InvoiceStatus
  - event/: InvoiceIssued, InvoiceFailed
  - port/in/: IssueInvoiceUseCase
  - port/out/: InvoiceRepository, InvoiceOutboxRepository, FiscalGateway, InvoiceStorageGateway, DomainEventPublisher
- **application/usecase/**: IssueInvoiceService
- **adapter/**:
  - in/messaging/: PaymentConfirmedConsumer
  - out/persistence/: JPA repositories for invoices and invoice_outbox
  - out/messaging/: InvoiceOutboxPublisher
  - out/fiscal/: NuvemFiscalGatewayAdapter
  - out/storage/: MinIOStorageAdapter
  - out/storage/: MinIOStorageAdapter

## Event Consumption & Publishing
Consumes from RabbitMQ:
- `assine.payment.confirmed`

Publishes to RabbitMQ:
- `assine.invoice.issued`
- `assine.invoice.failed`

## Configuration
- Database: `jdbc:postgresql://postgres-fiscal:5432/fiscal`
- Port: 8085
- RabbitMQ: consumes payment events
- Nuvem Fiscal: client_id, client_secret, environment (sandbox)
- MinIO: endpoint, access_key, secret_key, bucket (invoices)

## Key Constraints
- Fiscal legislation is separate from payment logic — isolated service
- Outbox pattern with retry (backoff exponential, 5 attempts) for issuance
- Unique index on payment_id prevents duplicate invoices
- Payer data fetched via HTTP sync from assine-subscriptions (query, not mutation)
- PDFs stored in MinIO (S3-compatible object storage)
- InvoiceStorageGateway abstraction allows S3 migration (AWS S3, GCS, Azure Blob)
- Structured JSON logging with correlation ID
