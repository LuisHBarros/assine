# Agent Guidelines for assine-content

## Service Overview
Manages daily newsletter content from Notion. Converts Notion blocks to HTML and publishes `assine.content.ready` event via RabbitMQ. Contains a 7 AM scheduler.

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn test -Dtest=ClassName
```

## Service Structure
- **domain/**: Core domain logic
  - model/: NewsletterContent
  - event/: ContentReadyEvent
  - port/in/: RetrieveContentUseCase, TriggerNewsletterRetryUseCase
  - port/out/: ContentSourcePort, EventPublisherPort
- **application/service/**: ContentService (orchestration)
- **adapter/**:
  - in/web/: ContentController
  - in/scheduler/: DailyNewsletterScheduler (7 AM cron)
  - out/notion/: NotionAdapter (WebClient)
  - out/messaging/: RabbitMQEventPublisher

## Event Publishing
Publishes to RabbitMQ:
- `assine.content.ready` (exchange: `assine.events`)

## Configuration
- Port: 8087
- Notion: `NOTION_API_KEY`, `NOTION_DATABASE_ID`
- RabbitMQ: `SPRING_RABBITMQ_HOST`

## Key Constraints
- Hexagonal architecture with zero framework dependencies in domain.
- No local database - stateless content retrieval from Notion.
- Notion blocks supported: paragraph, heading_1, heading_2, bulleted_list_item, numbered_list_item.
- Structured JSON logging with correlation ID.
