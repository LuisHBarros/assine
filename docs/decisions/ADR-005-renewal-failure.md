# ADR-005: Tratamento de Falha na Renovação

## Status
Aceito

## Contexto
O sistema Assine suporta dois métodos de pagamento via Stripe: cartão de crédito (recorrência nativa) e Pix (cobrança manual por ciclo). Cada método tem comportamento distinto em caso de falha na renovação, exigindo réguas de cobrança separadas.

## Decisão
Adotar réguas de cobrança distintas por método de pagamento, ambas orquestradas pelo assine-billing.

## Régua para Cartão de Crédito

O Stripe tenta a cobrança automaticamente e emite webhook por tentativa. O número de tentativas e o intervalo são configurados no dashboard do Stripe (Smart Retries). O assine-billing reage a cada webhook:

- invoice.payment_failed: marca assinatura como PAST_DUE, envia email "Problema com seu pagamento. Verifique seu cartão."
- invoice.payment_action_required: envia email com link de autenticação 3DS. Sem tratar esse caso o usuário fica preso em PAST_DUE sem saber o motivo.
- customer.subscription.deleted: Stripe esgotou as tentativas e cancelou. assine-billing publica PaymentFailed definitivo. Assinatura cancelada, acesso revogado, email de cancelamento enviado.

Régua do sistema alinhada à configuração do Stripe:
- Dia 0: cobrança falha → PAST_DUE, email de aviso
- Dia 3: Stripe retenta → se falhar, novo email
- Dia 7: Stripe retenta → se falhar, acesso suspenso
- Dia 10: Stripe desiste → cancelamento definitivo

## Régua para Pix

Pix não tem recorrência nativa no Stripe. O assine-billing mantém um job scheduler que roda mensalmente na data de renovação de cada assinatura com payment_method=PIX:

Job mensal de renovação Pix:
1. Busca assinaturas ativas com payment_method=PIX e current_period_end = hoje
2. Gera nova cobrança Pix via Stripe
3. Persiste Payment com status PENDING
4. assine-notifications envia email com QR code e prazo de pagamento

Régua após geração da cobrança:
- Dia 0: QR code enviado por email
- Dia 3: lembrete por email se ainda PENDING
- Dia 5: status → PAST_DUE, acesso suspenso, email de aviso
- Dia 10: cancelamento definitivo, evento SubscriptionCanceled publicado

O job de renovação é idempotente: verifica se já existe Payment PENDING para o ciclo atual antes de gerar nova cobrança.

## Webhooks relevantes

Cartão:
- invoice.payment_succeeded → PaymentConfirmed, assinatura reativada
- invoice.payment_failed → PAST_DUE
- invoice.payment_action_required → email com link 3DS
- customer.subscription.deleted → cancelamento definitivo

Pix:
- payment_intent.succeeded → PaymentConfirmed, assinatura renovada
- payment_intent.payment_failed → incrementa contador de falha

## Consequências

### Positivas
- Stripe fornece webhooks granulares por tentativa para cartão — régua de comunicação proativa com o usuário em cada etapa
- Tratamento de 3DS evita falhas silenciosas por autenticação pendente
- Job de renovação Pix é idempotente — seguro para reexecução em caso de falha do scheduler

### Negativas
- Réguas distintas por método de pagamento aumentam superfície de código e testes no assine-billing
- Job scheduler de Pix adiciona complexidade operacional — necessário monitorar execução via métrica pix.renewal.failed

### Riscos e mitigações
- Risco: job de renovação Pix falha para um subconjunto de assinaturas
  Mitigação: job idempotente pode ser reexecutado manualmente. Métrica pix.renewal.failed no Prometheus alerta falhas
- Risco: usuário não autentica o 3DS e fica em PAST_DUE indefinidamente
  Mitigação: régua de retry do Stripe cancela após N tentativas, gerando customer.subscription.deleted e encerrando o ciclo

## Alternativas rejeitadas

### Tratar cartão e Pix com a mesma régua
Impossível — cartão tem retry automático pelo Stripe com webhooks granulares. Pix exige geração manual de cobrança. Comportamentos fundamentalmente distintos exigem tratamento distinto.

### Ignorar invoice.payment_action_required
Deixa usuários presos em PAST_DUE por falha de autenticação 3DS sem comunicação. Comum em cartões europeus e cada vez mais em cartões brasileiros com 3DS habilitado.
