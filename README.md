# Checkout BFF

API de checkout de assinaturas construída como **BFF (Backend for Frontend)**. Ela monta o carrinho a partir dos planos escolhidos e conclui um pagamento simulado com cartão de crédito, débito ou Pix, devolvendo ao frontend exatamente o que cada tela precisa: pagamento, sucesso ou erro.

O projeto roda inteiro na sua máquina com **um único comando**, sem nenhuma conta em nuvem.

---

## Destaques

- **Dois endpoints, contrato estável:** pré-checkout e checkout, com um único formato de erro para todas as falhas, pronto para o frontend exibir.
- **Regras de negócio completas:** carrinho só pode ser pago se existir, não estiver expirado e não tiver sido pago; o meio de pagamento precisa ser um dos oferecidos para aquele carrinho; Pix não aceita dados de cartão; cartão vencido é recusado.
- **Sem cobrança duplicada:** cliques repetidos ou requisições simultâneas no mesmo carrinho geram uma única aprovação. Um teste dispara 8 pagamentos ao mesmo tempo e verifica isso.
- **Recuperação de pagamentos interrompidos:** se a aplicação cair no meio de uma cobrança, o carrinho é liberado automaticamente depois de 2 minutos, sem risco de cobrar duas vezes.
- **Dados de cartão protegidos:** número completo e CVV nunca são gravados nem aparecem em logs; só os 4 últimos dígitos são guardados.
- **Código Pix real:** o Pix aprovado devolve um "copia e cola" no formato oficial do Banco Central, com checksum válido.
- **Eventos de pagamento:** aprovações e recusas são publicadas no RabbitMQ de forma confiável (padrão transactional outbox).
- **Serviços externos simulados via HTTP:** catálogo de produtos e meios de pagamento são consumidos como serviços de verdade, com cache no Redis.
- **Testes em todas as camadas:** unitários, contrato da API, regras de arquitetura e integração com PostgreSQL e RabbitMQ reais, mais esteira de CI no GitHub Actions.

**Tecnologias:** Java 21, Spring Boot 3.5, PostgreSQL, Redis, RabbitMQ, Docker, JUnit 5, Mockito e Testcontainers.

---

## Como rodar

### Pré-requisito

