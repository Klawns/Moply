# Contratos HTTP

Os DTOs HTTP ficam em `infra.web.dto.request` e `infra.web.dto.response` nos módulos de trabalhos, recorrência, pagamentos e relatórios. Os casos de uso mantêm seus contratos internos. O OpenAPI em `/v3/api-docs` descreve os tipos usados pelo frontend.

## Trabalhos e recorrência

Criação de trabalho e simulação de preço recebem o mesmo payload:

```json
{
  "serviceDate": "2026-10-06",
  "conditions": {
    "customerId": "11111111-1111-1111-1111-111111111111",
    "customerLocationId": null,
    "startTime": "09:00:00",
    "description": "Limpeza",
    "contractedHours": 4,
    "hourlyRate": 30,
    "participantIds": ["22222222-2222-2222-2222-222222222222"],
    "initialStatus": "SCHEDULED"
  },
  "acceptedPricingFingerprint": null
}
```

Os UUIDs são ilustrativos e devem corresponder a registros da conta. `hourlyRate` e `initialStatus` omitidos ou nulos usam as preferências existentes. `customerLocationId`, horário e descrição mantêm sua opcionalidade. O fingerprint aceito fica na raiz, separado das condições.

Criação de série recebe `frequency`, `period: { startsOn, endsOn }`, o mesmo `conditions` e `acceptedPricingFingerprint`. `endsOn` pode ser nulo. A primeira ocorrência usa `period.startsOn`.

Respostas de trabalhos agrupam cliente em `customer: { id, name }`, data e horário em `schedule`, valores em `pricing` e vínculo recorrente em `recurrence: { seriesId, occurrenceDate }`. `recurrence` é nulo em trabalhos avulsos. Alocações continuam em `assignments`, preservando a ordem dos participantes.

A prévia agrupa valores em `pricing` e totais do rateio em `summary: { baseTotal, surplusAmount, excessAmount }`. Fingerprint, permissões de criação/confirmação e participantes permanecem na raiz. Erros de preço 409/422 incluem `code` e `pricingPreview` com exatamente o mesmo contrato da simulação.

Respostas de séries têm `id`, `frequency`, `period`, `conditions` e `lineage`. Dentro de `conditions`, horas, tarifa e moeda ficam em `pricing`; `frozenPricing` conserva o total, a versão da política e as alocações aprovadas. `lineage` contém `familyId`, `previousSeriesId`, `firstPosition` e `untilPosition`.

O histórico conserva informações da operação e agrupa o alvo em `target: { position, seriesId, occurrenceDate }` e a alteração de data em `serviceDateChange: { before, after }`.

## Pagamentos e relatórios

Pagamentos agrupam auditoria em `recording: { at, by }` e estorno em `reversal: { at, by, reason }`. `reversal` é nulo antes do estorno. O resumo de pagamentos do colaborador e cada trabalho compartilham `balance: { allocatedAmount, recordedAmount, remainingAmount, requiresAttention }`.

Relatórios retornam `period: { from, to }`, `context: { timezone, currencyCode, referenceDate }`, `summary` com os totais e as coleções paginadas. O relatório de recebimentos não tem data de referência, representada por `null`. Os itens usam `customer: { id, name }` e, quando aplicável, `collaborator: { id, name }`.

Paginação mantém `content`, `page`, `size`, `totalElements` e `totalPages`. Parâmetros de consulta, rotas e operation IDs permanecem iguais.

## Atualização do frontend

Esta alteração substitui os contratos anteriores nos endpoints `/api/v1`. Com o backend atualizado e OpenAPI habilitado, execute `npm run api:generate` no frontend. A geração atualiza modelos e serviços; código que acessava campos antigos deve usar os novos caminhos, por exemplo `work.pricing.totalAmount` e `work.customer.name`.
