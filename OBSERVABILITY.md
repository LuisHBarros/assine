# Observabilidade - Assine

## 1. Visão Geral

Observabilidade em sistemas distribuídos responde três perguntas fundamentais:

- **O que aconteceu nessa requisição?** (rastreamento/distributed tracing)
- **O sistema está saudável agora?** (métricas)
- **Quando e onde algo começou a falhar?** (logs)

Stack escolhida: Micrometer + Prometheus + Grafana + Zipkin

---

## 2. Logs Estruturados

Todos os logs em JSON usando `logstash-logback-encoder`.

### Campos obrigatórios em todo log
- `timestamp`
- `level`
- `service`
- `correlation_id`
- `user_id` (quando disponível)
- `event`
- `message`

O correlation_id deve ser propagado via header HTTP `X-Correlation-ID`, inserido no MDC do Logback em cada serviço através de um servlet filter.

### logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${spring.application.name}"}</customFields>
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <includeStructuredArguments>true</includeStructuredArguments>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### CorrelationIdFilter.java

```java
package br.com.assine.gateway.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC = "correlation_id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_MDC, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC);
        }
    }
}
```

Para propagar o correlation_id em requisições de saída (RestTemplate), configure um interceptor:

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(new CorrelationIdInterceptor()));
        return restTemplate;
    }
}

public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC = "correlation_id";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
        String correlationId = MDC.get(CORRELATION_ID_MDC);
        if (correlationId != null) {
            request.getHeaders().set(CORRELATION_ID_HEADER, correlationId);
        }
        return execution.execute(request, body);
    }
}
```

Para WebClient:

```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
            .filter((request, next) -> {
                String correlationId = MDC.get("correlation_id");
                if (correlationId != null) {
                    request.headers().set("X-Correlation-ID", correlationId);
                }
                return next.exchange(request);
            })
            .build();
}
```

---

## 3. Métricas com Micrometer + Prometheus

Dependência: `micrometer-registry-prometheus` (já incluso no Spring Boot Actuator).

Endpoint exposto em: `/actuator/prometheus`

### assine-billing

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BillingMetrics {

    private final Counter webhookReceived;
    private final Counter webhookDuplicate;

    public BillingMetrics(MeterRegistry registry) {
        this.webhookReceived = Counter.builder("webhook.received")
                .tag("status", "unknown")
                .description("Webhooks received by status")
                .register(registry);

        this.webhookDuplicate = Counter.builder("webhook.duplicate")
                .description("Duplicate webhooks blocked by idempotency")
                .register(registry);
    }

    public void recordWebhookReceived(String status) {
        webhookReceived.increment(
            Tags.of("status", status)
        );
    }

    public void recordDuplicateWebhook() {
        webhookDuplicate.increment();
    }
}
```

Gauge para outbox.pending:

```java
@Component
public class OutboxMetrics {

    private final OutboxRepository outboxRepository;

    public OutboxMetrics(MeterRegistry registry, OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;

        Gauge.builder("outbox.pending", outboxRepository, repo -> repo.countUnpublished())
                .description("Number of unpublished outbox events")
                .register(registry);
    }
}
```

No repository:

```java
public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {

    @Query("SELECT COUNT(o) FROM OutboxEntity o WHERE o.published = false")
    long countUnpublished();
}
```

### assine-subscriptions

```java
@Component
public class SubscriptionMetrics {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionMetrics(MeterRegistry registry, SubscriptionRepository repository) {
        this.subscriptionRepository = repository;

        for (SubscriptionStatus status : SubscriptionStatus.values()) {
            Gauge.builder("subscriptions.total", repository, repo ->
                    repo.countByStatus(status.name()))
                    .tag("status", status.name())
                    .description("Total subscriptions by status")
                    .register(registry);
        }
    }
}
```

No repository:

```java
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    @Query("SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.status = :status")
    long countByStatus(String status);
}
```

