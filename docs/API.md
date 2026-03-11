# API.md: Contrato da API do Assine

## Visão Geral

Todos os endpoints são expostos pelo assine-gateway na porta 8080 com prefixo /api/v1. Endpoints internos (comunicação entre serviços) não passam pelo gateway e são documentados separadamente. A API usa JSON em todas as requisições e respostas. Autenticação via Bearer token JWT no header Authorization, exceto nos endpoints públicos listados explicitamente.

## Formato de Erros (RFC 7807)

Todas as respostas de erro seguem o padrão RFC 7807 com Content-Type: application/problem+json.

Estrutura padrão:
```json
{
  "type": "https://assine.com/problems/subscription-not-active",
  "title": "Assinatura não está ativa",
  "status": 422,
  "detail": "A assinatura informada não possui status ACTIVE.",
  "instance": "/api/v1/subscriptions/me/cancel",
  "correlationId": "abc-123-def"
}
```

Campos:
- type: URI única que identifica o tipo do problema
- title: descrição curta legível por humanos
- status: HTTP status code
- detail: descrição específica do problema nesta ocorrência
- instance: endpoint que gerou o erro
- correlationId: X-Correlation-ID da requisição para rastreabilidade

Códigos de erro padronizados do sistema:
- 400 BAD_REQUEST: corpo da requisição inválido ou campos obrigatórios ausentes
- 401 UNAUTHORIZED: token ausente, inválido ou expirado
- 403 FORBIDDEN: token válido mas sem permissão para o recurso
- 404 NOT_FOUND: recurso não encontrado
- 409 CONFLICT: conflito de estado (ex: ACTIVE_PERIOD_EXISTS)
- 422 UNPROCESSABLE_ENTITY: regra de negócio violada
- 429 TOO_MANY_REQUESTS: rate limit atingido
- 503 SERVICE_UNAVAILABLE: serviço downstream indisponível

---

## assine-auth

### POST /api/v1/auth/magic-link

Público. Solicita envio de magic link para o email informado. Rate limit: 3 req/min por IP.

Request:
```json
{
  "email": "user@email.com"
}
```

Response 200:
```json
{
  "message": "Se o email existir, você receberá um link em instantes."
}
```

Explicar que a resposta é sempre 200 independente do email existir ou não — evita enumeração de usuários.

Erros possíveis: 400, 429

---

### GET /api/v1/auth/magic-link/validate?token={token}

Público. Valida o magic link e emite JWT.

