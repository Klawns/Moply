# Moply

## Testes de migrations PostgreSQL/Flyway

Requisitos: JDK 21, Docker em execução e acesso ao registry para baixar as imagens
do Testcontainers. Não é necessário iniciar o Compose nem configurar um banco local.

```sh
./mvnw -Dtest=CustomerOrderMigrationTest clean test
./mvnw spring-javaformat:validate
./mvnw clean verify
```

`CustomerOrderMigrationTest` usa PostgreSQL `17.6-bookworm`, também fixado no
`compose.yaml`, e as migrations reais de `src/main/resources/db/migration`.
O Testcontainers cria um container descartável com porta atribuída dinamicamente;
cada cenário usa um schema vazio e isolado, removido ao terminar. O teste não usa
o banco de desenvolvimento, Hibernate ou artefatos antigos de `target`.

Os cenários cobrem V1–V6 desde vazio, homônimos na transição V4→V6, rejeição de
nomes inválidos com rollback e nova tentativa da V5, e preservação do vínculo e
das restrições após V6. O nome atual do cliente é resolvido pelo vínculo.
As fixtures SQL representam o esquema histórico, que as factories de domínio
atuais já não representam.

A suíte integra o `clean verify` da CI e falha se Docker não estiver disponível;
não há skip automático. Testes de domínio/aplicação e algumas integrações usam H2 conforme o perfil
`test`; contas, colaboradores e trabalhos também têm integrações PostgreSQL. A versão fixada é a referência deste baseline, não uma declaração sobre
a versão usada em produção.

## Contas, autenticação e isolamento — PRs 2 e 3

V7 cria contas e gestores; V8 isola clientes, locais e ordens por conta, com FKs
compostas e versão do agregado cliente. V1–V7 permanecem intactas. V8 exige as
três tabelas operacionais vazias: se houver dados, falha com diagnóstico sem
apagá-los ou atribuir proprietário fictício. Reinicialize explicitamente apenas
o banco temporário antes de aplicá-la. A aplicação não limpa o banco ao iniciar.
Os testes PostgreSQL usam containers descartáveis e não alteram o banco local.

Configure `MOPLY_JWT_SECRET` com um segredo aleatório em Base64 de pelo menos
32 bytes, por exemplo gerado localmente com `openssl rand -base64 32`. Forneça-o
por variável de ambiente ou secret manager; não o versione nem registre tokens,
senhas ou o cabeçalho Cookie em logs. Todas as instâncias devem compartilhar a
mesma chave. `MOPLY_JWT_ISSUER` e `MOPLY_JWT_AUDIENCE` têm defaults `moply` e
`moply-api`. A chave presente no perfil de testes é pública e exclusiva de testes.

Fluxo HTTP, preservando cookies entre requisições:

1. `GET /api/v1/auth/csrf` retorna `headerName` e `token` e emite cookie CSRF
   separado. Envie o cabeçalho retornado e o cookie nas mutações, inclusive
   cadastro, login e logout. O token CSRF não autentica.
2. `POST /api/v1/accounts` recebe `name`, `timezone`, `email` e `password` e
   retorna 201 com `organizationId` e `userId`, sem autenticar automaticamente.
   Cadastro atômico, um gestor por conta, e-mail normalizado e único, BCrypt;
   senha de no mínimo 12 caracteres e no máximo 72 bytes UTF-8.
3. `POST /api/v1/auth/login` recebe form-urlencoded (`email`, `password`). Retorna
   204 com JWT somente no cookie `MOPLY_AUTH`, ou 401 sem redirect. Obtenha novo
   CSRF após login. JWT nunca aparece no corpo da resposta.
4. `GET /api/v1/auth/me` retorna IDs/e-mail. `GET /api/v1/accounts/me` e
   `/api/v1/accounts/me/preferences` retornam preferências e hoje no fuso da conta.
   `PUT /api/v1/accounts/me/preferences` altera `timezone` e `defaultWorkStatus`
   (`SCHEDULED`/`COMPLETED`), retorna 204. GBP permanece fixa.
5. APIs de clientes, locais e `/api/v1/work-orders` estão liberadas para a
   conta autenticada. Conta enviada no payload não muda o escopo. Consultas,
   alterações e referências a recursos alheios retornam 404. Trabalhos não têm exclusão pública.
   Atualizações concorrentes do cliente/locais retornam 409; recarregue e tente novamente.
