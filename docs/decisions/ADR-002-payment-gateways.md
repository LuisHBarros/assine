# ADR-002: Stripe como gateway de pagamento

## Status
Aceito

## Contexto
O sistema Assine precisa processar pagamentos recorrentes de assinaturas de newsletter. O público-alvo é brasileiro, o que exige suporte a cartão de crédito e Pix. As opções consideradas foram:

1. Stripe apenas
2. MercadoPago apenas
3. Stripe + MercadoPago com abstração via porta PaymentGateway

## Decisão
Adotar Stripe como gateway único, suportando cartão de crédito com recorrência nativa e Pix via integração nativa do Stripe para o Brasil. Boleto é removido do escopo.

A integração é isolada atrás da porta PaymentGateway no assine-billing. O domínio nunca referencia o Stripe diretamente — apenas a abstração. Essa decisão permite adicionar um segundo gateway no futuro implementando um novo adapter sem alterar o domínio.

## Motivação para adotar Stripe como gateway único

Stripe fornece uma solução robusta e madura para processamento de pagamentos:

- API robusta com webhooks granulares por tentativa de cobrança, permitindo uma régua de comunicação proativa com o usuário em cada etapa do processo de renovação
- Suporte nativo a Pix no Brasil como método de pagamento único, eliminando a necessidade de integração adicional
- SDK Java maduro e bem documentado, reduzindo o risco de problemas de integração e facilitando a manutenção
- Gateway único elimina duplicidade de webhooks, secrets e tratamento de casos edge, simplificando a arquitetura e reduzindo a superfície de manutenção

## Métodos de pagamento suportados

### Cartão de crédito
Recorrência nativa via Stripe Customer + Subscription. O Stripe cobra automaticamente a cada ciclo e notifica via webhooks granulares por tentativa: invoice.payment_succeeded, invoice.payment_failed, invoice.payment_action_required.

### Pix
Suportado nativamente pelo Stripe no Brasil como método de pagamento único. Sem recorrência nativa — o assine-billing mantém job scheduler mensal que gera nova cobrança Pix na data de renovação, envia o QR code ao usuário via assine-notifications e aguarda webhook de confirmação.

## Modelo de dados

Campos na tabela payments (assine-billing):
- payment_method VARCHAR(20): CREDIT_CARD | PIX

Campos na tabela subscriptions (assine-subscriptions):
- payment_method VARCHAR(20): CREDIT_CARD | PIX

## Porta PaymentGateway

Interface no domínio do assine-billing com as operações:
- createSubscription(CreateSubscriptionCommand): PaymentIntent
- cancelSubscription(externalSubscriptionId: String): void
- refund(externalPaymentId: String, amountCents: Integer): RefundResult
- parseWebhook(payload: String, signature: String): WebhookEvent

Implementação única: StripeGatewayAdapter.

## Consequências

### Positivas
- Gateway único elimina duplicidade de webhooks, secrets e casos edge
- API robusta com webhooks granulares por tentativa de cobrança
- Suporte nativo a Pix no Brasil sem necessidade de segundo gateway
- Retry configurável no dashboard do Stripe alinhado à régua do sistema
- SDK Java maduro e bem documentado

### Negativas
- Boleto removido do escopo — usuários sem cartão ou Pix não são atendidos
- Pix ainda exige job scheduler de renovação mensal no assine-billing
- Concentração em fornecedor único — risco mitigado pela abstração via porta PaymentGateway

### Riscos e mitigações
- Risco: Stripe indisponível impede novos pagamentos
  Mitigação: a porta PaymentGateway permite adicionar gateway secundário no futuro sem alterar o domínio
- Risco: mudança de pricing ou política do Stripe
  Mitigação: isolamento via adapter — troca de gateway é implementação de novo adapter sem impacto no domínio ou nos demais serviços

## Alternativas rejeitadas

### MercadoPago apenas
API menos madura para cartão de crédito internacional. Recorrência nativa limitada comparada ao Stripe. Risco de concentração em um único fornecedor.

### Stripe + MercadoPago
Complexidade operacional desproporcional ao benefício. Dois gateways aumentam a superfície de integração e manutenção. Gateway único com suporte nativo a Pix simplifica a arquitetura.
