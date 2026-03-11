# ADR-009: MinIO como Storage de PDFs de Notas Fiscais

## Status
Aceito

## Contexto
O serviço assine-fiscal emite NFS-e via Nuvem Fiscal. Após a emissão, a Nuvem Fiscal disponibiliza o PDF da nota via URL temporária. O sistema precisa decidir como armazenar e disponibilizar esse PDF para o usuário a longo prazo, considerando que o projeto roda localmente e não será publicado em nuvem.

As opções consideradas foram:
1. URL direta da Nuvem Fiscal — sem storage próprio
2. MinIO local — bucket S3-compatível rodando em Docker
3. BYTEA no banco PostgreSQL — PDF armazenado como binário

## Decisão
Adotar MinIO como solução de object storage local, rodando como container Docker no mesmo docker-compose do projeto. Após emitir a NFS-e, o assine-fiscal baixa o PDF da Nuvem Fiscal e armazena no MinIO. A URL interna do MinIO é persistida na tabela invoices e usada para envio por email e download futuro.

O MinIO é S3-compatível — em produção, a troca para AWS S3 ou Google Cloud Storage seria apenas uma mudança de configuração, sem alteração de código, graças à porta InvoiceStorageGateway.

## Porta InvoiceStorageGateway

Interface no domínio do assine-fiscal com duas operações:
- store(invoiceId: String, pdfContent: byte[]): String
  → armazena o PDF e retorna a URL de acesso
- retrieve(invoiceId: String): byte[]
  → recupera o conteúdo do PDF pelo ID da nota

Implementação: MinIOStorageAdapter.

A abstração via porta permite substituir o MinIO por qualquer solução S3-compatível (AWS S3, GCS, Azure Blob) apenas implementando um novo adapter.

## Fluxo de armazenamento

1. Nuvem Fiscal retorna pdf_url temporária na resposta da emissão
2. assine-fiscal faz GET na pdf_url para baixar o conteúdo binário
3. MinIOStorageAdapter faz upload do PDF para o bucket `invoices`
   com chave: invoices/{invoiceId}.pdf
4. MinIO retorna URL de acesso:
   http://minio:9000/invoices/{invoiceId}.pdf
5. assine-fiscal persiste a URL interna na coluna pdf_url
   da tabela invoices
6. Evento InvoiceIssued é publicado com a URL interna
7. assine-notifications consome InvoiceIssued e envia o PDF
   como anexo no email — baixa o conteúdo via MinIOStorageAdapter
   antes de anexar

## Convenção de nomenclatura dos objetos

Chave de cada objeto no bucket:
  invoices/{invoiceId}.pdf

Exemplos:
  invoices/550e8400-e29b-41d4-a716-446655440000.pdf

O bucket `invoices` é criado automaticamente na inicialização do assine-fiscal se não existir, via MinIO SDK:
  minioClient.makeBucket(MakeBucketArgs.builder()
      .bucket("invoices").build());

## Configuração do MinIO no docker-compose

- Porta 9000: API S3-compatível, usada pelo assine-fiscal e assine-notifications para upload e download
- Porta 9001: Console web do MinIO, acessível em http://localhost:9001 durante o desenvolvimento para visualizar os arquivos armazenados
- Volume minio_data: persiste os dados entre reinicializações do container
- Credenciais: MINIO_ROOT_USER e MINIO_ROOT_PASSWORD definem o access key e secret key usados pelo SDK

## Variáveis de ambiente do assine-fiscal

MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=assine
MINIO_SECRET_KEY=assine123
MINIO_BUCKET=invoices

## Dependência Maven

O assine-fiscal e o assine-notifications usam o SDK oficial do MinIO para Java:

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

## Consequências

### Positivas
- PDFs persistidos localmente — sem dependência de disponibilidade da URL temporária da Nuvem Fiscal após a emissão
- MinIO é S3-compatível — migração para AWS S3 em produção é apenas configuração, sem alteração de código
- Console web do MinIO em http://localhost:9001 facilita inspeção dos arquivos durante desenvolvimento
- Demonstra conhecimento de object storage no portfolio — padrão usado em produção por AWS S3, GCS e Azure Blob
- Volume Docker persiste os dados entre reinicializações

### Negativas
- Novo serviço no docker-compose aumenta consumo de recursos locais
- Inicialização do bucket na startup do assine-fiscal adiciona acoplamento de inicialização com o MinIO

### Riscos e mitigações
- Risco: MinIO indisponível ao tentar armazenar o PDF
  Mitigação: o Outbox Pattern do assine-fiscal já cobre retry da emissão completa — se o armazenamento falhar, a tentativa é refeita pelo job de retry incluindo o download e upload do PDF
- Risco: URL interna do MinIO não acessível fora da rede Docker
  Mitigação: para desenvolvimento local, o MinIO está exposto na porta 9000 do host. Em produção seria substituído por URL pública do S3.
- Risco: PDF indisponível na Nuvem Fiscal ao tentar baixar
  Mitigação: o download ocorre imediatamente após a emissão, enquanto a URL temporária ainda é válida. O retry do Outbox só é acionado se a emissão falhou — não após o PDF já ter sido armazenado no MinIO.

## Alternativas rejeitadas

### URL direta da Nuvem Fiscal
Links temporários podem expirar — o usuário que tentar acessar a nota meses depois encontraria um link quebrado. Inaceitável para documentos fiscais que precisam de acesso permanente.

### BYTEA no banco PostgreSQL
Banco de dados não é solução adequada para armazenamento de binários. PDFs em BYTEA aumentam o tamanho do banco, degradam performance de queries e dificultam backup e restauração. Object storage é a solução correta para esse tipo de dado.
