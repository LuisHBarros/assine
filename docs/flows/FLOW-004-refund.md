# FLOW-004: Fluxo de Reembolso

## Visão Geral
O reembolso no Assine é self-service — solicitado pelo próprio usuário via endpoint autenticado. O reembolso implica cancelamento imediato e automático da assinatura e revogação do acesso. O valor reembolsado segue a política progressiva definida no ADR-003: integral até o dia 14, progressivo do dia 15 ao 29, e zero a partir do dia 30. O contador começa na data de ativação da assinatura.

## Diagrama do Fluxo

```
Usuário solicita reembolso
  → POST /refunds { subscriptionId, reason? }
  → assine-gateway valida JWT, propaga X-User-Id

assine-billing:
  → valida ownership da Subscription
  → valida Payment CONFIRMED existente
  → GET /subscriptions/{id}/activation-date → assine-subscriptions
  → RefundCalculator.calculate()
  → dia >= 30: retorna 422 REFUND_PERIOD_EXPIRED
  → valor > 0: chama Stripe → processa reembolso
  → persiste Refund (status: PENDING)
  → publica via Outbox: SubscriptionRefundRequested

assine-subscriptions consome SubscriptionRefundRequested:
  → status: ACTIVE → CANCELED
  → cancela recorrência no Stripe
  → publica SubscriptionCanceled

assine-access consome SubscriptionCanceled:
  → revoga AccessPermission (revoked_at = now())

[webhook Stripe: charge.refund.updated status=succeeded]
assine-billing:
  → atualiza Refund (status: COMPLETED)
  → publica PaymentRefunded

assine-notifications consome:
  SubscriptionCanceled    → email "Reembolso solicitado, acesso encerrado"
  PaymentRefunded         → email "Reembolso de R${valor} (X%) confirmado"
```

## Passo a Passo

### 1. Usuário solicita reembolso
- POST /refunds
  body: { subscriptionId, reason? }
  header: Authorization: Bearer {token}
- assine-gateway valida JWT e propaga X-User-Id e X-User-Role
- Rate limiting: 5 requisições por hora por userId (ADR-008)

### 2. Validações no assine-billing
Execute as validações na seguinte ordem, retornando erro imediato em cada falha:

- Valida que a Subscription pertence ao X-User-Id recebido
  → se não pertence: 403 FORBIDDEN
- Valida que a Subscription tem status ACTIVE
  → se não está ACTIVE: 422 SUBSCRIPTION_NOT_ACTIVE
- Valida que existe Payment com status CONFIRMED para a Subscription
  → se não existe: 422 NO_CONFIRMED_PAYMENT
- Valida que não existe Refund PENDING ou COMPLETED para a Subscription
  → se existe: 409 REFUND_ALREADY_EXISTS

### 3. Cálculo do reembolso
- assine-billing busca activated_at via GET /subscriptions/{subscriptionId}/activation-date no assine-subscriptions (chamada HTTP síncrona — query pontual)
- Chama RefundCalculator.calculate(paidAmountCents, activatedAt, hoje)
- Se diasDesdeAtivacao >= 30:
  → retorna 422 com body:
    {
      "code": "REFUND_PERIOD_EXPIRED",
      "message": "O período de reembolso de 30 dias expirou.",
      "activatedAt": "{data}",
      "daysSinceActivation": 30
    }
- Se diasDesdeAtivacao <= 14: reembolso integral (100%)
- Se 15 <= diasDesdeAtivacao <= 29: reembolso progressivo
  percentual = (30 - diasDesdeAtivacao) / 15
  valorReembolso = floor(paidAmountCents * percentual)

### 4. Processamento do reembolso no Stripe
- assine-billing chama Stripe Refunds API com:
  { paymentIntentId: externalId, amount: refundAmountCents }
- Persiste Refund na tabela refunds:
  { status: PENDING, activated_at, days_since_activation, refund_percentage, original_amount_cents, refund_amount_cents }
- Transação atômica via Outbox Pattern:
  INSERT INTO processed_refund_requests (subscriptionId) — idempotência
  INSERT INTO refunds (status: PENDING)
  INSERT INTO outbox_events (SubscriptionRefundRequested)
