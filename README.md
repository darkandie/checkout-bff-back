# Checkout BFF

![CI](https://github.com/<seu-usuario>/checkout-bff/actions/workflows/ci.yml/badge.svg)

BFF (Backend for Frontend) do checkout de assinaturas. Monta o carrinho a partir dos produtos escolhidos (pré-checkout) e conclui o pagamento simulado (checkout), devolvendo ao frontend apenas o que cada tela precisa: pagamento, sucesso ou erro.

## Stack

- Java 21 e Spring Boot 3.5
- PostgreSQL com migrações Flyway
- Redis como cache dos serviços externos
- RabbitMQ para eventos de pagamento, com transactional outbox
- RestClient para consumir os serviços de produtos e meios de pagamento
- springdoc-openapi (Swagger UI)
- Docker e Docker Compose
- JUnit 5, Mockito, Testcontainers, ArchUnit e JaCoCo
- GitHub Actions, Terraform e Google Cloud (Cloud Run, Cloud SQL, Secret Manager, Artifact Registry)

## Arquitetura

O projeto segue ports and adapters. O domínio não conhece Spring, HTTP nem banco; os casos de uso dependem apenas de interfaces (ports), e cada integração vive no seu adapter.

```
src/main/java/com/carlosoliveira/checkout
├── domain                 regras de negócio puras (Cart, Product, PaymentMethod, eventos, erros)
├── application
│   ├── port/out           contratos com o mundo externo
│   ├── precheckout        caso de uso de pré-checkout
│   ├── checkout           caso de uso de checkout
│   └── outbox             relay dos eventos do outbox
├── adapter
│   ├── in/web             controllers, DTOs, contrato de erro e filtros HTTP
│   ├── in/scheduler       agendamento do relay do outbox
│   └── out
│       ├── catalog        cliente HTTP do serviço de produtos
│       ├── paymentmethod  cliente HTTP do serviço de meios de pagamento
│       ├── payment        gateway de pagamento simulado
│       ├── persistence    carrinhos e outbox no PostgreSQL
│       └── messaging      publicação dos eventos no RabbitMQ
├── mock                   serviços externos simulados (produtos e meios de pagamento)
└── config                 configuração de infraestrutura
```

Os serviços de produtos e de meios de pagamento são simulados como endpoints HTTP independentes em `/mocks/**`. O BFF os consome via HTTP pelos seus ports, exatamente como faria com serviços reais; basta apontar `PRODUCT_SERVICE_URL` e `PAYMENT_METHOD_SERVICE_URL` para outro endereço e desligar os mocks com `MOCKS_ENABLED=false`.

## Como rodar

Pré-requisitos: Java 21, Maven 3.9+ e Docker.

### Tudo em Docker

```bash
docker compose --profile app up --build
```

### API local, infraestrutura em Docker

```bash
docker compose up -d
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. O painel do RabbitMQ fica em `http://localhost:15672` (guest/guest).

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Para rodar sem Redis, use `CACHE_TYPE=none`.

## Testes

```bash
mvn test
mvn verify
```

`mvn test` roda os testes unitários e de contrato, sem dependências externas. `mvn verify` roda também os testes de integração contra um PostgreSQL real via Testcontainers (requer Docker) e falha se a cobertura de linhas ficar abaixo de 80%. O relatório de cobertura fica em `target/site/jacoco/index.html`.

| Camada | O que cobre |
| --- | --- |
| Domínio | Ciclo de vida do carrinho, transições de status, expiração, regras de cartão e preço |
| Casos de uso | Pré-checkout e checkout com Mockito: aprovação, recusa, todas as falhas de negócio, concorrência e falha do gateway |
| Adapters | Clientes HTTP com `MockRestServiceServer`, gateway simulado e geração do código Pix com CRC16 |
| Contrato HTTP | `@WebMvcTest` dos dois endpoints e invariantes do catálogo de erros: mesmo formato em todos os cenários |
| Arquitetura | ArchUnit garante as fronteiras de ports and adapters |
| Integração | Fluxos ponta a ponta com PostgreSQL e RabbitMQ: retentativa após recusa, pagamento duplicado, Pix, carrinho expirado, oito checkouts concorrentes do mesmo carrinho resultando em uma única aprovação e verificação de que o número do cartão nunca chega ao banco, recuperação de carrinho travado e entrega dos eventos do outbox |

## Integração contínua

O workflow [`ci.yml`](.github/workflows/ci.yml) roda em todo push na `main` e em pull requests: build, testes unitários e de integração, regras de arquitetura, gate de cobertura e build da imagem Docker. O Dependabot mantém dependências, actions e imagens base atualizadas.

## Deploy no GCP

A API é publicada no Cloud Run, com Cloud SQL (PostgreSQL), Secret Manager e Artifact Registry, tudo provisionado com Terraform em [`infra/terraform`](infra/terraform). O workflow [`deploy.yml`](.github/workflows/deploy.yml) publica uma nova revisão a cada push na `main` com o CI verde, autenticando via Workload Identity Federation e validando com um smoke test.

O passo a passo, as decisões de segurança e os custos estão em [`docs/deploy-gcp.md`](docs/deploy-gcp.md).

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| POST | `/operations/pre-checkout` | Cria o carrinho com produtos, preços e meios de pagamento |
| POST | `/operations/checkout` | Paga o carrinho com o meio escolhido |

Catálogo disponível: `basico` (R$ 19,90), `premium` (R$ 39,90) e `familia` (R$ 59,90).

## Simulando os cenários

O arquivo [`http/checkout.http`](http/checkout.http) tem todos os cenários prontos para IntelliJ ou para a extensão REST Client do VS Code. Com curl:

**1. Pré-checkout**

```bash
curl -s -X POST http://localhost:8080/operations/pre-checkout \
  -H 'Content-Type: application/json' \
  -d '{"products":[{"slug":"premium"}]}'
```

Guarde o `cartId` retornado.

**2. Pagamento aprovado com cartão**

```bash
curl -s -X POST http://localhost:8080/operations/checkout \
  -H 'Content-Type: application/json' \
  -d '{
    "cartId": "<cartId>",
    "paymentMethod": {
      "name": "Cartão de Crédito",
      "card": {"holderName": "Ana Souza", "number": "4111111111114242", "expiry": "12/28", "cvv": "123"}
    }
  }'
```

**3. Pagamento recusado**

Qualquer cartão terminado em `0002` é recusado (por exemplo `4000000000000002`). A resposta é `422` com `errorCode` `PAYMENT_DECLINED`. Um carrinho recusado pode ser pago novamente.

**4. Pagamento com Pix**

```bash
curl -s -X POST http://localhost:8080/operations/checkout \
  -H 'Content-Type: application/json' \
  -d '{"cartId": "<cartId>", "paymentMethod": {"name": "Pix"}}'
```

No sucesso, `payment.pixCode` traz um código Pix copia e cola simulado (BR Code com CRC16 válido) no lugar de `lastDigits`.

**5. Pagamento duplicado**

Repetir o checkout de um carrinho aprovado retorna `409` com `PAYMENT_ALREADY_APPROVED`, sem nova cobrança.

## Contrato de erro

Toda falha exibida pelo frontend usa o mesmo objeto:

```json
{
  "displayType": "BLOCK",
  "errorCode": "PAYMENT_DECLINED",
  "cartId": "8f6c1d1e-0c56-4f7e-9d0a-5b8f1f6f1c2a",
  "frontendError": {
    "code": "P-1001",
    "title": "Pagamento não aprovado",
    "description": "Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio.",
    "primaryButton": { "label": "Tentar novamente", "url": "RETRY" },
    "secondaryButton": { "label": "Trocar meio de pagamento", "url": "/checkout/pagamento" }
  }
}
```

Todas as chaves são sempre serializadas (inclusive `cartId` e `secondaryButton` nulos), para que o formato nunca varie.

| errorCode | HTTP | displayType | code |
| --- | --- | --- | --- |
| `INVALID_REQUEST` | 400 | BLOCK | P-0001 |
| `PAYMENT_DECLINED` | 422 | BLOCK | P-1001 |
| `PRODUCT_NOT_FOUND` | 422 | BLOCK | P-2001 |
| `CART_NOT_FOUND` | 404 | BLOCK | P-3001 |
| `CART_EXPIRED` | 410 | BLOCK | P-3002 |
| `PAYMENT_METHOD_NOT_ALLOWED` | 422 | INTERVENTION | P-4001 |
| `INVALID_PAYMENT_DATA` | 422 | INTERVENTION | P-4002 |
| `PAYMENT_ALREADY_APPROVED` | 409 | INTERVENTION | P-5001 |
| `PAYMENT_IN_PROGRESS` | 409 | INTERVENTION | P-5002 |
| `EXTERNAL_SERVICE_UNAVAILABLE` | 503 | BLOCK | P-9001 |
| `UNEXPECTED_ERROR` | 500 | BLOCK | P-9999 |

Falhas de negócio usam 4xx, como pede o contrato. Indisponibilidade de serviços externos e erros inesperados mantêm o mesmo objeto, mas com 503 e 500, para que monitoramento e retentativas de infraestrutura não confundam falha do servidor com erro do cliente.

## Decisões de projeto

- **Snapshot de preço no carrinho.** O preço é gravado no pré-checkout e o checkout cobra exatamente esse valor; o cliente nunca envia preço.
- **Concorrência e pagamento duplicado.** Antes de cobrar, o carrinho é reivindicado com um update condicional (`CREATED/DECLINED -> PROCESSING`). Duas requisições simultâneas para o mesmo carrinho não geram duas cobranças: só uma vence, a outra recebe `PAYMENT_IN_PROGRESS` ou `PAYMENT_ALREADY_APPROVED`.
- **Chamada ao gateway fora de transação.** A cobrança não segura conexão de banco aberta. Se o gateway falhar de forma inesperada, o carrinho volta ao status anterior e pode ser pago de novo.
- **Dados sensíveis.** Número completo do cartão e CVV não são persistidos nem logados. Apenas os quatro últimos dígitos são gravados, e os objetos de cartão mascaram o próprio `toString`.
- **Cache tolerante a falhas.** Produtos e meios de pagamento ficam em cache no Redis com TTL configurável. Se o Redis cair, o BFF registra o erro e consulta os serviços diretamente.
- **Carrinho com expiração.** Carrinhos expiram após `CART_TTL` (30 minutos por padrão).
- **Recuperação de pagamentos travados.** Cada cobrança abre uma tentativa de pagamento com id e horário de início, que funciona como um lease. Se a instância cair no meio da cobrança, o carrinho não fica preso em `PROCESSING`: passado o `PAYMENT_PROCESSING_TIMEOUT` (2 minutos), o próximo checkout assume o carrinho com o mesmo update condicional atômico. A recuperação acontece sob demanda, no próprio checkout, e por isso funciona no Cloud Run, onde a CPU fica restrita entre requisições.
- **Cobrança idempotente.** O id da tentativa vai como chave de idempotência para o gateway. Uma cobrança repetida na mesma tentativa (como na recuperação acima) devolve o resultado original em vez de cobrar de novo; uma nova tentativa depois de uma recusa é cobrada normalmente.
- **Eventos com transactional outbox.** Aprovação e recusa geram os eventos `payment.approved` e `payment.declined`, gravados na tabela `outbox_events` na mesma transação que atualiza o carrinho. Um relay agendado publica no exchange `checkout.events` do RabbitMQ usando `FOR UPDATE SKIP LOCKED` (várias instâncias sem disputa) e publisher confirms, marcando o evento como publicado só depois da confirmação do broker. A entrega é at-least-once, e o `messageId` de cada mensagem é o id do evento, para deduplicação nos consumidores. O checkout continua síncrono, como o contrato exige; os eventos servem a quem vem depois (ativação da assinatura, notificações).
- **Rastreabilidade.** Cada requisição recebe (ou propaga) um `X-Correlation-Id`, presente em todas as linhas de log.

## Configuração

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Porta HTTP |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/checkout` | URL JDBC do PostgreSQL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `checkout` / `checkout` | Credenciais do banco |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Endereço do Redis |
| `CACHE_TYPE` | `redis` | `redis` ou `none` |
| `CACHE_TTL` | `10m` | TTL do cache de produtos e meios de pagamento |
| `CART_TTL` | `30m` | Validade do carrinho |
| `MOCKS_ENABLED` | `true` | Habilita os serviços externos simulados |
| `PRODUCT_SERVICE_URL` | mock local | URL do serviço de produtos |
| `PAYMENT_METHOD_SERVICE_URL` | mock local | URL do serviço de meios de pagamento |
| `PAYMENT_DECLINED_CARD_SUFFIX` | `0002` | Final de cartão recusado pelo gateway simulado |
| `PAYMENT_PROCESSING_TIMEOUT` | `2m` | Tempo até um pagamento em processamento poder ser recuperado |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | Endereço do RabbitMQ |
| `EVENTS_PUBLISHER` | `rabbit` | `rabbit` ou `log` (apenas registra os eventos) |
| `OUTBOX_RELAY_ENABLED` | `true` | Liga o relay agendado do outbox |
| `OUTBOX_RELAY_INTERVAL_MS` | `2000` | Intervalo entre execuções do relay |
| `OUTBOX_BATCH_SIZE` | `50` | Eventos publicados por execução |
