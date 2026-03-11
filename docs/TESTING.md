# Testing - Assine

## Visão Geral

Este documento descreve as convenções e padrões de testagem adotados em todos os serviços do projeto Assine. Todos os serviços seguem as mesmas práticas para consistência e manutenibilidade.

---

## Replay de Eventos para Testar Fluxos

### Stripe CLI

Para testar fluxos que dependem de webhooks reais do Stripe, use o Stripe CLI para simular eventos:

```bash
# Iniciar o Stripe CLI em modo listen
stripe listen --forward-to http://localhost:8082/webhooks/stripe

# Simular pagamento confirmado
stripe trigger payment_intent.succeeded

# Simular falha no pagamento
stripe trigger invoice.payment_failed

# Simular chargeback
stripe trigger charge.dispute.created

# Simular reembolso confirmado
stripe trigger charge.refund.updated --add 'status=succeeded'
```

**Importante:** O Stripe CLI deve ser iniciado antes de rodar os testes de integração que dependem de webhooks reais. Configure as credenciais do Stripe no ambiente de testes antes de iniciar.

### WireMock como Alternativa

Para ambientes de CI/CD onde o Stripe CLI não está disponível, use WireMock para mockar os webhooks:

```java
@ExtendWith(WireMockExtension.class)
class WebhookControllerIT {

    @InjectServerPort
    private int wiremockPort;

    @Test
    void deveProcessarWebhookDePagamentoConfirmado() {
        // arrange
        String payload = """
            {
              "id": "evt_test123",
              "type": "payment_intent.succeeded",
              "data": { "object": { "id": "pi_test123" } }
            }
            """;

        stubFor(post(urlPathEqualTo("/webhooks/stripe"))
            .willReturn(ok()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"received\": true}")));

        // act
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/webhooks/stripe",
            createRequestWithHmac(payload),
            String.class
        );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(OK);
    }
}
```

O WireMock permite simular os mesmos eventos do Stripe sem depender da disponibilidade do Stripe CLI, ideal para pipelines de CI/CD.

---

## Convenções

As seguintes convenções são adotadas em todos os serviços:

### Nomenclatura dos Testes

Formato: `deve{Comportamento}Quando{Condicao}`

Exemplos de nomenclatura:
- `deveAtivarSubscriptionPendente`
- `deveLancarExcecaoAoAtivarSubscriptionJaAtiva`
- `deveCalcularReembolsoIntegralQuandoDiasMenorOuIgual14`
- `deveRetornarZeroQuandoDiasMaiorOuIgual30`
- `deveRevogarAcessoQuandoSubscriptionCancelada`

O nome do teste deve ser descritivo e em português, descrevendo claramente o comportamento esperado e a condição que o dispara.

### Organização dos Testes

**Testes unitários:**
- Mesmo pacote da classe testada
- Sufixo `Test`
- Não dependem de infraestrutura externa (banco, RabbitMQ, APIs)

Exemplo:
```
br.com.assine.billing.domain.model
├── RefundCalculator.java
└── RefundCalculatorTest.java
```

**Testes de integração:**
- Pacote `integration`
- Sufixo `IT`
- Podem depender de infraestrutura (Testcontainers, WireMock, RabbitMQ)

Exemplo:
```
br.com.assine.billing.integration
├── WebhookControllerIT.java
├── OutboxPublisherIT.java
└── RefundServiceIT.java
```

### Padrão AAA (Arrange, Act, Assert)

Todos os testes seguem o padrão AAA com comentários explícitos para legibilidade:

```java
@Test
void deveAtivarSubscriptionQuandoPagamentoConfirmado() {
    // arrange
    UUID paymentId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();

    Payment payment = new Payment(paymentId, subscriptionId, CONFIRMED);
    paymentRepository.save(payment);

    // act
    PaymentConfirmedEvent event = new PaymentConfirmedEvent(paymentId, subscriptionId);
    paymentEventConsumer.onPaymentConfirmed(event);

    // assert
    Subscription subscription = subscriptionRepository.findById(subscriptionId);
    assertThat(subscription.getStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getActivatedAt()).isNotNull();
}
```

Os comentários `// arrange`, `// act` e `// assert` são obrigatórios para facilitar a leitura rápida dos testes.

### Uso de Awaitility em Testes de Integração Assíncrona

Para testes que dependem de operações assíncronas (consumo de filas RabbitMQ, webhooks, jobs schedulers), use o Awaitility para aguardar o estado esperado:

