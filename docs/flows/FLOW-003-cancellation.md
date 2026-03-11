# FLOW-003: Fluxo de Cancelamento

## Visão Geral
O cancelamento no Assine possui três caminhos distintos: cancelamento voluntário pelo usuário (com duas sub-opções: fim do período ou imediato com reembolso), cancelamento por inadimplência (coberto no ADR-005), e cancelamento por chargeback (coberto no ADR-004). Este documento cobre exclusivamente o cancelamento voluntário.

O fluxo tem uma regra de negócio especial: se o usuário cancelou imediatamente mas não solicitou reembolso, e tenta se inscrever novamente dentro do período já pago, o sistema detecta o período ativo e apresenta duas opções ao usuário antes de prosseguir.

## Diagrama do Fluxo

```
Usuário solicita cancelamento
  → POST /subscriptions/me/cancel { option: END_OF_PERIOD | IMMEDIATE }

[Opção A: END_OF_PERIOD]
assine-subscriptions:
  → status: ACTIVE → CANCELED_AT_PERIOD_END
  → cancela recorrência no Stripe (cancel_at_period_end: true)
  → publica SubscriptionCancellationScheduled
assine-notifications:
  → email "Assinatura cancelada. Acesso até {current_period_end}"

[No dia current_period_end — job diário]
assine-subscriptions:
  → status: CANCELED_AT_PERIOD_END → CANCELED
  → publica SubscriptionCanceled
assine-access:
  → revoga AccessPermission (revoked_at = now())
assine-notifications:
  → email "Seu acesso foi encerrado"

[Opção B: IMMEDIATE]
assine-subscriptions:
  → status: ACTIVE → CANCELED
  → cancela recorrência no Stripe imediatamente
  → publica SubscriptionCanceled
assine-access:
  → revoga AccessPermission imediatamente
assine-billing:
  → calcula reembolso via RefundCalculator (ADR-003)
  → chama Stripe para processar reembolso
  → persiste Refund (status: PENDING)
  → aguarda webhook de confirmação
  → webhook chega: publica PaymentRefunded
assine-notifications:
  → email "Assinatura cancelada"
  → email "Reembolso de R${valor} processado" (após webhook)

[Tentativa de nova assinatura com período ativo]
Usuário cancelou com IMMEDIATE mas não solicitou reembolso
  → tenta POST /subscriptions
  → assine-subscriptions detecta Subscription CANCELED
    com current_period_end > hoje
  → retorna 409 com body:
    {
      "code": "ACTIVE_PERIOD_EXISTS",
      "message": "Você ainda tem acesso até {current_period_end}.",
      "options": ["REACTIVATE", "REFUND_AND_RESUBSCRIBE"]
    }
  → usuário escolhe via POST /subscriptions/me/reactivate
    { option: REACTIVATE | REFUND_AND_RESUBSCRIBE }

[REACTIVATE]
assine-subscriptions:
  → status: CANCELED → ACTIVE
  → recria recorrência no Stripe a partir de current_period_end
  → publica SubscriptionReactivated
assine-access:
  → restaura AccessPermission (revoked_at = null)
assine-notifications:
  → email "Assinatura reativada. Próxima renovação em {current_period_end}"

[REFUND_AND_RESUBSCRIBE]
assine-billing:
  → calcula reembolso via RefundCalculator (ADR-003)
  → processa reembolso no Stripe
  → aguarda webhook de confirmação
  → publica PaymentRefunded
assine-subscriptions consome PaymentRefunded:
  → status: CANCELED → PENDING (pronto para nova assinatura)
assine-notifications:
  → email "Reembolso de R${valor} processado. Você pode assinar novamente."
```

## Passo a Passo

### 1. Usuário solicita cancelamento
- POST /subscriptions/me/cancel
  body: { option: END_OF_PERIOD | IMMEDIATE }
- assine-gateway valida JWT e propaga X-User-Id
- assine-subscriptions valida que a Subscription pertence ao userId
- assine-subscriptions valida que o status atual é ACTIVE
- Se status não for ACTIVE: retorna 422 "Assinatura não está ativa"

### 2a. Cancelamento no fim do período (END_OF_PERIOD)
- assine-subscriptions atualiza status para CANCELED_AT_PERIOD_END
- Chama Stripe: atualiza Subscription com cancel_at_period_end=true
  — o Stripe não cobra no próximo ciclo mas mantém ativa até o fim
