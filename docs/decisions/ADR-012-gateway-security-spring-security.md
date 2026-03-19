# ADR-012: Segurança Centralizada no Gateway com Spring Security

## Status
Aceito

## Contexto
Em uma arquitetura de microserviços, a segurança pode ser implementada de forma distribuída (cada serviço valida o token) ou centralizada (o gateway valida e os serviços confiam). 

No projeto Assine, as rotas de todos os microserviços são conhecidas apenas pelo Gateway, que atua como o ponto de entrada único. Precisamos de um mecanismo robusto para:
1. Validar tokens JWT (RS256) vindos de clientes externos.
2. Aplicar regras de autorização baseadas em papéis (USER/ADMIN) antes de rotear a requisição.
3. Propagar a identidade do usuário para os microserviços internos.

## Decisão
Utilizar o **Spring Security** (via Spring Cloud Gateway) como o motor de segurança centralizado no `assine-gateway`.

### Motivação
O Gateway é o único componente que possui a visão completa de todas as rotas do ecossistema. Ao centralizar a segurança nele:
- **Redução de Acoplamento**: Os microserviços (`billing`, `subscriptions`, `fiscal`, etc.) não precisam conhecer detalhes de JWT ou RS256. Eles recebem a identidade já validada via headers.
- **Segurança por Omissão**: Qualquer novo endpoint adicionado é automaticamente protegido pelo Gateway, a menos que explicitamente configurado como público.
- **Facilidade de Manutenção**: A rotação de chaves públicas (JWKS) ou mudanças nas políticas de CORS e RBAC são feitas em um único lugar.

### Funcionamento
1. **Filtro de Autenticação**: O Gateway intercepta a requisição, extrai o JWT e valida sua assinatura contra o JWKS do `assine-auth`.
2. **Autorização (RBAC)**: Com base no payload do JWT (campo `role`), o Spring Security decide se o usuário pode acessar a rota solicitada (ex: `/admin/**` requer `ROLE_ADMIN`).
3. **Propagação de Contexto**: Após a validação, o Gateway injeta headers internos (`X-User-Id`, `X-User-Role`) na requisição enviada ao microserviço de destino.

## Consequências

### Positivas
- **Simplificação dos Microserviços**: Menos código boilerplate de segurança nos serviços de negócio.
- **Consistência**: A mesma regra de autorização é aplicada uniformemente, independente de qual serviço atende a requisição.
- **Visibilidade**: Auditoria e logs de acesso centralizados.

### Negativas
- **Single Point of Failure**: Se a segurança no Gateway falhar ou estiver mal configurada, todo o sistema fica vulnerável ou inacessível.
- **Confiança Implícita**: Microserviços internos devem confiar nos headers recebidos. Isso requer que a rede interna seja protegida (serviços não expostos diretamente à internet), o que já está garantido pela topologia do Docker Compose do projeto.

## Alternativas Rejeitadas
- **Validação Distribuída**: Exigiria que cada microserviço configurasse o Spring Security e conhecesse a chave pública do `assine-auth`, aumentando a complexidade de configuração e o overhead de rede para buscar o JWKS.
