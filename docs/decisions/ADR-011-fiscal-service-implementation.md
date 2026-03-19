# ADR-011: Implementação Técnica do Serviço Assine Fiscal

## Status
Aceito

## Contexto
O serviço `assine-fiscal` foi implementado para automatizar a emissão de NFS-e seguindo as diretrizes do ADR-006 e ADR-009. Esta decisão documenta os detalhes técnicos da implementação, refinando as escolhas de stack e padrões de resiliência.

## Decisão

### 1. Arquitetura Hexagonal (Ports & Adapters)
A estrutura segue rigorosamente o padrão hexagonal para isolar a lógica de negócio (domínio) de detalhes de infraestrutura (gateways, bancos, mensageria).
- **Domain**: Modelos (`Invoice`, `InvoiceOutbox`), Eventos (`InvoiceIssued`, `InvoiceFailed`) e Portas.
- **Application**: Casos de uso (`IssueInvoiceService`, `InvoiceRetryJob`).
- **Adapter In**: Mensageria (`PaymentConfirmedConsumer`).
- **Adapter Out**: Persistência (JPA), Fiscal (Nuvem Fiscal), Storage (AWS SDK S3), Subscription (Feign).

### 2. Padrão Outbox para Resiliência
Para garantir que toda confirmação de pagamento resulte em uma tentativa de emissão, mesmo em caso de falhas temporárias na API da Nuvem Fiscal ou no MinIO:
- O consumer apenas registra a intenção na tabela `invoice_outbox`.
- Um job agendado (`InvoiceRetryJob`) processa os registros pendentes.
- Implementado **Backoff Exponencial** (inicial: 1s, multiplicador: 2x) com limite de **5 tentativas**.
- Após exaustão de retries, a nota é marcada como `FAILED` e um evento de erro é publicado.

### 3. Integração com MinIO via AWS SDK v2
Diferente do sugerido inicialmente no ADR-009 (SDK nativo do MinIO), optou-se pelo **AWS SDK for Java v2 (software.amazon.awssdk:s3)**.
- **Motivação**: O AWS SDK é o padrão de mercado para integrações S3. O MinIO é 100% compatível. Usar o AWS SDK facilita a migração para a AWS (ou outros providers como GCS) apenas alterando o endpoint, sem trocar a biblioteca.
- **Configuração**: Uso de `S3Client` com `endpointOverride`.

### 4. Comunicação Síncrona via Spring Cloud OpenFeign
Para buscar dados do tomador no `assine-subscriptions`, foi utilizado o OpenFeign.
- **Motivação**: Simplicidade e integração nativa com o ecossistema Spring Boot.
- **Resiliência**: A falha na chamada HTTP é capturada pelo `InvoiceRetryJob`, que agenda uma nova tentativa automaticamente via Outbox.

### 5. Idempotência
Garantida por um índice único na coluna `payment_id` tanto na tabela `invoices` quanto na `invoice_outbox`. Isso evita a emissão de notas duplicadas para o mesmo pagamento, mesmo em cenários de reprocessamento de mensagens pelo RabbitMQ.

## Consequências

### Positivas
- Alta portabilidade entre provedores de cloud (S3-compatible).
- Tolerância a falhas robusta em integrações externas críticas.
- Baixo acoplamento entre serviços.

### Negativas
- Aumento da latência percebida para a emissão da nota (devido ao processamento assíncrono via job), o que é aceitável para processos fiscais.
