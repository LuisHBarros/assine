# ADR-010: BaseEntity com UUIDv7 como padrão

**Data:** 2026-03-17
**Status:** Aceito

## Contexto

Em arquiteturas de microserviços e bancos de dados relacionais como o PostgreSQL, o uso de identificadores globais únicos (UUIDs) é preferível em vez de IDs incrementais para evitar o vazamento de informações de negócio e simplificar o mapeamento em sistemas distribuídos.

No entanto, o uso do UUID versão 4 (totalmente randômico) como chave primária pode causar **fragmentação de índices B-Tree** ao longo do tempo. Como os valores não são sequenciais, as inserções em massa exigem a reescrita e o particionamento constante das páginas de índice, degradando o desempenho de gravação (page splits e redução de locality of reference).

O **UUID versão 7 (UUIDv7)**, introduzido na RFC 9562, resolve esse problema combinando um timestamp em milissegundos com uma parte aleatória. Isso garante que os identificadores gerados sejam ordenáveis de forma cronológica (lexicograficamente sequenciais) e mantenham as propriedades de colisão quase nulas.

## Decisão

Foi definido que **todos os microserviços** da plataforma Assine devem adotar o **UUIDv7** como padrão para chaves primárias.

Para garantir a consistência e evitar repetição de código, foi adotada a convenção da criação de uma classe **`BaseEntity`** em cada microserviço de persistência. Essa classe deve:
- Ser anotada com `@MappedSuperclass` (em projetos JPA/Hibernate).
- Conter a definição do campo `id` do tipo `UUID`.
- Ser responsável por gerar e atribuir automaticamente um UUIDv7 no momento de pré-persistência (`@PrePersist` ou através de um gerador customizado) caso o ID não tenha sido fornecido.

Como a biblioteca padrão do Java (`java.util.UUID`) ainda não fornece a geração nativa do UUIDv7, o projeto utiliza bibliotecas especializadas (ex: `uuid-creator` de *f4b6a3*) para essa finalidade.

## Consequências

- **Positivas:**
  - **Desempenho de banco de dados:** Redução significativa na fragmentação do índice B-Tree, otimizando a velocidade de `INSERT` no PostgreSQL e economizando espaço em disco.
  - **Localidade de referência:** Buscas por registros criados em datas próximas tendem a estar nas mesmas páginas de disco, melhorando o desempenho de `SELECT`.
  - **Ordenação temporal implícita:** IDs podem ser usados secundariamente para ordenação cronológica caso o timestamp de criação não esteja indexado.
  - **Consistência do ecossistema:** A existência de um `BaseEntity` simplifica a criação de novas entidades e mantém o padrão arquitetural em todos os *bounded contexts*.

- **Negativas/Limitações:**
  - Dependência de uma biblioteca de terceiros para a geração correta do UUIDv7.
  - Os IDs de diferentes máquinas/serviços gerados no exato mesmo milissegundo continuam aleatórios na porção de entropia, necessitando uma geração de qualidade e entropia segura.