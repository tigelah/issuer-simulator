# ADR-004 — Simulador de Emissor Independente

## Status
Aceito

## Contexto
A decisão final de autorização pertence ao banco emissor.

## Decisão
Criar um serviço separado para simular o emissor,
com decisões assíncronas baseadas em eventos.

## Consequências
- Fluxo realista
- Isolamento de responsabilidades
- Fácil simulação de falhas

## Alternativas
- Autorizar direto no adquirente (rejeitado)
