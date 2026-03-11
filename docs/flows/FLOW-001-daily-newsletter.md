# FLOW-001: Envio da Newsletter Diária

## Visão Geral
O fluxo é disparado automaticamente às 7h (horário de Brasília) pelo serviço assine-notifications. Dois serviços participam ativamente: assine-content (busca e transforma o conteúdo do Notion) e assine-notifications (orquestra e envia). A comunicação entre eles é HTTP síncrono — justificativa: é uma leitura no momento do envio, não uma mutação de estado. Se o conteúdo não estiver disponível, o fluxo falha imediatamente e nenhum email é enviado.

## Diagrama do Fluxo

```
Scheduler (7h)
  → assine-notifications
      → GET /content/today → assine-content → Notion API
      → GET /access/subscribers → assine-access
      → GET /users/emails → assine-subscriptions
      → SendGrid API (em lotes de 50)
      → notification_history (registra sent ou failed por usuário)
```

## Passo a Passo

### 1. Scheduler dispara
- Cron: `0 0 7 * * *` com zone America/Sao_Paulo
- Gera um correlationId (UUID) no início do fluxo
- Insere o correlationId no MDC do Logback
- Propaga via header X-Correlation-ID em todas as chamadas HTTP do fluxo

### 2. Busca o conteúdo do dia
- GET /content/today no assine-content
- Resposta 200: objeto com title, bodyHtml, scheduledDate
- Resposta 404: conteúdo não encontrado ou não marcado como Pronto no Notion
- Comportamento em caso de 404 ou erro: fluxo encerrado, métrica content.fetch.failed incrementada, nenhum email enviado

### 3. Busca assinantes ativos
- GET /access/subscribers?resource=newsletter no assine-access
- Retorna lista de userIds com permissão ativa e não revogada

### 4. Busca emails dos assinantes
- GET /users/emails?ids={uuid-1,uuid-2,...} no assine-subscriptions
- O assine-subscriptions é o dono do email do usuário
- O assine-notifications nunca armazena emails — busca sob demanda

### 5. Dispara os emails via SendGrid
- Processamento em lotes de 50 usuários
- Cada lote processado em paralelo (parallelStream)
- Falha individual não aborta o lote — cada envio é independente
- Cada envio registrado no notification_history com status SENT ou FAILED
- Em caso de falha individual: loga o erro com userId e correlationId, não relança a exceção

### 6. Registro de métricas e log final
- Counter newsletter.sent incrementado com total de envios bem-sucedidos
- Counter newsletter.failed incrementado com total de falhas
- Log estruturado ao final com: total_subscribers, sent, failed, correlation_id

## Comportamentos de Falha

1. assine-content indisponível ou retorna 404
   → fluxo encerrado imediatamente, métrica content.fetch.failed, endpoint manual POST /content/retry-today disponível para o admin

2. assine-access indisponível
   → fluxo encerrado, nenhum email enviado, erro logado com correlationId

3. Falha individual no envio de um email (SendGrid)
   → registrado como FAILED no notification_history, fluxo continua para os demais assinantes

## Decisões de Design

1. Por que HTTP síncrono entre assine-notifications e assine-content, se a comunicação entre serviços é via eventos?
   Justificativa: eventos são para mutações de estado com desacoplamento temporal. Aqui é uma leitura síncrona — o conteúdo precisa estar disponível no momento do envio. Falha imediata é o comportamento correto.

2. Por que o assine-notifications busca o email no assine-subscriptions em vez de armazenar localmente?
   Justificativa: cada serviço é dono dos seus dados. Email é dado do usuário — pertence ao contexto de assinaturas. Duplicar esse dado criaria inconsistência eventual sem benefício claro.
