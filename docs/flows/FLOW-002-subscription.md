# FLOW-002: Fluxo de Assinatura

## Visão Geral
Descreva que este é o fluxo central do sistema Assine, cobrindo desde
o cadastro do usuário até a liberação do acesso à newsletter. Envolve
seis serviços: assine-auth, assine-subscriptions, assine-billing,
assine-access, assine-fiscal e assine-notifications. O fluxo diverge
por método de pagamento (cartão de crédito ou Pix) após a criação da
assinatura, convergindo novamente no processamento do webhook de
confirmação.

## Diagrama do Fluxo

Inclua um diagrama em formato de texto mostrando os dois caminhos:

Usuário acessa o sistema
  → [não autenticado] assine-auth (magic link ou Google OAuth2)
  → JWT emitido

Usuário escolhe o plano
  → assine-gateway → assine-subscriptions
  → Subscription criada (status: PENDING)
  → evento: SubscriptionCreated

Usuário escolhe método de pagamento:

[Cartão de crédito]                   [Pix]
assine-billing                         assine-billing
→ Stripe cria PaymentIntent            → Stripe gera QR code
→ retorna client_secret                → retorna QR code + expiresAt
→ frontend confirma no Stripe          → frontend exibe QR code
→ banco pode exigir 3DS                → usuário paga no app do banco
→ webhook: invoice.payment_succeeded   → webhook: payment_intent.succeeded

[Convergência]
assine-billing:
  → Outbox: persiste Payment + publica PaymentConfirmed

assine-subscriptions consome PaymentConfirmed:
  → PENDING → ACTIVE
  → publica SubscriptionActivated

assine-access consome SubscriptionActivated:
  → cria AccessPermission

assine-fiscal consome PaymentConfirmed:
  → emite NFS-e → publica InvoiceIssued

assine-notifications consome:
  SubscriptionCreated   → email com QR code (Pix) ou "processando" (cartão)
  SubscriptionActivated → email "Acesso liberado"
  InvoiceIssued         → email com PDF da nota fiscal

## Passo a Passo

### 1. Autenticação
- Usuário não autenticado é direcionado para assine-auth
- Escolhe entre magic link ou Google OAuth2
- Após autenticação: JWT emitido com role=USER
- JWT propagado em todas as requisições via header Authorization: Bearer {token}

### 2. Escolha do plano
- GET /subscriptions/plans — lista planos disponíveis (público)
- Usuário seleciona o plano desejado
- POST /subscriptions com body { planId, paymentMethod: CREDIT_CARD | PIX }
- assine-subscriptions cria Subscription com status PENDING
- Persiste payment_method e define current_period_end como nulo (ainda não pago)
- Publica SubscriptionCreated
- assine-notifications consome SubscriptionCreated:
  se paymentMethod=PIX: aguarda QR code para incluir no email
  se paymentMethod=CREDIT_CARD: envia email "Estamos processando seu pagamento"

### 3a. Pagamento via Cartão de Crédito
- POST /billing/payment-intent { subscriptionId }
- assine-billing cria PaymentIntent no Stripe
- Retorna { clientSecret } para o frontend
- Frontend usa Stripe.js para confirmar o pagamento com o clientSecret
- Se banco exige 3DS:
  → Stripe retorna status requires_action com redirectUrl
  → Frontend redireciona usuário para autenticação no banco
  → Usuário autentica e retorna ao sistema
  → Stripe processa e envia webhook invoice.payment_succeeded
- Se não exige 3DS:
  → Stripe processa diretamente
  → Webhook invoice.payment_succeeded chega no assine-billing

### 3b. Pagamento via Pix
- POST /billing/pix { subscriptionId }
- assine-billing cria PaymentIntent Pix no Stripe
- Retorna { qrCode, qrCodeUrl, expiresAt } para o frontend
- Frontend exibe QR code ao usuário
- assine-notifications envia email com QR code e prazo de pagamento
- Usuário paga no app do banco
- Webhook payment_intent.succeeded chega no assine-billing
- QR code expira em 24 horas se não utilizado:
  → Job diário verifica Subscriptions PENDING com QR code expirado
  → Cancela a Subscription (status: CANCELED)
  → Publica SubscriptionCanceled
  → assine-notifications envia email "Seu QR code expirou. Tente novamente."