6. `POST /api/v1/auth/logout`, com CSRF, remove cookies e retorna 204. Obtenha
   novo CSRF antes do próximo login.

JWT HS256 tem validade absoluta de **30 minutos da emissão**, sem renovação
por atividade, refresh token, blacklist ou `auth_version`. **Logout não revoga
uma cópia do JWT: ela continua válida até expirar.** A chave persistente permite
que tokens sobrevivam a reinícios. Trocar a chave invalida todos os tokens.
Assinatura, emissor, audiência, tempos e vínculo atual gestor/conta são validados.
Apenas o cookie autentica: Bearer, query string e corpo não são alternativas.

Cookies usam `HttpOnly`, `SameSite=Lax`, caminho `/api/v1` e não têm `Domain`.
Em produção ambos exigem `Secure`; servir interface/API na mesma origem HTTPS.
Não há sessão HTTP de autenticação ou CSRF. CSRF ausente/inválido retorna 403;
sem autenticação retorna 401 quando a requisição passa pela validação CSRF.

Verificação:

```sh
./mvnw -Dtest='AccountApiIntegrationTest,JwtCookieServiceTest,TenantMigrationTest,CustomerRepositoryIntegrationTest,WorkOrderIntegrationTest' test
./mvnw spring-javaformat:validate
./mvnw clean verify
```

A suíte mantém os cenários históricos dos PRs 1/2 e acrescenta duas contas,
credenciais por cookie, expiração, CSRF, concorrência de locais e V8 real em
PostgreSQL. Os testes antigos de contrato HTTP sem filtros usam um principal
explícito; `AccountApiIntegrationTest` cobre a cadeia de segurança real.

### Separação entre accounts e auth

- `accounts` mantém o cadastro atômico da conta e do gestor, os dados dos usuários e as preferências da conta.
- `auth` concentra a configuração do Spring Security, autenticação, JWT, cookies, CSRF e os endpoints `/api/v1/auth`. Login e logout são atendidos pelos filtros do Spring Security; CSRF e usuário autenticado pelo `AuthController`.
- `auth` consulta usuários pelo port `AppUserRepository` de `accounts` e fornece a implementação de `PasswordHasher` usada no cadastro. Domínio e casos de uso de `accounts` permanecem independentes do Spring Security.
- Os controllers de contas, clientes e serviços recebem a identidade autenticada por `AccountPrincipal`, fornecido por `auth`.

### Colaboradores (PR 4)

`collaborators` cadastra participantes com identidade própria por conta, nome
obrigatório e telefone opcional. Não cria usuários nem credenciais de login.
Todas as rotas abaixo usam o cookie JWT; mutações também exigem CSRF.

| Método | Rota | Resposta |
|---|---|---|
| POST | `/api/v1/collaborators` | 201, UUID e `Location` |
| GET | `/api/v1/collaborators` | 200, lista incluindo inativos |
| GET | `/api/v1/collaborators?active=true` ou `false` | 200, lista filtrada |
| GET | `/api/v1/collaborators/{id}` | 200, cadastro |
| PUT | `/api/v1/collaborators/{id}` | 200, cadastro atualizado |
| POST | `/api/v1/collaborators/{id}/deactivate` | 204, inclusive se já inativo |

POST e PUT recebem `{"name":"Maria","phone":"+44 123"}`. PUT substitui os
campos cadastrais; telefone ausente, nulo ou em branco limpa o contato.
As consultas retornam `id`, `name`, `phone` e `active`. Homônimos são permitidos.
A conta vem exclusivamente da autenticação. Recursos alheios ou inexistentes
retornam 404; entrada inválida retorna 400.

Desativação preserva identidade e cadastro para histórico. Colaboradores
inativos continuam consultáveis, mas não podem ser editados (409) nem são
retornados pelo caso de uso `FindEligibleCollaborators`, disponível para futura
integração com trabalhos. Não há reativação ou exclusão física pela API.
Gravações concorrentes usam versão otimista; conflitos retornam 409.

`V9__create_collaborators.sql` acrescenta somente a tabela de colaboradores,
com vínculo obrigatório à conta, chave composta para referências futuras e
índice por conta/estado. Não altera migrations anteriores nem dados existentes.
O PR 6 usa essas identidades nas participações dos trabalhos.

Testes específicos (PostgreSQL 17.6 via Testcontainers requer Docker):

