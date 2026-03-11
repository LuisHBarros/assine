# ADR-004: Tratamento de Chargebacks

## Status
Aceito

## Contexto
Chargeback ocorre quando o usuário contesta uma cobrança diretamente no banco emissor, sem passar pelo sistema Assine. O gateway debita o valor da conta do negócio e abre uma disputa. É diferente de um reembolso voluntário — o sistema é notificado depois que o estorno já aconteceu, sem controle prévio.

## Decisão
Tratar chargeback como cancelamento imediato e incondicional da assinatura, sem período de graça. Ao contrário do cancelamento voluntário, que respeita o fim do período pago, o chargeback indica contestação ativa da cobrança — manter o acesso seria inconsistente com o estorno já realizado.

## Webhook do Stripe

O Stripe notifica via:
- charge.dispute.created: disputa aberta, valor debitado
- charge.dispute.updated: atualização do status da disputa
- charge.dispute.closed: disputa resolvida (won ou lost)

O mesmo controle de idempotência via processed_webhook_events se aplica.

## Fluxo

1. Webhook charge.dispute.created chega no assine-billing
2. Validação HMAC-SHA256 via StripeWebhookFilter
3. Verificação de idempotência em processed_webhook_events
4. Transação atômica via Outbox Pattern:
   INSERT INTO processed_webhook_events
   INSERT INTO chargebacks (status=OPENED)
   INSERT INTO outbox_events (evento: ChargebackOpened)
5. assine-subscriptions consome ChargebackOpened: cancela assinatura imediatamente (status=CANCELED, sem período de graça)
6. assine-access consome SubscriptionCanceled: revoga acesso imediatamente (revoked_at = now())
7. assine-notifications envia email:
   "Identificamos uma contestação no seu pagamento. Seu acesso foi suspenso. Entre em contato para mais informações."
8. Quando charge.dispute.closed chega: UPDATE chargebacks SET status = WON ou LOST, resolved_at = now()
   Nenhuma reativação automática — reativação exige ação manual

## Tabela chargebacks (assine-billing)

```sql
CREATE TABLE chargebacks (
    id              UUID PRIMARY KEY,
    payment_id      UUID NOT NULL REFERENCES payments(id),
    subscription_id UUID NOT NULL,
    external_id     VARCHAR(255) NOT NULL,
    amount_cents    INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL,
    opened_at       TIMESTAMP NOT NULL,
    resolved_at     TIMESTAMP
);
```

## Consequências

### Positivas
- Comportamento consistente: contestação da cobrança implica perda imediata do acesso, sem ambiguidade
- Mesmo pipeline de Outbox Pattern dos demais eventos de pagamento — sem tratamento especial na infraestrutura
- Registro completo do ciclo de vida da disputa (OPENED → WON/LOST)

### Negativas
- Sem reativação automática em caso de disputa ganha (WON) — requer processo manual ou política explícita a definir no futuro
- Usuário pode contestar por engano e perder acesso imediatamente

### Riscos e mitigações
- Risco: chargeback duplicado pelo Stripe
  Mitigação: idempotência via processed_webhook_events
- Risco: disputa ganha mas acesso não reativado
  Mitigação: status WON registrado na tabela chargebacks permite identificar casos elegíveis para reativação manual

## Alternativas rejeitadas

### Manter acesso durante a disputa
Inconsistente com o estorno já realizado. O valor já foi debitado da conta do negócio — manter o acesso representaria prejuízo duplo.

### Reativar automaticamente se disputa for ganha
Aumenta complexidade sem garantia de que o usuário merece reativação. A decisão de reativar deve ser consciente e manual.
