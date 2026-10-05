# Jornada do usuário na API: valores por hora e rateio

Esta jornada descreve o cadastro das tarifas opcionais, o preenchimento do formulário de trabalho e a confirmação do cálculo. Todos os endpoints usam a organização do usuário autenticado; não envie `organizationId` para escolher a conta. Requisições de escrita, inclusive a consulta de prévia via POST, seguem a autenticação por cookie e a proteção CSRF existentes.

## 1. Configurar o valor/hora padrão da organização

Consultar `GET /api/v1/accounts/me/preferences` (também disponível em `GET /api/v1/accounts/me`). A resposta inclui `defaultHourlyRate`, opcional, junto de `timezone`, `defaultWorkStatus`, `currencyCode` e `today`.

Atualizar com `PUT /api/v1/accounts/me/preferences`:

```json
{
  "timezone": "Europe/London",
  "defaultWorkStatus": "SCHEDULED",
  "defaultHourlyRate": 30.00
}
```

Sucesso: HTTP 204. O valor é compartilhado pela organização. Na atualização, omitir `defaultHourlyRate` preserva o cadastro; enviar `null` o remove. `timezone` e `defaultWorkStatus` continuam obrigatórios nesta operação.

Ao abrir o cadastro de trabalho, o frontend deve consultar as preferências e preencher `hourlyRate` quando houver padrão. O usuário pode editar esse campo. A API também resolve o padrão quando `hourlyRate` é omitido ou nulo na criação ou na prévia. Um valor explícito prevalece; valor explícito inválido não usa o padrão como alternativa. Sem valor explícito e sem preferência, a API responde HTTP 400.

## 2. Cadastrar o valor/hora opcional do colaborador

`POST /api/v1/collaborators`:

```json
{
  "name": "Ana",
  "phone": "+44 123456789",
  "hourlyRate": 20.00
}
```

Sucesso: HTTP 201 com o ID, mantendo o contrato de criação existente. `GET /api/v1/collaborators`, `GET /api/v1/collaborators/{id}` e a resposta da edição incluem `hourlyRate` quando cadastrado.

Em `PUT /api/v1/collaborators/{id}`, omitir `hourlyRate` preserva a tarifa; enviar `null` remove; enviar um número substitui. O nome continua obrigatório. Colaboradores inativos não podem ser editados nem incluídos em novos trabalhos.

Os dois valores opcionais usam GBP, devem ser maiores que zero e ter até duas casas decimais. Não há conversão de moeda. Zero, negativo ou mais casas retornam HTTP 400.

## 3. Consultar a prévia antes de salvar

`POST /api/v1/work-orders/pricing-preview` recebe os mesmos dados da criação de trabalho. IDs abaixo são exemplos e devem ser substituídos por cadastros da organização.

```json
{
  "customerId": "11111111-1111-1111-1111-111111111111",
  "serviceDate": "2026-10-05",
  "startTime": "10:00:00",
  "description": "Limpeza",
  "contractedHours": 4.00,
  "hourlyRate": 30.00,
  "participantIds": [
    "22222222-2222-2222-2222-222222222222",
    "33333333-3333-3333-3333-333333333333"
  ],
  "initialStatus": "SCHEDULED"
}
```

Esta consulta valida os dados, cliente, local e participantes, mas não grava trabalho, série ou lançamento financeiro. O exemplo considera Ana com £20/h e Bruno sem tarifa fixa:

```json
{
  "pricingFingerprint": "<identificador retornado pela API>",
  "currencyCode": "GBP",
  "contractedHours": 4.00,
  "hourlyRate": 30.00,
  "totalAmount": 120.00,
  "allocationPolicyVersion": 2,
  "requiresConfirmation": true,
  "canCreate": true,
  "baseTotal": 100.00,
  "surplusAmount": 20.00,
  "excessAmount": 0.00,
  "participantCount": 2,
  "participants": [
    {
      "collaboratorId": "22222222-2222-2222-2222-222222222222",
      "inclusionPosition": 0,
      "appliedHourlyRate": 20.00,
      "rateSource": "COLLABORATOR",
      "individualHours": 2.00000000,
      "baseAmount": 40.00,
      "surplusAmount": 10.00,
      "allocatedAmount": 50.00
    },
    {
      "collaboratorId": "33333333-3333-3333-3333-333333333333",
      "inclusionPosition": 1,
      "appliedHourlyRate": 30.00,
      "rateSource": "WORK_ORDER",
      "individualHours": 2.00000000,
      "baseAmount": 60.00,
      "surplusAmount": 10.00,
      "allocatedAmount": 70.00
    }
  ]
}
```

### Mostrar a divisão ao usuário

Quando `requiresConfirmation=true`, apresentar o aviso antes de enviar a criação:

> Os colaboradores têm tarifas diferentes da tarifa do trabalho. O preço é £120. As 4 horas são divididas entre 2 participantes: Ana tem base de £40 e Bruno de £60. A sobra de £20 será dividida igualmente, acrescentando £10 a cada um. Ana receberá £50 e Bruno £70.

Oferecer **aceitar**, **ajustar** ou **desistir**. Ajustar significa mudar os dados do trabalho ou os cadastros e consultar uma nova prévia. Desistir não envia a criação. Não há opção para ignorar tarifas fixas somente nesta ordem.

### Fórmula e centavos

