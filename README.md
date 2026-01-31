# issuer-simulator

O **issuer-simulator** simula o **banco emissor** no fluxo de adquirência.
Ele é responsável por decidir se um pagamento é **autorizado ou negado**
com base em regras simples de saldo, bloqueio e cartão.

---

## 🎯 Propósito

No mundo real, após risco aprovado, a transação é enviada ao **emissor**
(banco do cliente), que decide se o pagamento pode ser autorizado.

Este serviço simula:
- autorização de pagamentos
- recusas por regras simples
- resposta assíncrona via eventos

Antes de publicar `payment.authorized`, o emissor consulta o ledger:
- GET /limits/pan/{panHash}
- GET /limits/users/{userId}
- GET /accounts/{accountId}/available-credit

Regras:
- autorização não consome limite de janela
- captura consome (via ledger consumer de payment.captured)

---

## 🧱 Arquitetura (Clean)

```
issuer-simulator
├── domain
│   └── model
│       └── IssuerDecision
├── application
│   └── usecase
│       └── AuthorizePaymentUseCase
├── entrypoints
│   └── kafka
│       └── IssuerEventsConsumer
└── infrastructure
    ├── messaging
    │   └── KafkaPublisher
    └── config
```

---

## 🔄 Comunicação (Kafka)

### Consome
- `payment.risk.approved`

### Produz
- `payment.authorized`
- `payment.declined`

---

## 🧠 Regras simuladas

- Valores muito altos → recusados
- PANs específicos → recusados
- Demais transações → autorizadas

---

## 📊 Observabilidade
- `/actuator/health`
- `/actuator/prometheus`
- Logs com correlationId

---

## ▶️ Como rodar

```bash
mvn clean spring-boot:run
```

ou

```bash
docker compose up --build
```

Testes:
```bash
mvn clean verify
```

---

## 🔗 Papel no fluxo E2E

```
acquirer-core
   ↓ (risk.approved)
issuer-simulator
   ↓ 
ledger-service
   ↓ (check-limits)
issuer-simulator
   ↓ (authorized | declined)
acquirer-core
```