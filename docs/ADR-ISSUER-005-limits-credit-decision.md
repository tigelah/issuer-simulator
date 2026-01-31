# ADR-ISSUER-002 — Decisão de autorização do emissor com limites (ledger) e motivo padronizado insufficient_funds
## Contexto

No fluxo de adquirência simulado, o emissor é responsável por decidir a autorização final. Para simular o mercado com mais fidelidade, precisamos:

Negar compras acima do limite disponível retornando um motivo padronizado (insufficient_funds);

Aplicar limite configurável por userId e/ou PAN (sem expor PAN, usando panHash);

Garantir que autorização não consuma crédito real: a reserva ocorre como HOLD, e o consumo real acontece somente após CAPTURE no ledger.

O emissor consome eventos payment.authorize.requested e emite payment.authorized ou payment.declined.

## Decisão

O issuer-simulator passa a consultar o ledger-service antes de autorizar:

- GET /accounts/{accountId}/available-credit

- GET /limits/users/{userId} (se existir userId)

- GET /limits/pan/{panHash} (fallback se não existir regra por usuário)

### O emissor avalia:

- Se amountCents > availableCents ⇒ publica payment.declined com reason="insufficient_funds".

- Se existir regra daily/monthly e a transação exceder a janela (pré-check) ⇒ reason="limit_exceeded".

- Caso contrário ⇒ publica payment.authorized.

- O emissor não grava estado financeiro (não escreve em banco/ledger); apenas decide e publica eventos.

## Consequências Positivas

Simula comportamento real do emissor: negação por falta de saldo/limite usando insufficient_funds.

Centraliza a fonte de verdade de crédito no ledger-service (auditável e consistente).

Evita “consumo fantasma”: autorização apenas reserva (HOLD), consumo real é no CAPTURE.

## Negativas / Trade-offs

O emissor fica dependente do ledger em tempo real (latência e disponibilidade).

Em caso de indisponibilidade do ledger, é necessário definir política (fail-close ou fail-open).

Para limites diários/mensais “perfeitos”, idealmente o emissor consultaria gasto acumulado; aqui foi adotado um pré-check simplificado (o consumo real será refletido no ledger na captura).

## Alternativas consideradas

Aplicar limite no acquirer-core
Rejeitado: a decisão de saldo/limite é responsabilidade do emissor e deve ser consistente com o ledger.

Consumir limite já na autorização
Rejeitado: viola o requisito “captura consome”; autorização é reserva.

Emitir eventos sem consultar ledger (simulação pura)
Rejeitado: perde realismo e não valida o EPIC PAG-01.

## Observações de implementação

O evento payment.authorize.requested deve carregar:

- accountId (obrigatório)

- userId (opcional)

- panHash (preferencial; não publicar PAN)

- panHash deve ser derivado por SHA-256 do PAN (apenas no boundary confiável; idealmente tokenização).

## Razões padronizadas:

- insufficient_funds (sem crédito disponível)

- limit_exceeded (limite diário/mensal ou limite configurado menor que a transação)

- risk_rejected (quando consumir payment.risk.rejected)

## Observabilidade:

logar correlationId, paymentId, accountId, decision, reason.