- Preço: horas contratadas × tarifa do trabalho, arredondado para centavos com `HALF_EVEN`, como no comportamento anterior.
- Havendo tarifa fixa diferente: base individual = horas contratadas × tarifa aplicada ÷ quantidade total de participantes. A base é truncada em centavos, sem arredondar as horas intermediárias.
- Sem tarifa própria, aplicar a tarifa do trabalho à base daquele participante.
- Dividir a sobra entre todos, inclusive quem possui tarifa própria. Centavos restantes vão aos primeiros participantes na ordem enviada em `participantIds`.
- `individualHours` mostra uma aproximação com oito casas; não deve ser usado para recalcular o resultado no frontend. O resultado monetário da API é o definitivo.
- Sem tarifas fixas diferentes da tarifa do trabalho, manter o rateio anterior e a política versão 1. A nova regra usa versão 2.

O valor cadastrado é a base da remuneração; o adicional da sobra pode elevar a remuneração efetiva. A soma das parcelas aprovadas continua igual ao preço do trabalho. Cadastrar um colaborador sem tarifa própria continua permitido.

## 4. Aceitar e criar o trabalho

Enviar `POST /api/v1/work-orders` com os mesmos dados consultados na prévia e acrescentar:

```json
{
  "acceptedPricingFingerprint": "<copiar pricingFingerprint da prévia aceita>"
}
```

Esse trecho é um campo adicional do pedido completo, não um pedido separado. Sucesso: HTTP 201 com o trabalho. `assignments` inclui `allocatedAmount`, `appliedHourlyRate`, `fixedRate`, `baseAmount` e `surplusAmount`. O booleano `fixedRate` indica se a tarifa veio do colaborador.

A confirmação é exigida quando ao menos uma tarifa fixa difere da tarifa da ordem. Quando todas coincidem ou todos estão sem tarifa própria, a criação antiga continua funcionando sem consultar prévia ou enviar confirmação.

A API recalcula antes de salvar. Se algum dado confirmado ou tarifa aplicada mudar, ou se faltar confirmação obrigatória, responde HTTP 409 com `code=PRICING_ACCEPTANCE_REQUIRED` e `pricingPreview` atualizado. Mostrar a nova conta ao usuário e obter nova aceitação; não reenviar confirmação automaticamente. Quando um identificador é enviado, ele é validado mesmo que a confirmação não seja obrigatória.

O identificador confirma os dados e condições calculadas, incluindo organização, ordem dos participantes, tarifas, preço e status inicial resolvido. Não reserva tarifas e não funciona como chave de idempotência da criação.

### Bases acima do preço

Se Ana passar a £40/h no mesmo trabalho, as bases serão £80 + £60 = £140, acima do preço de £120. A prévia responde HTTP 200 com `canCreate=false`, `baseTotal=140.00` e `excessAmount=20.00`. As parcelas exibidas nesse estado são as bases, não pagamentos aprovados. A interface deve pedir ajuste e impedir a ação de aceitar.

Tentar criar nessa situação retorna HTTP 422, `code=PRICING_BASES_EXCEED_TOTAL` e `pricingPreview`. Mesmo enviar o identificador correto não permite salvar. Não há redução automática das bases.

Os erros de precificação usam o formato `ProblemDetail` existente, com as propriedades adicionais `code` e `pricingPreview`. Nenhuma criação parcial ocorre nos casos de conflito ou excesso.

## 5. Criar uma série recorrente

Usar a mesma prévia de trabalho, com `serviceDate` igual ao futuro `startsOn` da série e com as mesmas condições. Em `POST /api/v1/recurrence-series`, enviar o contrato existente (`frequency`, `startsOn`, `endsOn` opcional, cliente, local, horário, descrição, horas, tarifa, participantes e status) e `acceptedPricingFingerprint` da prévia aceita.

`hourlyRate` também pode ser omitido ou nulo na criação da série para usar a preferência. `initialStatus` omitido usa a preferência de status vigente. Mantenha as condições resolvidas da prévia até a confirmação; a API detectará mudanças relevantes.

A resposta da série e sua consulta por ID apresentam `conditions.frozenPricing`, contendo `totalAmount`, `allocationPolicyVersion` e `assignments` com as parcelas e suas bases. As condições aprovadas ficam congeladas na série e são repetidas nas ocorrências geradas, inclusive posteriormente. Reagendamento que cria uma série sucessora preserva esse snapshot.

Mudar a tarifa do colaborador ou a preferência da organização vale para novas ordens avulsas e novas séries. Não altera ordens já criadas nem ocorrências futuras de uma série existente. A geração automática não exige nova confirmação.

## 6. Consultar pagamentos e relatórios

Pagamentos, saldos e relatórios usam `allocatedAmount` persistido. No exemplo, o limite devido à Ana é £50 e ao Bruno £70; o preço do cliente permanece £120. Alterar cadastros depois da criação não modifica essas obrigações. Concluir, cancelar e reagendar preservam as condições financeiras conforme as regras operacionais existentes.

A migração V16 é aditiva: colaboradores e organizações existentes começam sem tarifa opcional. Ordens históricas mantêm suas parcelas e podem não ter os campos de detalhamento preenchidos. Séries anteriores sem snapshot continuam gerando pelo rateio antigo, independentemente de novas tarifas nos cadastros. O frontend deve aceitar a ausência desse detalhamento histórico.