### assine-auth

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    private final Counter magicLinkRequested;
    private final Counter magicLinkValidated;
    private final Counter magicLinkFailed;
    private final Counter oauth2Success;
    private final Counter oauth2Failed;

    public AuthMetrics(MeterRegistry registry) {
        this.magicLinkRequested = Counter.builder("magic.link.requested")
                .description("Magic links requested")
                .register(registry);

        this.magicLinkValidated = Counter.builder("magic.link.validated")
                .description("Magic links successfully validated")
                .register(registry);

        this.magicLinkFailed = Counter.builder("magic.link.failed")
                .tag("reason", "unknown")
                .description("Magic link validation failures")
                .register(registry);

        this.oauth2Success = Counter.builder("oauth2.success")
                .tag("provider", "google")
                .description("Successful OAuth2 authentications")
                .register(registry);

        this.oauth2Failed = Counter.builder("oauth2.failed")
                .tag("provider", "google")
                .tag("reason", "unknown")
                .description("Failed OAuth2 authentications")
                .register(registry);
    }

    public void recordMagicLinkRequested() {
        magicLinkRequested.increment();
    }

    public void recordMagicLinkValidated() {
        magicLinkValidated.increment();
    }

    public void recordMagicLinkFailed(String reason) {
        magicLinkFailed.increment(Tags.of("reason", reason));
    }

    public void recordOAuth2Success(String provider) {
        oauth2Success.increment(Tags.of("provider", provider));
    }

    public void recordOAuth2Failed(String provider, String reason) {
        oauth2Failed.increment(Tags.of("provider", provider, "reason", reason));
    }
}
```

Gauge para auth.users.total:

```java
@Component
public class AuthUserMetrics {

    private final AuthUserRepository authUserRepository;

    public AuthUserMetrics(MeterRegistry registry, AuthUserRepository repository) {
        this.authUserRepository = repository;

        for (UserRole role : UserRole.values()) {
            Gauge.builder("auth.users.total", repository, repo ->
                    repo.countByRole(role.name()))
                    .tag("role", role.name())
                    .description("Total auth users by role")
                    .register(registry);
        }
    }
}
```

No repository:

```java
public interface AuthUserRepository extends JpaRepository<AuthUserEntity, Long> {

    @Query("SELECT COUNT(u) FROM AuthUserEntity u WHERE u.role = :role")
    long countByRole(String role);
}
```

---

## 4. Distributed Tracing com Micrometer Tracing + Zipkin

Dependências Maven:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
```

Cada requisição recebe um traceId automático que aparece nos logs e permite visualizar no Zipkin o caminho completo entre serviços.

### Fluxo de trace no Zipkin

```
Gateway
  └─→ Subscriptions (HTTP)
        └─→ RabbitMQ Event (PaymentConfirmed)
              ├─→ Access Consumer
              └─→ Notification Consumer
```

### application.yml

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
spring:
  application:
    name: subscriptions
```

---

## 5. Infraestrutura (docker-compose)

### docker-compose.yml (adicionar serviços)

```yaml
services:

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./infra/prometheus.yml:/etc/prometheus/prometheus.yml
    depends_on:
      - gateway
      - subscriptions
      - billing
      - access
      - notifications
      - fiscal

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus

  zipkin:
    image: openzipkin/zipkin:latest
    ports:
      - "9411:9411"

volumes:
  grafana-data:
```

### infra/prometheus.yml

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['gateway:8080']

  - job_name: 'subscriptions'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['subscriptions:8081']

  - job_name: 'billing'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['billing:8082']

  - job_name: 'access'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['access:8083']

  - job_name: 'notifications'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['notifications:8084']

  - job_name: 'fiscal'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['fiscal:8085']

  - job_name: 'auth'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['auth:8086']
```

---

## 6. Alertas Recomendados

Alertas críticos a configurar no Grafana:

- **outbox.pending crescendo por mais de 5 minutos**
  Indica falha na publicação de eventos para o RabbitMQ. Possíveis causas: RabbitMQ indisponível, problema de autenticação, ou erro no OutboxPublisher.

- **webhook.duplicate acima do esperado**
  Pode indicar replay de webhooks externos ou problema na geração de idempotency keys. Taxa normal deve ser baixa (<1%).

- **Taxa de subscriptions com status PAST_DUE aumentando**
  Problema recorrente no gateway de pagamento ou falta de renovação automática. Monitorar tendência de aumento.

- **Latência do endpoint /actuator/health acima de 1s em qualquer serviço**
  Indica degradação de performance. Predispor para investigar gargalos: CPU, memória, conexões de banco ou lentidão de rede.

- **Taxa de magic.link.failed acima de 10% dos requests**
  Pode indicar problema no serviço de email ou tokens expirando antes do uso. Verificar logs para identificar a causa (token_not_found, token_expired, token_already_used).

- **Taxa de oauth2.failed acima de 5% dos requests**
  Pode indicar problema na integração com Google OAuth2 ou erros na validação do email. Verificar logs para identificar a causa (invalid_code, invalid_grant, email_not_verified).

- **Taxa de novos cadastros (auth.users.total) caindo abruptamente**
  Indica possível problema no fluxo de autenticação. Predispor para investigar se os endpoints de magic link e OAuth2 estão respondendo corretamente.
