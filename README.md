# Assine

Plataforma de assinaturas de newsletter construída com microserviços em Java/Spring Boot, arquitetura hexagonal e DDD.

![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3-green)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-blue)

## Sobre o projeto

O Assine é uma plataforma de assinaturas de newsletter com foco em arquitetura de software. O objetivo do projeto é demonstrar a aplicação prática de DDD, arquitetura hexagonal, comunicação assíncrona orientada a eventos e boas práticas de engenharia de software em um domínio de negócio real.

O sistema suporta assinatura via cartão de crédito e Pix (Stripe), cancelamento com política de reembolso progressivo, emissão automática de NFS-e via Nuvem Fiscal, envio de newsletter diária com conteúdo gerenciado pelo Notion, e autenticação sem senha via magic link e Google OAuth2.

O projeto foi construído com decisões arquiteturais deliberadas e documentadas — cada bounded context tem responsabilidade clara, a comunicação entre serviços é exclusivamente assíncrona via RabbitMQ, e o Outbox Pattern garante consistência eventual entre persistência e mensageria.

## Arquitetura

| Serviço | Responsabilidade | Porta |
|---------|-----------------|-------|
| assine-gateway | Roteamento, autenticação JWT, rate limiting | 8080 |
| assine-auth | Magic link, Google OAuth2, emissão de JWT | 8086 |
| assine-subscriptions | Core do domínio: planos e assinaturas | 8081 |
| assine-billing | Integração Stripe, webhooks, Outbox Pattern | 8082 |
| assine-access | Permissões de acesso ao conteúdo | 8083 |
| assine-notifications | Envio de emails via SendGrid | 8084 |
| assine-content | Integração com Notion, scheduler newsletter | 8087 |
| assine-fiscal | Emissão de NFS-e via Nuvem Fiscal | 8085 |

**Decisões arquiteturais centrais:**
- Arquitetura hexagonal (ports and adapters) em todos os serviços
- Comunicação entre BCs exclusivamente via eventos (RabbitMQ)
- Outbox Pattern no assine-billing para atomicidade entre persistência e publicação de eventos
- JWT com RS256 e validação local no gateway via JWKS

## Tecnologias

| Categoria | Tecnologias |
|-----------|-------------|
| Backend | Java 21, Spring Boot 3, Spring Cloud Gateway |
| Mensageria | RabbitMQ |
| Banco de dados | PostgreSQL 16 (instância por serviço) |
| Object Storage | MinIO (S3-compatível) |
| Autenticação | JWT RS256, Google OAuth2, Magic Link |
| Pagamentos | Stripe (cartão + Pix) |
| Fiscal | Nuvem Fiscal (NFS-e) |
| Conteúdo | Notion API |
| Email | SendGrid |
| Observabilidade | Micrometer, Prometheus, Grafana, Zipkin |
| Testes | JUnit 5, Testcontainers, WireMock, AssertJ |
| Infraestrutura | Docker, Docker Compose |

## Pré-requisitos

**O que precisa estar instalado:**
- Docker e Docker Compose
- Java 21 (para rodar os serviços fora do Docker)
- Stripe CLI (para testar webhooks localmente)

**Contas e chaves necessárias:**
- Conta Stripe (gratuita) — para obter as chaves de API e webhook
- Conta Nuvem Fiscal (gratuita em sandbox)
- Projeto Google Cloud com OAuth2 configurado
- Conta SendGrid (gratuita)
- Integration token do Notion e Database ID configurado

## Como rodar localmente

### 6.1 Configuração do ambiente

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/assine.git
cd assine

# Copie o arquivo de variáveis de ambiente
cp .env.example .env

# Edite o .env com suas chaves
# Preencha: STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET,
# GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET,
# SENDGRID_API_KEY, NOTION_API_KEY, NOTION_DATABASE_ID,
# NUVEM_FISCAL_CLIENT_ID, NUVEM_FISCAL_CLIENT_SECRET
```

### 6.2 Subindo a infraestrutura

```bash
# Sobe todos os serviços, bancos, RabbitMQ, MinIO e observabilidade
docker compose up -d

# Verifica se todos os serviços estão saudáveis
docker compose ps
```

### 6.3 Configurando o Stripe CLI para webhooks

```bash
# Instale o Stripe CLI: https://stripe.com/docs/stripe-cli
stripe login
stripe listen --forward-to localhost:8082/api/v1/webhooks/stripe

# Copie o webhook secret exibido no terminal para o .env
# STRIPE_WEBHOOK_SECRET=whsec_...
```

### 6.4 Testando o fluxo de assinatura

```bash
# Simular pagamento confirmado
stripe trigger payment_intent.succeeded

# Simular falha no pagamento
stripe trigger invoice.payment_failed

# Simular chargeback
stripe trigger charge.dispute.created
```

## Interfaces disponíveis localmente

| Interface | URL | Credenciais |
|-----------|-----|-------------|
| API Gateway | http://localhost:8080 | — |
| RabbitMQ Management | http://localhost:15672 | assine / assine |
| MinIO Console | http://localhost:9001 | assine / assine123 |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |
| Zipkin | http://localhost:9411 | — |

## Documentação

- `docs/Assine_structure.md` — estrutura completa do repositório
- `docs/API.md` — contrato completo da API (request/response)
- `docs/OBSERVABILITY.md` — logs, métricas, tracing e alertas
- `docs/TESTING.md` — estratégia de testes por serviço
- `docs/decisions/` — ADRs com todas as decisões arquiteturais
- `docs/flows/` — fluxos detalhados dos processos principais

## Decisões arquiteturais

- **ADR-001:** Notion como fonte de conteúdo da newsletter
- **ADR-002:** Stripe como gateway de pagamento único (cartão + Pix)
- **ADR-003:** Política de reembolso progressivo a partir do dia 15
- **ADR-004:** Tratamento de chargebacks com cancelamento imediato
- **ADR-005:** Réguas de renovação distintas para cartão e Pix
- **ADR-006:** Serviço dedicado assine-fiscal com Nuvem Fiscal
- **ADR-007:** Autenticação via magic link e Google OAuth2 com account linking
- **ADR-008:** Autorização por papel com validação centralizada no gateway
- **ADR-009:** MinIO como storage de PDFs de notas fiscais
