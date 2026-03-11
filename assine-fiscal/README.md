# assine-fiscal

Serviço dedicado para emissão de NFS-e (Nota Fiscal de Serviço Eletrônica) usando integração com Nuvem Fiscal.

## Responsabilidade

- Consumir eventos de pagamento confirmado (`PaymentConfirmed`)
- Buscar dados do tomador no serviço de assinaturas
- Emitir notas fiscais via Nuvem Fiscal
- Gerenciar retry e resiliência via Outbox Pattern
- Publicar eventos de emissão (`InvoiceIssued`, `InvoiceFailed`)

## Arquitetura

Segue o padrão hexagonal (Ports & Adapters):

```
domain/
├── model/          # Invoice, InvoiceId, InvoiceStatus
├── event/          # InvoiceIssued, InvoiceFailed
└── port/           # Use cases e repositórios

application/
└── usecase/        # IssueInvoiceService

adapter/
├── in/messaging/   # PaymentConfirmedConsumer
├── out/persistence/# JPA repositories
├── out/messaging/  # InvoiceOutboxPublisher
└── out/fiscal/     # NuvemFiscalGatewayAdapter
```

## Configuração

Variáveis de ambiente:
- `SPRING_DATASOURCE_URL`: jdbc:postgresql://postgres-fiscal:5432/fiscal
- `SPRING_RABBITMQ_HOST`: rabbitmq
- `NUVEM_FISCAL_CLIENT_ID`: cliente da API Nuvem Fiscal
- `NUVEM_FISCAL_CLIENT_SECRET`: secret da API Nuvem Fiscal
- `NUVEM_FISCAL_ENVIRONMENT`: sandbox (produção em ambiente real)

## Tabelas

- `invoices`: notas fiscais emitidas
- `invoice_outbox`: resiliência para emissão com retry

Consulte `tables.md` para definições SQL completas.
