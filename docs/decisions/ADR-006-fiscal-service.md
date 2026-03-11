# ADR-006: Serviço Dedicado de Emissão de NFS-e com Nuvem Fiscal

## Status
Aceito

## Contexto
No Brasil, toda prestação de serviço paga exige emissão de NFS-e (Nota Fiscal de Serviço Eletrônica). A nota é municipal — cada prefeitura tem seu próprio sistema e webservice. A emissão precisa ser automatizada após cada pagamento confirmado, mas tem ciclo de vida, legislação e possibilidade de falha independentes do pagamento.

As opções consideradas foram:
1. Responsabilidade do assine-billing (adapter adicional)
2. Serviço dedicado assine-fiscal com integração via emissor
3. Integração direta com a prefeitura (webservice municipal)
4. Emissão manual pelo admin

## Decisão
Criar o serviço dedicado assine-fiscal, responsável exclusivamente pela emissão, consulta e cancelamento de NFS-e. A integração com a prefeitura é abstraída pela Nuvem Fiscal, uma plataforma de emissão fiscal com API REST moderna e plano gratuito em ambiente de homologação.

O assine-fiscal consome o evento PaymentConfirmed do RabbitMQ, busca os dados do tomador no assine-subscriptions via HTTP síncrono, emite a nota via Nuvem Fiscal e publica InvoiceIssued ou InvoiceFailed.

## Motivação para serviço dedicado

NFS-e tem legislação própria e ciclo de vida distinto do pagamento: uma nota pode ser cancelada mesmo após emitida, sem cancelar o pagamento. Falha na emissão não deve impactar o pagamento já confirmado nem a ativação da assinatura — isolamento de falha justifica serviço separado. Legislação fiscal muda com frequência — isolar em serviço dedicado limita o escopo de mudanças futuras. O assine-billing já tem responsabilidade clara (integração com gateway de pagamento) — adicionar fiscal aumentaria acoplamento sem benefício.

## Motivação para Nuvem Fiscal

Plano gratuito em ambiente de homologação (sandbox) — adequado para projeto de portfolio que não será publicado em produção. API REST moderna — sem necessidade de SOAP, XML manual ou certificado digital gerenciado pela aplicação. Suporte a múltiplos municípios — a complexidade de cada prefeitura é absorvida pela plataforma. SDK Java disponível no Maven Central. Em caso de migração para produção, apenas as credenciais mudam — a variável NUVEM_FISCAL_ENVIRONMENT controla sandbox vs produção.

## Porta FiscalGateway

Interface no domínio do assine-fiscal com três operações:
- issue(IssueInvoiceCommand): InvoiceResult
- query(externalId: String): InvoiceStatus
- cancel(externalId: String): void

Implementação: NuvemFiscalGatewayAdapter.

A abstração via porta permite substituir a Nuvem Fiscal por outro emissor no futuro sem alterar o domínio.

## Storage de PDFs

Após a emissão da NFS-e, o PDF gerado pela Nuvem Fiscal é armazenado no MinIO (ADR-009). O assine-fiscal utiliza a porta InvoiceStorageGateway para abstrair o storage — a implementação atual é MinIOStorageAdapter, substituível por qualquer solução S3-compatível em produção.