- Publica SubscriptionCancellationScheduled com { subscriptionId, userId, cancelAt: current_period_end }
- assine-notifications envia email informando data do encerramento
- Job diário no assine-subscriptions verifica Subscriptions com status CANCELED_AT_PERIOD_END e current_period_end <= hoje:
  → atualiza status para CANCELED
  → publica SubscriptionCanceled
  → assine-access revoga AccessPermission
  → assine-notifications envia email de encerramento

### 2b. Cancelamento imediato (IMMEDIATE)
- assine-subscriptions atualiza status para CANCELED
- Chama Stripe: cancela Subscription imediatamente
- Publica SubscriptionCanceled
- assine-access consome SubscriptionCanceled:
  → revoga AccessPermission (revoked_at = now())
- assine-billing consome SubscriptionCanceled:
  → busca Payment confirmado da assinatura
  → chama RefundCalculator.calculate() com activated_at e hoje
  → se refundAmount = 0: não processa reembolso, apenas loga
  → se refundAmount > 0: chama Stripe para processar reembolso
  → persiste Refund com status PENDING
  → aguarda webhook stripe: charge.refund.updated status=succeeded
  → publica PaymentRefunded
- assine-notifications consome:
  SubscriptionCanceled → email de cancelamento com valor do reembolso
  PaymentRefunded      → email de confirmação do reembolso

### 3. Tentativa de nova assinatura com período ativo
- Usuário que cancelou com IMMEDIATE tenta POST /subscriptions
- assine-subscriptions verifica se existe Subscription CANCELED com current_period_end > hoje para o mesmo userId
- Se existe: retorna 409 ACTIVE_PERIOD_EXISTS com as duas opções
- Usuário escolhe via POST /subscriptions/me/reactivate
  body: { option: REACTIVATE | REFUND_AND_RESUBSCRIBE }

### 4a. Reativação (REACTIVATE)
- assine-subscriptions atualiza status CANCELED → ACTIVE
- Recria recorrência no Stripe a partir de current_period_end
  — sem nova cobrança imediata, próxima cobrança no fim do período
- Publica SubscriptionReactivated
- assine-access consome SubscriptionReactivated:
  → restaura AccessPermission (revoked_at = null)
- assine-notifications envia email confirmando reativação e informando data da próxima renovação

### 4b. Reembolso e nova assinatura (REFUND_AND_RESUBSCRIBE)
- assine-billing calcula e processa reembolso (mesmo fluxo do 2b)
- Após PaymentRefunded: assine-subscriptions marca status como PENDING
- assine-notifications envia email informando que pode assinar novamente
- Usuário inicia novo fluxo de assinatura (FLOW-002)

## Mapa de estados da Subscription neste fluxo

- ACTIVE → CANCELED_AT_PERIOD_END (opção END_OF_PERIOD)
- CANCELED_AT_PERIOD_END → CANCELED (job diário no fim do período)
- ACTIVE → CANCELED (opção IMMEDIATE)
- CANCELED → ACTIVE (reativação)
- CANCELED → PENDING (reembolso e nova assinatura)

## Comportamentos de Falha

1. Stripe indisponível ao cancelar recorrência
   → assine-subscriptions atualiza o status localmente e retorna sucesso ao usuário. Um job de reconciliação diário verifica Subscriptions CANCELED com recorrência ainda ativa no Stripe e tenta cancelar novamente.

2. Webhook de confirmação do reembolso não chega
   → Refund permanece PENDING. Job diário consulta o status do reembolso diretamente na API do Stripe e atualiza localmente.

3. Usuário tenta cancelar assinatura já cancelada
   → assine-subscriptions retorna 422 com mensagem "Assinatura não está ativa"

4. RefundCalculator retorna zero (dia 30 ou além)
   → assine-billing não processa reembolso. Email de cancelamento enviado sem menção a reembolso.

## Decisões de Design

1. Por que o status CANCELED_AT_PERIOD_END em vez de manter ACTIVE com uma flag de cancelamento agendado?
   Justificativa: status explícito torna o estado da assinatura inequívoco em qualquer consulta. Uma flag booleana adicional cria estados implícitos que aumentam a complexidade das queries e da lógica de negócio.

2. Por que o assine-billing consome SubscriptionCanceled para processar o reembolso em vez de o assine-subscriptions chamar o billing diretamente?
   Justificativa: mantém a regra de comunicação assíncrona entre bounded contexts. O assine-subscriptions não precisa saber que existe um serviço de billing — ele apenas publica o estado. O billing reage de forma independente.

3. Por que retornar 409 com opções em vez de bloquear a nova assinatura completamente?
   Justificativa: o usuário pagou por um período que ainda está vigente. Bloquear sem explicação seria confuso. Apresentar as opções dá transparência e evita que o usuário pague duas vezes pelo mesmo período sem perceber.