Response 200:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 3600
}
```

Erros possíveis: 401 (token inválido, expirado ou já utilizado)

---

### GET /api/v1/auth/oauth2/google

Público. Redireciona para o fluxo de autenticação do Google.
Response: 302 redirect para accounts.google.com

---

### GET /api/v1/auth/oauth2/google/callback

Público. Callback do Google OAuth2. Emite JWT após autenticação.

Response 200: mesmo formato do magic-link/validate

Erros possíveis: 401 (autenticação Google falhou), 503 (Google indisponível)

---

### POST /api/v1/auth/refresh

Público. Renova o accessToken usando o refreshToken.

Request:
```json
{
  "refreshToken": "eyJ..."
}
```

Response 200: mesmo formato do magic-link/validate

Erros possíveis: 401 (refreshToken inválido ou expirado)

---

### GET /api/v1/auth/.well-known/jwks.json

Público. Retorna a chave pública RSA usada para verificar JWTs. Consumido pelo assine-gateway para validação local de tokens.

Response 200:
```json
{
  "keys": [{
    "kty": "RSA",
    "use": "sig",
    "kid": "assine-2025",
    "n": "...",
    "e": "AQAB"
  }]
}
```

---

## assine-subscriptions

### GET /api/v1/subscriptions/plans

Público. Lista os planos disponíveis.

Response 200:
```json
{
  "plans": [
    {
      "id": "uuid",
      "name": "Mensal",
      "priceCents": 2990,
      "interval": "MONTHLY",
      "active": true
    }
  ]
}
```

---

### POST /api/v1/subscriptions

Autenticado (USER). Cria uma nova assinatura.

Request:
```json
{
  "planId": "uuid",
  "paymentMethod": "CREDIT_CARD | PIX"
}
```

Response 201:
```json
{
  "subscriptionId": "uuid",
  "status": "PENDING",
  "planId": "uuid",
  "paymentMethod": "CREDIT_CARD | PIX",
  "createdAt": "2025-03-10T10:00:00Z"
}
```

Erros possíveis: 400, 401, 409 ACTIVE_PERIOD_EXISTS (retorna também options: ["REACTIVATE", "REFUND_AND_RESUBSCRIBE"])

---

### GET /api/v1/subscriptions/me

Autenticado (USER). Retorna a assinatura do usuário autenticado.

Response 200:
```json
{
  "subscriptionId": "uuid",
  "status": "ACTIVE",
  "planId": "uuid",
  "planName": "Mensal",
  "priceCents": 2990,
  "paymentMethod": "CREDIT_CARD",
  "currentPeriodEnd": "2025-04-10T00:00:00Z",
  "canceledAt": null,
  "createdAt": "2025-03-10T10:00:00Z"
}
```

Erros possíveis: 401, 404

---

### POST /api/v1/subscriptions/me/cancel

Autenticado (USER). Cancela a assinatura do usuário autenticado.

Request:
```json
{
  "option": "END_OF_PERIOD | IMMEDIATE"
}
```

Response 200:
```json
{
  "subscriptionId": "uuid",
  "status": "CANCELED_AT_PERIOD_END | CANCELED",
  "canceledAt": "2025-03-10T10:00:00Z",
  "accessUntil": "2025-04-10T00:00:00Z | null"
}
```

Erros possíveis: 401, 422 SUBSCRIPTION_NOT_ACTIVE

---

### POST /api/v1/subscriptions/me/reactivate

Autenticado (USER). Reativa assinatura cancelada com período ativo ou solicita reembolso para assinar novamente.

Request:
```json
{
  "option": "REACTIVATE | REFUND_AND_RESUBSCRIBE"
}
```

Response 200 (REACTIVATE):
```json
{
  "subscriptionId": "uuid",
  "status": "ACTIVE",
  "nextRenewalDate": "2025-04-10T00:00:00Z"
}
```

Response 202 (REFUND_AND_RESUBSCRIBE):
```json
{
  "message": "Reembolso em processamento. Você poderá assinar novamente em breve.",
  "refundAmountCents": 1500,
  "refundPercentage": 50.00
}
```

Erros possíveis: 401, 404, 422

---

### GET /api/v1/subscriptions (ADMIN)

Autenticado (ADMIN). Lista todas as assinaturas com paginação.

Query params: page (default 0), size (default 20), status (opcional: ACTIVE | PENDING | CANCELED | PAST_DUE)

Response 200:
```json
{
  "content": [
    {
      "subscriptionId": "uuid",
      "userId": "uuid",
      "status": "ACTIVE",
      "planName": "Mensal",
      "currentPeriodEnd": "2025-04-10T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

Erros possíveis: 401, 403

---

## assine-billing

### POST /api/v1/billing/payment-intent

Autenticado (USER). Cria PaymentIntent no Stripe para cartão. Chamado após POST /subscriptions com paymentMethod=CREDIT_CARD.

Request:
```json
{
  "subscriptionId": "uuid"
}
```

Response 200:
```json
{
  "clientSecret": "pi_xxx_secret_yyy",
  "paymentIntentId": "pi_xxx"
}
```

Erros possíveis: 401, 404, 422, 503

---

### POST /api/v1/billing/pix

Autenticado (USER). Gera cobrança Pix no Stripe. Chamado após POST /subscriptions com paymentMethod=PIX.

Request:
```json
{
  "subscriptionId": "uuid"
}
```

Response 200:
```json
{
  "qrCode": "00020126...",
  "qrCodeUrl": "https://...",
  "expiresAt": "2025-03-11T10:00:00Z",
  "paymentIntentId": "pi_xxx"
}
```

Erros possíveis: 401, 404, 422, 503

---

### POST /api/v1/refunds

Autenticado (USER). Solicita reembolso para a assinatura ativa. Rate limit: 5 req/hora por userId.

Request:
```json
{
  "subscriptionId": "uuid",
  "reason": "string opcional"
}
```

Response 202:
```json
{
  "refundId": "uuid",
  "refundAmountCents": 1993,
  "refundPercentage": 66.67,
  "originalAmountCents": 2990,
  "daysSinceActivation": 20,
  "status": "PENDING",
  "estimatedDays": "5-10 dias úteis"
}
```

Erros possíveis: 401, 403, 404, 409 REFUND_ALREADY_EXISTS, 422 SUBSCRIPTION_NOT_ACTIVE, 422 REFUND_PERIOD_EXPIRED, 422 NO_CONFIRMED_PAYMENT, 503

---

### POST /api/v1/webhooks/stripe

Público (protegido por HMAC-SHA256, não por JWT). Recebe eventos do Stripe. Não deve ser exposto pelo gateway com autenticação JWT — usa validação própria via Stripe-Signature header.

Response 200: sempre retorna 200 se a assinatura for válida, independente do processamento interno.

Erros possíveis: 401 (assinatura HMAC inválida)

---

## assine-access

### GET /api/v1/access/{userId}

Interno (não exposto pelo gateway publicamente). Consultado pelo gateway para verificar permissão antes de liberar acesso a recursos protegidos.

Response 200:
```json
{
  "userId": "uuid",
  "resource": "newsletter",
  "hasAccess": true,
  "expiresAt": "2025-04-10T00:00:00Z"
}
```

Erros possíveis: 404

---

## assine-content

### GET /api/v1/content/today

Interno (não exposto pelo gateway). Consultado pelo assine-notifications às 7h no fluxo da newsletter.

Response 200:
```json
{
  "title": "Título da newsletter",
  "bodyHtml": "<h1>...</h1>",
  "scheduledDate": "2025-03-10"
}
```

Response 404: conteúdo não encontrado ou não marcado como Pronto

---

### POST /api/v1/content/retry-today

Autenticado (ADMIN). Força reenvio manual da newsletter do dia.

Response 202:
```json
{
  "message": "Reenvio da newsletter agendado para processamento."
}
```

Erros possíveis: 401, 403, 404

---

## assine-fiscal

### GET /api/v1/invoices/me

Autenticado (USER). Lista as notas fiscais do usuário autenticado.

Query params: page (default 0), size (default 20)

Response 200:
```json
{
  "content": [
    {
      "invoiceId": "uuid",
      "number": "000001",
      "series": "A",
      "amountCents": 2990,
      "status": "ISSUED",
      "issuedAt": "2025-03-10T10:05:00Z",
      "pdfUrl": "http://minio:9000/invoices/uuid.pdf"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

Erros possíveis: 401

---

### GET /api/v1/invoices (ADMIN)

Autenticado (ADMIN). Lista todas as notas fiscais com paginação.

Query params: page (default 0), size (default 20), status (opcional: PENDING | ISSUED | FAILED | CANCELED)

Response 200: mesmo formato de /invoices/me com campo adicional userId em cada item.

Erros possíveis: 401, 403

---

## Endpoints Internos entre Serviços

Descreva que os seguintes endpoints são chamados diretamente entre serviços sem passar pelo gateway. Não requerem JWT — confiam na topologia de rede Docker onde apenas serviços internos têm acesso.

Liste os endpoints internos:

GET /api/v1/subscriptions/{id}/activation-date
  → chamado pelo assine-billing durante o fluxo de reembolso
  Response: { "activatedAt": "2025-03-10" }

GET /api/v1/subscriptions/{userId}/fiscal-data
  → chamado pelo assine-fiscal para obter dados do tomador da NFS-e
  Response:
  {
    "userId": "uuid",
    "name": "João Silva",
    "document": "123.456.789-00",
    "documentType": "CPF | CNPJ",
    "email": "user@email.com",
    "address": {
      "street": "Rua das Flores, 123",
      "city": "Sertãozinho",
      "state": "SP",
      "zipCode": "14160-000"
    }
  }

GET /api/v1/access/subscribers?resource=newsletter
  → chamado pelo assine-notifications às 7h para obter lista de assinantes ativos
  Response: { "userIds": ["uuid-1", "uuid-2"] }

GET /api/v1/users/emails?ids={uuid-1,uuid-2,...}
  → chamado pelo assine-notifications para obter emails dos assinantes antes do envio da newsletter
  Response:
  {
    "users": [
      { "userId": "uuid", "email": "user@email.com" }
    ]
  }

---

## Paginação

Descreva que todos os endpoints que retornam listas usam paginação com os seguintes query params padrão:
- page: número da página (base 0, default 0)
- size: itens por página (default 20, máximo 100)

E os seguintes campos na resposta:
- content: array com os itens da página
- page: página atual
- size: tamanho da página
- totalElements: total de registros
- totalPages: total de páginas

---

## Headers Padrão

Descreva os headers presentes em todas as requisições e respostas:

Requisições:
- Authorization: Bearer {accessToken} (obrigatório em rotas autenticadas)
- Content-Type: application/json
- X-Correlation-ID: UUID gerado pelo cliente ou pelo gateway se ausente

Respostas:
- Content-Type: application/json ou application/problem+json
- X-Correlation-ID: mesmo valor recebido na requisição