```sh
./mvnw '-Dtest=Collaborator*Test' test
./mvnw spring-javaformat:validate
./mvnw clean verify
```

### Contratos de aplicação e valores de domínio

Casos de uso que operam em uma conta implementam `Usecase.Contextual<Input, Output>`:

```java
public UUID execute(Usecase.Context context, CreateCollaboratorInput input)
```

`Usecase.Context` contém `organizationId`, obtido pelo controller a partir do
principal autenticado. O input contém somente os dados da operação. Contexto e
input são argumentos separados; portas e queries continuam recebendo a conta
explicitamente. O contexto não autentica o chamador nem substitui o isolamento
no repositório. Esse contrato é usado por clientes, locais, trabalhos,
colaboradores e preferências da conta.

O cadastro público de conta usa `Usecase<Input, Output>` e `execute(input)`, pois
não existe conta autenticada nessa etapa. Ambos os contratos ficam em `Usecase`;
o antigo wrapper `AccountInput<T>` foi removido.

`Collaborator` é uma classe de domínio imutável, com fábricas `create`/`restore`
e operações `update`/`deactivate`. `CollaboratorName` concentra a obrigatoriedade
e a normalização do nome. O VO `Phone`, em `shared.domain.vo`, mantém uma única
regra de telefone para clientes e colaboradores. DTOs e persistência convertem
os VOs para os valores de transporte sem mudar o contrato HTTP ou o esquema.


### Trabalhos com participantes e cálculo persistido (PR 6)

| Método | Rota | Resposta |
|---|---|---|
| POST | `/api/v1/work-orders` | 201, representação completa e `Location` |
| GET | `/api/v1/work-orders/{id}` | 200, representação completa |
| GET | `/api/v1/work-orders?from=2026-09-01&to=2026-09-30&customerId=<uuid>` | 200, lista |

Todos exigem JWT; criação também exige CSRF. Os filtros são opcionais,
combináveis e sempre limitados à conta autenticada. Datas são inclusivas;
intervalo invertido retorna 400. A lista é ordenada por data do serviço e ID.

Exemplo de criação (substitua os UUIDs por cadastros reais da mesma conta):

```json
{
  "customerId": "00000000-0000-0000-0000-000000000010",
  "customerLocationId": null,
  "serviceDate": "2026-09-28",
  "startTime": "09:30:00",
  "description": "Limpeza do escritório",
  "contractedHours": 3,
  "hourlyRate": 11.50,
  "participantIds": [
    "00000000-0000-0000-0000-000000000020",
    "00000000-0000-0000-0000-000000000021",
    "00000000-0000-0000-0000-000000000022",
    "00000000-0000-0000-0000-000000000023"
  ],
  "initialStatus": "SCHEDULED"
}
```

Local, horário e descrição são opcionais. `initialStatus` aceita `SCHEDULED`
ou `COMPLETED`; quando omitido, usa `defaultWorkStatus` da conta naquele momento.
`CANCELLED` é reconhecido no histórico, mas não é aceito na criação. Os comandos de conclusão, reagendamento e cancelamento foram acrescentados no PR 7, descrito abaixo.

Participantes são obrigatórios, únicos, ordenados e ativos na inclusão. O local,
quando informado, deve pertencer ao cliente escolhido. Entradas inválidas ou
participantes duplicados retornam 400; referências inexistentes, de outra conta
ou local de outro cliente retornam 404; colaborador inativo retorna 409.
Erros usam `ProblemDetail`.

A resposta contém `id`, `customerId`, `customer` (nome atual),
`customerLocationId`, `serviceDate`, `startTime`, `description`, `contractedHours`,
`hourlyRate`, `currencyCode`, `totalAmount`, `allocationPolicyVersion`, `status`,
`version`, `participantCount` e `assignments`. Cada participação contém
`collaboratorId`, `inclusionPosition` (a partir de zero) e `allocatedAmount`.
A quantidade é derivada das participações. Conta, GBP, total e parcelas são
definidos pelo servidor.

A política exata v1 calcula o total com HALF_EVEN em duas casas e distribui
centavos residuais pela ordem de inclusão: no exemplo, £34,50 vira £8,63,
£8,63, £8,62 e £8,62. Parcelas zero são válidas. Horas e tarifa devem ser
positivas e ter no máximo duas casas; o total arredondado deve ser positivo.
Condições, total, versão da política e parcelas ficam persistidos atomicamente.
Reconstituição verifica ordem, unicidade e soma, sem recalcular. Renomear o
cliente altera o nome consultado; mudar preferências ou desativar participantes
não altera os trabalhos existentes.

