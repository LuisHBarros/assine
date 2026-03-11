# ADR-003: Política de Reembolso Progressivo

## Status
Aceito

## Contexto
O sistema Assine precisa definir uma política de reembolso para assinantes que solicitam cancelamento após o pagamento. O reembolso pode ser solicitado a qualquer momento dentro do ciclo de 30 dias. A política precisa ser justa para o usuário e sustentável para o negócio, com regras claras e calculáveis de forma determinística.

## Decisão
Adotar reembolso progressivo baseado em fração do período restante, com contador iniciando na data de ativação da assinatura.

Até o dia 14 (inclusive), o reembolso é integral. A partir do dia 15, o desconto é aplicado progressivamente até o dia 29. No dia 30 ou após, nenhum reembolso é concedido.

### Fórmula
diasDesdeAtivacao = hoje - subscriptionActivatedAt (em dias inteiros)

Se diasDesdeAtivacao <= 14: reembolso integral (100%)
Se diasDesdeAtivacao >= 30: sem reembolso (0%)
Se 15 <= diasDesdeAtivacao <= 29:
    diasRestantes  = 30 - diasDesdeAtivacao
    percentual     = diasRestantes / 15
    valorReembolso = valorPago * percentual (arredondado para baixo, em centavos)

O divisor é 15 (não 30) porque a janela progressiva vai do dia 15 ao dia 29 — 15 dias de decaimento.

### Exemplos
- Dia 0-14: reembolso de 100% (integral)
- Dia 15:   reembolso de 100% (15/15) — último dia integral
- Dia 16:   reembolso de 93%  (14/15)
- Dia 20:   reembolso de 67%  (10/15)
- Dia 25:   reembolso de 33%  (5/15)
- Dia 29:   reembolso de 7%   (1/15)
- Dia 30:   reembolso de 0%

### Arredondamento
O valor em centavos é sempre arredondado para baixo (floor) para evitar reembolsos acima do valor pago por erro de arredondamento. Exemplo: 67% de R$29,90 (2990 centavos) = floor(2003,3) = 2003 centavos.

## Implementação

### RefundCalculator (Value Object no domínio do assine-billing)
Classe RefundCalculator no pacote domain do assine-billing com um método único:

```
public RefundAmount calculate(
    int paidAmountCents,
    LocalDate activatedAt,
    LocalDate refundRequestedAt
)
```

O método calcula diasDesdeAtivacao como o número de dias inteiros entre activatedAt e refundRequestedAt (inclusive o dia da solicitação).

- Se diasDesdeAtivacao <= 14: retorna RefundAmount com 100% do valor pago
- Se diasDesdeAtivacao >= 30: retorna RefundAmount.zero()
- Se 15 <= diasDesdeAtivacao <= 29: aplica a fórmula progressiva com divisor 15 e retorna RefundAmount com valor em centavos (floor) e percentual aplicado

RefundCalculator não depende de nenhum framework — é Java puro, testável com JUnit sem Spring context.

### Fluxo de solicitação de reembolso
1. Admin ou usuário solicita reembolso via endpoint POST /refunds com body { subscriptionId, reason }
2. assine-billing busca o Payment confirmado da assinatura
3. assine-billing busca a data de ativação no assine-subscriptions via GET /subscriptions/{id}/activation-date
4. RefundCalculator.calculate() determina o valor a reembolsar
5. Se valorReembolso = 0: retorna 422 com mensagem explicando que o período de reembolso expirou
6. assine-billing chama gateway.refund(externalPaymentId, valorReembolso)
7. Persiste Refund com status PENDING na tabela refunds
8. Aguarda webhook de confirmação do gateway
9. Webhook chega: persiste status COMPLETED + publica PaymentRefunded via Outbox Pattern
10. assine-subscriptions consome PaymentRefunded: cancela assinatura imediatamente (sem esperar fim do período)
11. assine-access consome SubscriptionCanceled: revoga acesso imediatamente
12. assine-notifications envia email com valor reembolsado e percentual aplicado

### Tabela refunds (assine-billing)
```sql
CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    subscription_id UUID NOT NULL,
    external_id VARCHAR(255),
    requested_at TIMESTAMP NOT NULL,
    activated_at DATE NOT NULL,
    days_since_activation INTEGER NOT NULL,
    refund_percentage NUMERIC(5,2) NOT NULL,
    original_amount_cents INTEGER NOT NULL,
    refund_amount_cents INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    completed_at TIMESTAMP
);
```

days_since_activation e refund_percentage são persistidos intencionalmente para auditoria — mesmo que sejam deriváveis da fórmula, garantem rastreabilidade do cálculo exato aplicado em cada reembolso.

## Consequências

### Positivas
- Fórmula determinística: dado uma data de ativação e uma data de solicitação, o valor calculado é sempre o mesmo
- Primeiros 14 dias com reembolso integral reduzem fricção para novos assinantes — política justa e fácil de comunicar
- RefundCalculator é um value object puro, testável sem infraestrutura
- Política clara e comunicável ao usuário no momento da assinatura
- Auditoria completa: cada reembolso persiste o percentual e os dias calculados, não apenas o valor final

### Negativas
- Requer busca da data de ativação no assine-subscriptions durante o fluxo de reembolso (chamada HTTP síncrona justificada: é uma query pontual, não uma mutação de estado)
- Reembolsos de valor muito baixo (ex: 7% de R$9,90 = R$0,69) podem ser menores que as taxas do gateway — decisão operacional a avaliar

### Riscos e mitigações
- Risco: data de ativação incorreta gera cálculo errado
  Mitigação: activated_at é persistido na tabela refunds no momento da solicitação, criando registro imutável do cálculo
- Risco: webhook de confirmação do gateway não chega
  Mitigação: mesmo fluxo de Outbox Pattern dos demais pagamentos — o status permanece PENDING até a confirmação

## Alternativas rejeitadas

### Reembolso integral até X dias, zero após
Binário demais — penaliza usuários que pedem reembolso um dia fora da janela sem nenhuma proporcionalidade.

### Reembolso fixo por faixas (0-7 dias: 100%, 8-15: 50%, 16-30: 0%)
Cria cliff effects — o usuário no dia 15 recebe 50% mas no dia 16 recebe 0%. Injusto e difícil de defender.

### Decaimento progressivo desde o dia 0
Penaliza assinantes novos que experimentam o produto e decidem cancelar rapidamente. A janela de 14 dias integral é mais justa e alinhada com práticas de mercado.