```java
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@Test
void deveAtivarSubscriptionAposWebhook() {
    // arrange
    UUID subscriptionId = UUID.randomUUID();
    subscriptionRepository.save(new Subscription(subscriptionId, PENDING));

    // act
    stripeWebhookController.handleWebhook(createPaymentSucceededEvent());

    // assert
    await().atMost(10, SECONDS).untilAsserted(() -> {
        var subscription = subscriptionRepository.findById(subscriptionId);
        assertThat(subscription.getStatus()).isEqualTo(ACTIVE);
    });
}
```

O Awaitility permite testar operações assíncronas sem usar `Thread.sleep()`, com timeout configurável e mensagens de erro claras.

```java
await().atMost(10, SECONDS)
    .withPollInterval(1, SECONDS)
    .untilAsserted(() -> {
        var refund = refundRepository.findById(refundId);
        assertThat(refund.getStatus()).isEqualTo(COMPLETED);
    });
```

### Testes de Domínio (Unitários)

Testes de domínio não devem depender de Spring Context ou infraestrutura:

```java
class RefundCalculatorTest {

    private final RefundCalculator calculator = new RefundCalculator();

    @Test
    void deveRetornarReembolsoIntegralQuandoDiasMenorOuIgual14() {
        // arrange
        int paidAmountCents = 2990; // R$29,90
        LocalDate activatedAt = LocalDate.now().minusDays(10);
        LocalDate refundRequestedAt = LocalDate.now();

        // act
        RefundAmount result = calculator.calculate(
            paidAmountCents,
            activatedAt,
            refundRequestedAt
        );

        // assert
        assertThat(result.getRefundAmountCents()).isEqualTo(2990);
        assertThat(result.getRefundPercentage()).isEqualTo(100);
    }

    @Test
    void deveRetornarZeroQuandoDiasMaiorOuIgual30() {
        // arrange
        int paidAmountCents = 2990;
        LocalDate activatedAt = LocalDate.now().minusDays(30);
        LocalDate refundRequestedAt = LocalDate.now();

        // act
        RefundAmount result = calculator.calculate(
            paidAmountCents,
            activatedAt,
            refundRequestedAt
        );

        // assert
        assertThat(result.getRefundAmountCents()).isEqualTo(0);
        assertThat(result.getRefundPercentage()).isEqualTo(0);
    }
}
```

### Testes de Integração com Testcontainers

Para testes que dependem de infraestrutura real (PostgreSQL, RabbitMQ), use Testcontainers:

```java
@Testcontainers
class SubscriptionServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer<>("rabbitmq:3-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Test
    void deveCriarSubscriptionEPublicarEvento() {
        // arrange
        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            UUID.randomUUID(),
            "MENSAL"
        );

        // act
        subscriptionService.create(command);

        // assert
        await().atMost(5, SECONDS).untilAsserted(() -> {
            var subscription = subscriptionRepository.findById(command.getUserId());
            assertThat(subscription).isNotNull();
            assertThat(subscription.getStatus()).isEqualTo(PENDING);
        });
    }
}
```

### Mock de Dependências Externas

Para testes que dependem de APIs externas (Stripe, SendGrid, Nuvem Fiscal), use `@MockBean` do Spring Boot:

```java
@SpringBootTest
class PaymentServiceTest {

    @MockBean
    private StripeGateway stripeGateway;

    @MockBean
    private DomainEventPublisher eventPublisher;

    @Autowired
    private PaymentService paymentService;

    @Test
    void deveProcessarPagamentoESolicitarReembolso() {
        // arrange
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(paymentId, 1000, CONFIRMED);

        when(stripeGateway.refund(any(), anyInt()))
            .thenReturn(new RefundResult("ref_123", SUCCESS));

        // act
        paymentService.requestRefund(paymentId, "Solicitação do usuário");

        // assert
        verify(stripeGateway).refund(payment.getExternalId(), 1000);
        verify(eventPublisher).publish(any(PaymentRefundedEvent.class));
    }
}
```

### Coverage de Código

O objetivo é manter cobertura de código acima de 80% para:
- Classes de domínio (models, value objects, domain services)
- Casos de uso (application services)

Classes de infraestrutura (adapters, controllers) devem ter foco em cenários críticos e integrações, não necessariamente alta cobertura absoluta.

Para executar os testes com relatório de coverage:

```bash
mvn clean test jacoco:report
```

O relatório é gerado em `target/site/jacoco/index.html`.