As rotas antigas `/api/v1/order-services` foram retiradas, incluindo DELETE.
O contrato com `employeeCount` não permite criar trabalhos. Pagamentos e
recorrência permanecem fora deste PR.

#### Migrations e compatibilidade

V1–V9 são preservadas. V10 mantém a tabela física `tb_order_service`, amplia os
campos do trabalho e cria `tb_work_assignment`. FKs compostas protegem a conta e
o pertencimento do local ao cliente; há unicidade por trabalho/colaborador e
trabalho/posição. Valores usam `NUMERIC` sem precisão fixa e checks de escala,
sinal e finitude, evitando arredondamento silencioso no armazenamento.
V11 verifica a integridade das participações e retira `employee_count`.

**V10 aborta antes da expansão se houver trabalhos legados.** Uma contagem não
permite recuperar as identidades dos participantes. A falha preserva dados e
histórico Flyway; não apaga registros, cria participantes fictícios nem infere
identidades. A implantação exige resolver explicitamente esses dados antes de
repetir a migration. Não execute limpeza automática no banco de desenvolvimento.

Verificação focada:

```sh
./mvnw '-Dtest=WorkOrder*Test,ExactAllocationPolicyTest,CalculationInputsTest,AccountApiIntegrationTest,CustomerApiIntegrationTest' test
./mvnw clean verify
./mvnw spring-javaformat:validate
git diff --check
```

Os testes PostgreSQL descartáveis validam V1–V11, Hibernate `ddl-auto=validate`,
restrições compostas, rejeição de legado com dados preservados, round-trip de
valores grandes/parcelas zero e rollback integral quando uma participação falha.


### Ciclo operacional e agenda (PR 7)

| Método | Rota | Resposta |
|---|---|---|
| POST | `/api/v1/work-orders/{id}/complete` | 204 |
| POST | `/api/v1/work-orders/{id}/reschedule` | 204 |
| POST | `/api/v1/work-orders/{id}/cancel` | 204, inclusive se já cancelado |

Os comandos exigem JWT e CSRF e usam a conta autenticada. Recursos inexistentes
ou alheios retornam 404, dados inválidos 400 e transições proibidas 409, com
`ProblemDetail`. Concluir e cancelar não precisam de corpo. Concluir um agendado
é permitido inclusive no futuro; repetir conclusão ou cancelamento não grava
novamente nem incrementa a versão.

Reagendamento recebe `{"serviceDate":"2026-10-15","startTime":"09:30:00"}`.
A data é obrigatória; horário omitido ou nulo remove o horário. Agendados podem
ser reagendados; concluídos somente enquanto sua data atual for posterior a
hoje no fuso IANA da conta. A nova data pode ser passada, atual ou futura e o
estado é preservado. O relógio é injetado, sem depender do fuso do servidor.
Cancelados não podem ser concluídos ou reagendados. Não há reabertura nem DELETE.

A agenda reutiliza `GET /api/v1/work-orders`, com filtros opcionais combináveis
`from`, `to`, `customerId` e `status` (`SCHEDULED`, `COMPLETED`, `CANCELLED`).
Os limites de data são inclusivos e a ordenação permanece por data e ID. Sem
filtro de estado, a consulta inclui o histórico cancelado. Consulta por ID
continua disponível após cancelamento; nenhuma consulta gera ocorrências.

As operações bloqueiam a raiz do trabalho por conta antes de validar e usam
uma transação local. Apenas estado, data e horário são atualizados; identidade,
condições, versão da política e linhas de participação/parcelas são preservadas,
inclusive se um participante foi desativado. A versão otimista existente segue
ativa. `workflows.application.CancelWorkOrder` coordena o cancelamento pelo
contrato público de trabalhos; seu decorator de infraestrutura delimita a
transação para futura integração. Esse registro descreve a entrega histórica do PR 7. Os módulos financeiro e de recorrência são descritos abaixo.

Testes adicionais cobrem transições, preferência e sobrescrita no cadastro,
limites de dia/horário de verão com relógio fixo, HTTP/isolamento, preservação das
linhas de participação, cancelamento concorrente, reagendamento contra cancelamento
e rollback da transação externa. Executar:

```sh
./mvnw '-Dtest=WorkOrder*Test,ExactAllocationPolicyTest,CalculationInputsTest,AccountApiIntegrationTest,CustomerApiIntegrationTest' test
./mvnw clean verify
./mvnw spring-javaformat:validate
git diff --check
```

## PR 9 — recorrência e geração limitada

`recurrence` é um módulo da mesma aplicação. `POST /api/v1/recurrence-series` cria a série e os trabalhos da janela atual em uma única transação; `GET /api/v1/recurrence-series/{id}` consulta suas condições. As rotas exigem autenticação; criação exige CSRF. A conta vem do principal.

```json
{
  "frequency": "MONTHLY",
  "startsOn": "2026-10-31",
  "endsOn": null,
  "customerId": "11111111-1111-1111-1111-111111111111",
  "customerLocationId": null,
  "startTime": "09:00:00",
  "description": "Serviço mensal",
  "contractedHours": 3,
  "hourlyRate": 11.50,
  "participantIds": ["22222222-2222-2222-2222-222222222222"],
  "initialStatus": "SCHEDULED"
}
```

Frequências: `WEEKLY`, `BIWEEKLY`, `MONTHLY`. `initialStatus` é opcional; quando omitido, a preferência atual é resolvida e persistida na série. Alterações posteriores da preferência não mudam séries existentes. O término é opcional e inclusivo. IDs do exemplo devem ser substituídos por cadastros existentes da conta.

A janela tem 30 datas: `[hoje, hoje + 30 dias)`, no fuso da conta. Mensal usa o último dia válido em meses curtos sem perder o dia âncora. Retomada não gera ocorrências passadas. Navegar pela agenda não amplia o horizonte.

O agendador executa na inicialização e a cada 15 minutos após a conclusão da execução anterior. Configuração:

```properties
moply.recurrence.scheduler.enabled=true
moply.recurrence.scheduler.delay=PT15M
moply.recurrence.scheduler.initial-delay=PT0S
```

Séries são percorridas em lotes de 100. Cada tentativa possui transação e bloqueio próprios; falha de uma série não bloqueia as demais. Logs incluem conta/série, janela, resultados após commit e duração; falhas durante uma ocorrência incluem a data original. Uma série com participante inativo falha sem alterar sua equipe; será tentada novamente. Não há interface de reparação de série neste PR.

V14 adiciona séries, participantes ordenados e identidade de ocorrência. V1–V13 permanecem intactas. A unicidade por série/data original impede duplicação, inclusive após cancelamento ou reagendamento. Respostas de trabalhos incluem `recurrenceSeriesId` e `occurrenceDate`; a data operacional continua em `serviceDate`.

Cada ocorrência recebe novas atribuições e parcelas calculadas pelo contrato público de criação de trabalhos. Nenhum pagamento, acerto, reversão ou chave financeira é copiado. Trabalho futuro pode nascer concluído, mas permanece projeção e não antecipa operações financeiras.

### Ajustes financeiros anteriores, separados do módulo novo

- Pagamento do cliente é integral e único enquanto ativo. Acertos do colaborador podem ser vários parciais até o saldo da atribuição; V13 já permite isso e não foi alterada.
- Por decisão confirmada nesta entrega, novos acertos exigem `serviceDate <= paidOn <= hoje`, no fuso da conta. A restrição vale também para trabalhos avulsos. Registros existentes, repetições idempotentes e reversões são preservados.
- Acerto ativo bloqueia cancelamento. Reagendamento preserva os acertos, inclusive quando muda a data para o futuro; novos acertos respeitam a nova data. Pagamento ativo do cliente bloqueia reagendamento.
- Foi removido o caso de uso público legado de reagendamento que podia ignorar pagamentos; consumidores usam o workflow transacional protegido. Pagamentos, acertos, reversões e operações do trabalho continuam coordenados pelo bloqueio da mesma raiz.

Validação: testes `Recurrence*Test` cobrem calendário, fuso, concorrência PostgreSQL, preservação da identidade, migração V13→V14, rollback e continuidade. Executar `./mvnw clean verify spring-javaformat:validate` e `git diff --check`.

Alterações coletivas de série pertencem ao PR 10 e não foram implementadas. Também não há novos relatórios, geração retroativa, pagamentos automáticos ou movimentação de dinheiro. Documentos detalhados em `src/docs` continuam sob a regra de ignore preexistente.