### 4. Processamento do webhook (cartão e Pix)
- assine-billing recebe webhook do Stripe
- Valida assinatura HMAC-SHA256 via StripeWebhookFilter
- Verifica idempotência em processed_webhook_events
- Transação atômica via Outbox Pattern:
  INSERT INTO processed_webhook_events
  INSERT INTO payments (status: CONFIRMED)
  INSERT INTO outbox_events (PaymentConfirmed)
- Retorna 200 ao Stripe

### 5. Ativação da assinatura
- assine-subscriptions consome PaymentConfirmed
- Transição de estado: PENDING → ACTIVE
- Define current_period_end = now() + 30 dias
- Publica SubscriptionActivated
- assine-notifications consome SubscriptionActivated:
  envia email "Seu acesso à newsletter está ativo"

### 6. Liberação do acesso
- assine-access consome SubscriptionActivated
- Cria AccessPermission:
  { userId, resource: "newsletter", subscriptionId, expires_at: current_period_end }
- A partir desse momento GET /access/{userId} retorna acesso permitido

### 7. Emissão da nota fiscal
- assine-fiscal consome PaymentConfirmed
- Busca dados do tomador via GET /subscriptions/{userId}/fiscal-data
- Persiste intenção de emissão no invoice_outbox
- Chama Nuvem Fiscal para emitir NFS-e
- Persiste Invoice com status ISSUED
- Publica InvoiceIssued
- assine-notifications consome InvoiceIssued:
  envia email com PDF da nota fiscal em anexo

## Comportamentos de Falha

Descreva em prosa os cinco cenários de falha e o comportamento
esperado em cada um:

1. Stripe indisponível no momento do pagamento
   → assine-billing retorna 503 para o frontend
   → Subscription permanece PENDING
   → usuário pode tentar novamente

2. Webhook de confirmação não chega
   → Subscription permanece PENDING indefinidamente
   → Job diário detecta Subscriptions PENDING sem Payment confirmado
     há mais de 24 horas e cancela automaticamente

3. Banco exige 3DS e usuário abandona a autenticação
   → PaymentIntent expira no Stripe (tempo configurável)
   → Stripe envia webhook payment_intent.payment_failed
   → assine-billing publica PaymentFailed
   → assine-subscriptions marca como PAST_DUE
   → assine-notifications envia email de falha

4. QR code Pix expira sem pagamento
   → Job diário cancela a Subscription
   → assine-notifications envia email com instrução para nova tentativa

5. Falha na emissão da NFS-e
   → Pagamento já confirmado e acesso já liberado — não são afetados
   → invoice_outbox retenta com backoff exponencial
   → Após 5 tentativas: InvoiceFailed publicado, admin notificado

## Decisões de Design

Explique em prosa as três decisões que podem ser questionadas
em entrevista:

1. Por que a Subscription é criada com status PENDING antes do pagamento?
   Justificativa: separa a intenção de assinar da confirmação do
   pagamento. Permite rastrear usuários que iniciaram mas não
   concluíram a assinatura, e mantém o estado consistente em caso
   de falha entre os passos.

2. Por que o assine-fiscal consome PaymentConfirmed e não
   SubscriptionActivated?
   Justificativa: a NFS-e deve ser emitida quando o pagamento é
   confirmado, não quando a assinatura é ativada. São eventos que
   ocorrem em sequência, mas a obrigação fiscal está vinculada ao
   pagamento, não ao acesso.

3. Por que o frontend confirma o pagamento de cartão via Stripe.js
   em vez do backend fazer isso?
   Justificativa: PCI DSS. Dados do cartão nunca trafegam pelo
   backend do Assine — o Stripe.js envia diretamente para os
   servidores do Stripe. O backend só lida com o clientSecret,
   que não é dado sensível.