- Retorna 202 ACCEPTED ao frontend com:
  {
    "refundId": "{uuid}",
    "refundAmountCents": {valor},
    "refundPercentage": {percentual},
    "status": "PENDING",
    "estimatedDays": "5-10 dias úteis"
  }

### 5. Cancelamento da assinatura
- assine-subscriptions consome SubscriptionRefundRequested
- Atualiza status ACTIVE → CANCELED
- Chama Stripe para cancelar recorrência imediatamente
- Define canceled_at = now()
- Publica SubscriptionCanceled
- assine-access consome SubscriptionCanceled:
  → revoga AccessPermission (revoked_at = now())
- assine-notifications consome SubscriptionCanceled:
  → envia email:
    "Seu reembolso de R${valor} (${percentual}%) foi solicitado. Seu acesso foi encerrado. O valor será creditado em 5-10 dias úteis."

### 6. Confirmação do reembolso via webhook
- Webhook Stripe chega: charge.refund.updated com status=succeeded
- assine-billing valida HMAC e idempotência (processed_webhook_events)
- Atualiza Refund: status PENDING → COMPLETED, completed_at = now()
- Publica PaymentRefunded com { refundId, subscriptionId, userId, refundAmountCents, refundPercentage }
- assine-notifications consome PaymentRefunded:
  → envia email:
    "Seu reembolso de R${valor} foi confirmado e será creditado no seu cartão/conta em até 5-10 dias úteis dependendo do seu banco."

## Mapa de estados neste fluxo

Subscription:
- ACTIVE → CANCELED (ao consumir SubscriptionRefundRequested)

Refund:
- PENDING → COMPLETED (ao receber webhook de confirmação)
- PENDING → FAILED (ao receber webhook de falha)

AccessPermission:
- ativa → revogada (revoked_at preenchido)

## Comportamentos de Falha

1. assine-subscriptions indisponível ao buscar activation-date
   → assine-billing retorna 503 ao frontend
   → nenhum estado é alterado — usuário pode tentar novamente
   → Refund não é criado

2. Stripe indisponível ao processar reembolso
   → assine-billing retorna 503 ao frontend
   → nenhum estado é alterado — Outbox não é gravado
   → usuário pode tentar novamente

3. Webhook de confirmação não chega
   → Refund permanece PENDING indefinidamente
   → Job diário consulta o status do reembolso diretamente na Stripe API via GET /v1/refunds/{externalId}
   → Atualiza status localmente se confirmado

4. Webhook indica falha no reembolso (charge.refund.updated status=failed)
   → Refund atualizado para FAILED
   → assine-billing publica RefundFailed
   → assine-notifications envia email ao usuário informando a falha e orientando a entrar em contato com o suporte
   → Subscription permanece CANCELED — acesso não é restaurado automaticamente (requer intervenção manual do admin)

## Decisões de Design

1. Por que o reembolso cancela a assinatura automaticamente sem dar opção ao usuário?
   Justificativa: reembolso e continuidade de acesso são mutuamente exclusivos — não faz sentido devolver o valor e manter o acesso ao período pago. A simplicidade da regra evita estados ambíguos e é fácil de comunicar ao usuário.

2. Por que o assine-billing publica SubscriptionRefundRequested em vez de chamar o assine-subscriptions diretamente via HTTP?
   Justificativa: mantém a regra de comunicação assíncrona entre bounded contexts. O cancelamento da assinatura é uma consequência do reembolso, não uma etapa síncrona obrigatória. Se o assine-subscriptions estiver temporariamente indisponível, o reembolso já foi iniciado no Stripe e o cancelamento ocorrerá quando o serviço se recuperar.

3. Por que retornar 202 ACCEPTED em vez de 200 OK?
   Justificativa: o reembolso não é processado sincronamente — ele depende de confirmação via webhook do Stripe. O 202 comunica ao frontend que a solicitação foi aceita e está em processamento, sem prometer conclusão imediata.

4. Por que persistir days_since_activation e refund_percentage na tabela refunds se são deriváveis?
   Justificativa: auditoria imutável. O cálculo é determinístico, mas persistir os valores garante rastreabilidade exata do que foi calculado em cada reembolso, independente de mudanças futuras na política ou na data do sistema.
