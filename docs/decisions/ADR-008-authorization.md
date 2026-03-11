# ADR-008: Autorização por Papel com Validação no Gateway

## Status
Aceito

## Contexto
O sistema Assine possui dois perfis de usuário com permissões distintas: USER (assinante) e ADMIN (operador do sistema). A autorização precisa ser consistente entre todos os serviços sem duplicar lógica de validação em cada um.

## Decisão
Centralizar a validação do JWT e a extração do papel no assine-gateway. Os serviços internos recebem o userId e o role via headers HTTP internos e confiam neles sem revalidar o token. A fronteira de segurança é única: o gateway.

## Headers internos propagados pelo gateway

Para toda requisição autenticada que passa pelo gateway:
- X-User-Id: uuid do usuário extraído do JWT
- X-User-Role: papel do usuário (USER | ADMIN)
- X-Correlation-ID: id de rastreabilidade da requisição

Os serviços internos nunca recebem o JWT diretamente — apenas os headers propagados pelo gateway.

## Matriz de permissões

USER:
- GET  /subscriptions/me          — visualiza própria assinatura
- POST /subscriptions/me/cancel   — cancela próxima renovação
- POST /refunds                   — solicita reembolso
- GET  /invoices/me               — visualiza próprias notas fiscais

ADMIN:
- GET  /subscriptions             — lista todas as assinaturas
- GET  /users                     — lista todos os usuários
- POST /newsletters               — cria conteúdo da newsletter
- PUT  /newsletters/{id}/publish  — publica conteúdo
- GET  /invoices                  — lista todas as notas fiscais
- POST /content/retry-today       — reenvio manual da newsletter

Público (sem autenticação):
- POST /auth/magic-link
- GET  /auth/magic-link/validate
- GET  /auth/oauth2/google
- GET  /auth/oauth2/google/callback
- POST /auth/refresh
- GET  /auth/.well-known/jwks.json
- POST /webhooks/stripe            — protegido por HMAC, não por JWT

## Validação no gateway

O gateway executa na seguinte ordem para rotas protegidas:
1. Extrai o header Authorization: Bearer {token}
2. Valida assinatura RS256 usando chave pública do JWKS
3. Valida expiração (exp)
4. Extrai sub (userId) e role do payload
5. Verifica se o role tem permissão para a rota acessada
6. Propaga X-User-Id e X-User-Role para o serviço downstream
7. Se qualquer etapa falhar: retorna 401 ou 403

## Isolamento de recursos por usuário

Endpoints do perfil USER são restritos ao próprio usuário. O serviço downstream valida que o recurso solicitado pertence ao X-User-Id recebido. Exemplo: POST /refunds valida que a subscriptionId no body pertence ao userId do header — um USER não pode solicitar reembolso em nome de outro usuário.

## Rate limiting por papel

Configurado no gateway via Spring Cloud Gateway + Redis:

POST /auth/magic-link:
- 3 requisições por minuto por IP
- Previne spam de emails de magic link

POST /refunds:
- 5 requisições por hora por userId
- Previne abuso de solicitações de reembolso

Demais rotas autenticadas:
- 60 requisições por minuto por userId

## Consequências

### Positivas
- Fronteira de segurança única: apenas o gateway lida com JWT
- Serviços internos são mais simples — sem dependência de biblioteca de segurança para validar tokens
- Rate limiting centralizado no gateway — sem duplicação por serviço
- Matriz de permissões documentada e centralizada em um único lugar

### Negativas
- Serviços internos confiam cegamente nos headers do gateway — comunicação direta entre serviços (bypassing o gateway) seria insegura. Mitigado pela topologia de rede: serviços internos não são expostos externamente no docker-compose
- Mudança de papel de um usuário só é refletida após expiração do JWT atual (até 1 hora de defasagem)

### Riscos e mitigações
- Risco: serviço interno chamado diretamente sem passar pelo gateway
  Mitigação: no docker-compose, apenas o gateway expõe porta pública. Serviços internos estão na rede interna Docker apenas
- Risco: JWT roubado usado até expiração
  Mitigação: accessToken com vida curta (1 hora). Refresh token pode ser revogado via blacklist em Redis se necessário no futuro

## Alternativas rejeitadas

### Validação do JWT em cada serviço
Duplica lógica de segurança, aumenta acoplamento com a biblioteca de JWT e torna a rotação de chaves mais complexa. Centralizar no gateway é o padrão para arquiteturas de microserviços.

### RBAC granular (permissões por recurso)
Desnecessário para dois papéis com permissões bem definidas. RBAC granular faria sentido se houvesse múltiplos papéis com sobreposição de permissões.