Apenas o **[Docker Desktop](https://www.docker.com/products/docker-desktop/)** instalado e aberto. Não é preciso instalar Java nem Maven.

### Subir o projeto

Na pasta do projeto:

```bash
docker compose --profile app up --build
```

A primeira execução leva alguns minutos, porque baixa as imagens e compila a aplicação. Quando aparecer no log a linha `Started CheckoutBffApplication`, a API está pronta em **http://localhost:8080**.

Esse comando sobe tudo de uma vez: a API, o PostgreSQL, o Redis e o RabbitMQ.

### Parar

```bash
docker compose --profile app down
```

Para apagar também os dados do banco, use `docker compose --profile app down -v`.

---

## Como testar a API

Existem três jeitos. Escolha o mais confortável.

### 1. Swagger (mais fácil, direto no navegador)

Abra **http://localhost:8080/swagger-ui.html**. Lá estão os dois endpoints, com os formatos de requisição e de todas as respostas possíveis. Clique em **Try it out** para executar.

### 2. Arquivo de requisições prontas

O arquivo [`http/checkout.http`](http/checkout.http) tem todos os cenários em sequência (aprovação, recusa, Pix, pagamento duplicado, carrinho inexistente). Ele abre direto no **IntelliJ IDEA** ou no **VS Code** com a extensão *REST Client*: é só clicar em executar em cada requisição.

### 3. Pelo terminal com curl

Os comandos abaixo são para macOS e Linux (no Windows, use o Swagger ou o arquivo `.http`).

**Passo 1: criar o carrinho**

```bash
curl -X POST http://localhost:8080/operations/pre-checkout \
  -H 'Content-Type: application/json' \
  -d '{"products":[{"slug":"premium"}]}'
```

Copie o `cartId` da resposta e guarde numa variável:

```bash
export CART_ID=cole-o-cartId-aqui
```

**Passo 2: pagar com cartão**

```bash
curl -X POST http://localhost:8080/operations/checkout \
  -H 'Content-Type: application/json' \
  -d '{
    "cartId": "'"$CART_ID"'",
    "paymentMethod": {
      "name": "Cartão de Crédito",
      "card": { "holderName": "Ana Souza", "number": "4111111111114242", "expiry": "12/30", "cvv": "123" }
    }
  }'
```

**Passo 3: tentar pagar de novo o mesmo carrinho**

```bash
curl -X POST http://localhost:8080/operations/checkout \
  -H 'Content-Type: application/json' \
  -d '{"cartId": "'"$CART_ID"'", "paymentMethod": {"name": "Pix"}}'
```

A resposta é um erro `409` avisando que o pagamento já foi aprovado, e nenhuma nova cobrança é feita.

### Dados para teste

**Planos disponíveis**

| Slug | Plano | Preço |
| --- | --- | --- |
| `basico` | Plano Básico | R$ 19,90 |
| `premium` | Plano Premium | R$ 39,90 |
| `familia` | Plano Família | R$ 59,90 |

**Meios de pagamento:** `Cartão de Crédito`, `Cartão de Débito` e `Pix`.

**Cartões**

| Cenário | Número | Resultado |
| --- | --- | --- |
| Aprovado | `4111111111114242` (qualquer número de 13 a 19 dígitos) | Pagamento aprovado |
| Recusado | `4000000000000002` (qualquer número terminado em `0002`) | Pagamento recusado; o carrinho pode tentar de novo |

A validade deve estar no formato `MM/AA` e no futuro (por exemplo `12/30`), e o CVV deve ter 3 ou 4 dígitos.

---

## Endpoints

### `POST /operations/pre-checkout`

Recebe os planos escolhidos e devolve o carrinho com preços e meios de pagamento disponíveis.

**Requisição**

```json
{ "products": [{ "slug": "premium" }] }
```

**Resposta `200`**

```json
{
  "cartId": "8f6c1d1e-0c56-4f7e-9d0a-5b8f1f6f1c2a",
  "products": [
    {
      "slug": "premium",
      "name": "Plano Premium",
      "description": "Assinatura mensal com catálogo completo",
      "price": 39.90,
      "currency": "BRL"
    }
  ],
  "paymentMethods": [
    { "name": "Cartão de Crédito" },
    { "name": "Cartão de Débito" },
    { "name": "Pix" }
  ]
}
```

### `POST /operations/checkout`

Paga o carrinho com o meio escolhido. Os dados do cartão só são enviados para crédito e débito.

**Requisição com cartão**

```json
{
  "cartId": "8f6c1d1e-0c56-4f7e-9d0a-5b8f1f6f1c2a",
  "paymentMethod": {
    "name": "Cartão de Crédito",
    "card": { "holderName": "Ana Souza", "number": "4111111111114242", "expiry": "12/30", "cvv": "123" }
  }
}
```

**Requisição com Pix**

```json
{ "cartId": "8f6c1d1e-0c56-4f7e-9d0a-5b8f1f6f1c2a", "paymentMethod": { "name": "Pix" } }
```

**Resposta `200` (cartão)**

```json
{
  "cartId": "8f6c1d1e-0c56-4f7e-9d0a-5b8f1f6f1c2a",
  "status": "APROVADO",
  "paidAt": "2026-11-02T20:15:30Z",
  "payment": {
    "method": "Cartão de Crédito",
    "lastDigits": "4242",
    "amount": 39.90,
    "currency": "BRL"
  },
  "products": [
    { "slug": "premium", "name": "Plano Premium", "price": 39.90 }
  ]
}
```

No Pix, o campo `lastDigits` dá lugar a `pixCode`, com o código "copia e cola".

### Formato de erro

Toda falha, de qualquer endpoint, volta sempre com o mesmo objeto. Exemplo de pagamento recusado (`422`):

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

Principais erros:

| Situação | HTTP | `errorCode` |
| --- | --- | --- |
| Requisição inválida (campo faltando, formato errado) | 400 | `INVALID_REQUEST` |
| Plano inexistente | 422 | `PRODUCT_NOT_FOUND` |
| Carrinho inexistente | 404 | `CART_NOT_FOUND` |
| Carrinho expirado (30 minutos) | 410 | `CART_EXPIRED` |
| Meio de pagamento não oferecido para o carrinho | 422 | `PAYMENT_METHOD_NOT_ALLOWED` |
| Dados de pagamento inválidos (cartão no Pix, cartão vencido) | 422 | `INVALID_PAYMENT_DATA` |
| Pagamento recusado | 422 | `PAYMENT_DECLINED` |
| Carrinho já pago | 409 | `PAYMENT_ALREADY_APPROVED` |
| Pagamento do carrinho em andamento | 409 | `PAYMENT_IN_PROGRESS` |

A lista completa está no Swagger e em [`docs/architecture.md`](docs/architecture.md).

---

## Outras páginas úteis

Com o projeto rodando:

| Endereço | O que é |
| --- | --- |
| http://localhost:8080/swagger-ui.html | Documentação interativa da API |
| http://localhost:8080/actuator/health | Status da aplicação |
| http://localhost:15672 | Painel do RabbitMQ (usuário `guest`, senha `guest`). Em **Queues**, a fila `checkout.payment-approved` recebe um evento a cada pagamento aprovado |

---

## Rodando os testes

Requer **Java 21**, **Maven 3.9+** e o **Docker** aberto (os testes de integração sobem PostgreSQL e RabbitMQ em containers).

```bash
mvn test      # testes unitários e de contrato, rápidos
mvn verify    # todos os testes, incluindo integração, com relatório de cobertura
```

O relatório de cobertura fica em `target/site/jacoco/index.html`.

---

## Rodando a API fora do Docker (opcional)

Para desenvolver ou depurar pela IDE, suba só a infraestrutura com Docker e rode a aplicação localmente com Java 21 e Maven:

```bash
docker compose up -d
mvn spring-boot:run
```

---

## Problemas comuns

| Sintoma | Solução |
| --- | --- |
| `Cannot connect to the Docker daemon` | O Docker Desktop não está aberto. Abra e espere o ícone ficar estável. |
| `port is already allocated` | Outro programa usa uma das portas 8080, 5432, 6379, 5672 ou 15672 (por exemplo, um PostgreSQL local). Pare esse programa e rode de novo. |
| A primeira subida demora | É normal: o Docker está baixando imagens e dependências. As próximas são bem mais rápidas. |
| `CART_EXPIRED` ao pagar | O carrinho vale 30 minutos. Crie outro no pré-checkout. |

---

## Estrutura do projeto

```
├── src/main/java/...      código da aplicação
│   ├── domain             regras de negócio (carrinho, pagamento, erros)
│   ├── application        casos de uso: pré-checkout e checkout
│   ├── adapter            API HTTP, banco, cache, mensageria e gateway simulado
│   └── mock               serviços externos simulados (produtos e meios de pagamento)
├── src/test/java/...      testes
├── http/checkout.http     requisições prontas para testar
├── docker-compose.yml     ambiente local completo
├── docs/                  documentação técnica
└── infra/terraform        infraestrutura como código para o Google Cloud
```

**Quer os detalhes técnicos?** A arquitetura, as decisões de projeto e todas as configurações estão em [`docs/architecture.md`](docs/architecture.md). O projeto também já tem infraestrutura como código e esteira de deploy para o Google Cloud, descritas em [`docs/deploy-gcp.md`](docs/deploy-gcp.md); ela não está hospedada, para que a avaliação não dependa de nenhum serviço pago.