O campo pdf_url na tabela invoices armazena a URL interna do MinIO (http://minio:9000/invoices/{invoiceId}.pdf) e não a URL temporária da Nuvem Fiscal, garantindo acesso permanente ao documento fiscal.

O assine-notifications acessa o PDF via InvoiceStorageGateway ao processar o evento InvoiceIssued, baixando o conteúdo para anexar ao email enviado ao assinante.

## Dados do tomador

O evento PaymentConfirmed não carrega dados pessoais do assinante. O assine-fiscal busca nome, CPF/CNPJ e endereço via HTTP síncrono no assine-subscriptions (GET /subscriptions/{userId}/fiscal-data) antes de emitir a nota. Essa chamada é justificada por ser uma query pontual de leitura, não uma mutação de estado.

## Idempotência na emissão

A tabela invoices possui índice único em payment_id — o mesmo pagamento nunca gera duas notas, mesmo que o consumer seja chamado mais de uma vez.

## Resiliência via Outbox de emissão

A tabela invoice_outbox persiste a intenção de emitir antes de chamar a Nuvem Fiscal. Um job de retry com backoff exponencial tenta reemitir notas com status PENDING. Após 5 tentativas sem sucesso, o status é marcado como FAILED e o evento InvoiceFailed é publicado, notificando o admin via assine-notifications.

## Tabelas (assine-fiscal)

```sql
CREATE TABLE invoices (
    id                UUID PRIMARY KEY,
    payment_id        UUID NOT NULL,
    subscription_id   UUID NOT NULL,
    user_id           UUID NOT NULL,
    external_id       VARCHAR(255),
    series            VARCHAR(10),
    number            VARCHAR(20),
    amount_cents      INTEGER NOT NULL,
    status            VARCHAR(20) NOT NULL,
    issuer_response   JSONB,
    pdf_url           VARCHAR(500),
    issued_at         TIMESTAMP,
    created_at        TIMESTAMP NOT NULL,
    UNIQUE (payment_id)
);

CREATE TABLE invoice_outbox (
    id                UUID PRIMARY KEY,
    payment_id        UUID NOT NULL UNIQUE,
    payload           JSONB NOT NULL,
    attempts          INTEGER NOT NULL DEFAULT 0,
    last_attempt_at   TIMESTAMP,
    issued            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP NOT NULL
);

CREATE INDEX idx_invoice_outbox_unpublished ON invoice_outbox(issued) WHERE issued = FALSE;
```

## Mapa de eventos

assine-fiscal consome:
- PaymentConfirmed → dispara emissão da NFS-e

assine-fiscal publica:
- InvoiceIssued → assine-notifications envia PDF por email ao assinante
- InvoiceFailed → assine-notifications envia alerta ao admin

## Consequências

### Positivas
- Falha na emissão fiscal não afeta pagamento nem ativação da assinatura
- Nuvem Fiscal abstrai complexidade municipal — sem SOAP, sem certificado digital gerenciado pela aplicação
- Plano gratuito em sandbox adequado para portfolio sem custo operacional
- Porta FiscalGateway permite troca de emissor sem impacto no domínio
- Idempotência via índice único em payment_id previne notas duplicadas

### Negativas
- Novo serviço adiciona complexidade operacional: banco dedicado, consumer dedicado, job de retry dedicado
- Busca de dados do tomador via HTTP síncrono cria dependência pontual com assine-subscriptions durante a emissão
- Em produção, exigiria plano pago na Nuvem Fiscal e configuração de certificado digital

### Riscos e mitigações
- Risco: Nuvem Fiscal indisponível no momento da emissão
  Mitigação: invoice_outbox com retry e backoff exponencial — a emissão é tentada novamente sem intervenção manual
- Risco: dados do tomador indisponíveis no assine-subscriptions
  Mitigação: falha registrada no invoice_outbox, retry tentará novamente quando o serviço estiver disponível
- Risco: mesma nota emitida duas vezes por retry
  Mitigação: índice único em payment_id impede duplicidade

## Alternativas rejeitadas

### Responsabilidade do assine-billing
Aumentaria o acoplamento do billing com legislação fiscal. Falhas fiscais afetariam o serviço de pagamento. Ciclos de vida distintos justificam separação.

### Integração direta com a prefeitura
Exige SOAP, XML, certificado digital A1/A3 e tratamento específico por município. Complexidade desproporcional para o objetivo do projeto.

### Emissão manual pelo admin
Não escala e não demonstra automação. Inadequado para portfolio técnico focado em arquitetura de sistemas.
