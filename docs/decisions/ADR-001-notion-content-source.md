# ADR-001: Notion como fonte de conteúdo da newsletter

## Status
Aceito

## Contexto
O sistema Assine precisa de uma fonte de conteúdo para a newsletter diária
enviada às 7h (horário de Brasília). O administrador precisa escrever e revisar
o conteúdo antes do envio. As opções consideradas foram:

1. CMS próprio (assine-content com editor interno)
2. Integração com Notion via API
3. Arquivo Markdown versionado em repositório Git

## Decisão
Integrar com o Notion via API REST oficial (api.notion.com/v1).

O administrador mantém um Database no Notion com as propriedades:
- Title (Text): título da newsletter
- Date (Date): data de envio programada
- Status (Select): Rascunho | Pronto
- Body: conteúdo em blocos nativos do Notion (page content)

O serviço assine-content implementa um job scheduler que, às 7h horário de
Brasília, consulta o Database filtrando por Date = hoje e Status = Pronto,
busca os blocos da página, converte para HTML e disponibiliza via endpoint
interno GET /content/today para o assine-notifications.

A integração com o Notion é isolada atrás de uma porta ContentSourceGateway
(padrão hexagonal), de forma que a fonte de conteúdo pode ser substituída
no futuro sem alterar o domínio.

## Consequências

### Positivas
- O administrador escreve em uma ferramenta familiar, com suporte a
  formatação rica, colaboração e histórico de versões
- O sistema não precisa construir nem manter um editor de conteúdo
- A porta ContentSourceGateway desacopla o domínio da fonte externa,
  permitindo trocar o Notion por outra ferramenta no futuro sem
  impacto no restante do sistema

### Negativas
- Dependência de disponibilidade da API do Notion em horário crítico (7h)
- Necessidade de converter blocos nativos do Notion para HTML,
  cobrindo os tipos: paragraph, heading_1, heading_2,
  bulleted_list_item, numbered_list_item
- Credenciais adicionais para gerenciar: NOTION_API_KEY e NOTION_DATABASE_ID

### Riscos e mitigações
- Risco: API do Notion indisponível às 7h
  Mitigação: falha silenciosa com log estruturado, métrica
  content.fetch.failed incrementada, e endpoint manual
  POST /content/retry-today para reenvio sob demanda pelo admin
- Risco: conteúdo não marcado como Pronto no horário
  Mitigação: comportamento esperado — o sistema não envia.
  O admin é responsável por marcar o status antes das 7h.

## Alternativas rejeitadas

### CMS próprio
Exigiria construir e manter um editor de conteúdo, desviando o foco
do projeto das decisões arquiteturais centrais (mensageria, hexagonal,
observabilidade).

### Markdown em repositório Git
Simples demais para um produto real. Não oferece interface amigável
para o administrador e não demonstra integração com sistemas externos.
