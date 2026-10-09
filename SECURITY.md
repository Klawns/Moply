# Segurança da autenticação

Spring Security, cookies, JWT, CSRF, limites de autenticação e persistência desses controles pertencem a `auth.infra`. `AuthRateLimitStore` e `TokenRevocations` são contratos técnicos sem tipos Spring. Domínio e aplicação continuam independentes de Spring/Jakarta e dos adaptadores; `ArchitectureBoundaryTest` verifica essa fronteira. Organização de operações de negócio continua vindo do principal autenticado, nunca do payload.

## Login e cadastro

Somente POST de `/api/v1/auth/login` e `/api/v1/accounts` consome limites, depois da validação CSRF e antes da autenticação/cadastro custosos. Contamos solicitações, incluindo credenciais inválidas, logins bem-sucedidos e cadastros inválidos. O endpoint público CSRF permanece disponível; seu token não autentica ninguém.

A política usa reposição contínua: períodos ociosos acumulam capacidade até a rajada. Estes são valores iniciais para desenvolvimento, configuráveis por ambiente, não um SLA nem limites calibrados para tráfego produtivo:

| Escopo | Rajada | Reposição | Taxa sustentada | Variáveis |
| --- | --- | --- | --- | --- |
| Login por IP | 20 | 1 solicitação / 3 s | 20/min | `MOPLY_AUTH_LOGIN_IP_BURST`, `MOPLY_AUTH_LOGIN_IP_REFILL_SECONDS` |
| Login por e-mail normalizado | 5 | 1 solicitação / 60 s | 1/min | `MOPLY_AUTH_LOGIN_ACCOUNT_BURST`, `MOPLY_AUTH_LOGIN_ACCOUNT_REFILL_SECONDS` |
| Cadastro por IP | 3 | 1 solicitação / 1200 s | 3/h | `MOPLY_AUTH_SIGNUP_IP_BURST`, `MOPLY_AUTH_SIGNUP_IP_REFILL_SECONDS` |

E-mail usa `strip` e minúsculas com `Locale.ROOT`, como a autenticação. Endereços IP são canonicalizados, inclusive IPv6. Chaves persistidas usam SHA-256: não armazenam IP/e-mail em texto, mas hashes não representam anonimização contra ataques de dicionário. Não logar valores, cookies ou senhas.

As cotas são globais entre instâncias que compartilham banco/schema e **a mesma configuração**. PostgreSQL serializa o débito de todas as chaves numa transação curta, sob bloqueio da linha de controle. Nenhum débito é realizado se uma das cotas negar a tentativa. Hashing de senha fica fora dessa transação. Esse bloqueio privilegia simplicidade e consistência; medir contenção antes de grande escala, podendo substituir o adaptador por armazenamento distribuído adequado sem mudar contratos internos.

Há até `MOPLY_AUTH_RATE_MAX_ENTRIES` chaves (padrão 10000). Estado completamente reposto pode ser removido sem alterar a cota; limpeza ocorre nas solicitações e a cada minuto. Ao atingir a capacidade, novas identidades recebem 429 com espera sugerida de 60 segundos. Identidades existentes continuam sujeitas às próprias cotas. Falha de banco não libera a autenticação: retorna 503. Sincronizar relógios das instâncias; timestamps do limitador usam precisão de milissegundos. Requisições negadas não estendem o bloqueio. Não há bloqueio permanente de usuário.

Limitar globalmente o e-mail também permite que um terceiro cause espera temporária para esse identificador. IPs compartilhados/NAT dividem a cota de IP. Essas são limitações da política, não garantias de isolamento por pessoa. Operações autenticadas não foram submetidas a uma cota de negócio nova; qualquer futura cota de organização deve usar o principal e tratar justiça entre organizações explicitamente.

Toda limitação retorna HTTP 429, `Retry-After` em segundos, `Cache-Control: no-store` e o `ProblemDetail` existente com `category=SECURITY_ERROR`, `code=RATE_LIMIT_EXCEEDED` e `requestId`. Não expomos contadores por e-mail. O frontend mostra a espera em português/inglês e não repete automaticamente POSTs. Clientes devem aguardar o header; valores são sugestões, não promessa de aprovação da tentativa seguinte. Não criar exceções por headers, planos ou payloads de clientes.

## IP e proxy confiável

Por padrão, usar apenas o IP da conexão. `server.forward-headers-strategy=none` impede o container de reescrever essa identidade a partir de headers não confiáveis. `X-Forwarded-For` e `Forwarded` são ignorados.

`MOPLY_AUTH_TRUSTED_PROXIES` aceita uma lista separada por vírgulas de **IPs literais exatos** do proxy. Somente uma conexão vinda de um desses endereços pode fornecer `X-Real-IP`, contendo um único IP literal válido. Ausência/valor inválido conserva o IP do proxy; não fazemos resolução DNS de headers. Não liberar redes inteiras ou confiar em qualquer origem. Configurar endereços estáveis, impedir acesso público direto ao backend e obrigar o proxy a **sobrescrever** esse header. Sem essa configuração, todos os clientes atrás do proxy compartilharão sua cota de IP.

`moply-frontend/deploy/nginx.conf` oferece um modelo HTTPS com sobrescrita de `X-Real-IP`, limpeza dos outros headers e limite de corpo de 64 KiB. O formulário de autenticação do Tomcat aceita até 16 KiB. Ajustar conscientemente esses limites se o contrato da aplicação mudar. Rate limiting de autenticação não substitui defesa volumétrica no ingress.

## Logout e JWT

Cada JWT tem `jti` aleatório, validade fixa de 30 minutos, assinatura HS256, emissor/audiência e vínculo de usuário/organização verificados. Logout com CSRF válido persiste o `jti` até a expiração e remove os cookies. Uma cópia do JWT passa a receber 401 nas solicitações seguintes; outras sessões do mesmo usuário continuam válidas. Repetir logout é idempotente. Cookies duplicados são rejeitados como ambíguos.

A revogação é compartilhada entre instâncias e sobrevive a reinícios. Índice por expiração e limpeza a cada minuto evitam retenção indefinida; armazenamos apenas identificador/expiração, nunca o token. Falha ao persistir a revogação retorna 503, sem anunciar sucesso nem remover o cookie de autenticação. O logout padrão pode limpar o CSRF antes dessa falha; obter novo CSRF para repetir. Uma solicitação já autenticada/em andamento pode terminar após logout. Não é mecanismo de cancelamento de transações em andamento.

JWTs antigos sem `jti` deixam de autenticar: essa alteração exige novo login. Exclusão do usuário ou mudança do vínculo organizacional já invalida autenticação. Futuras funcionalidades de troca de senha, bloqueio e encerramento de todas as sessões precisam definir invalidação de todas as credenciais; o logout atual revoga apenas a sessão apresentada.

## Implantação e manutenção

Usar `ACTIVE_PROFILE=prod`, HTTPS público e cookies Secure. O template Nginx redireciona HTTP para HTTPS e fornece HSTS, CSP, Referrer-Policy, nosniff e frame DENY no HTML da SPA. Certificado/chave, hostname, DNS `backend`, rede e allowlist precisam refletir a implantação real. Não usar certificado de teste em produção. Headers foram testados em ambiente temporário; isso não valida um ambiente produtivo externo.

CSP restringe scripts e conexões à mesma origem, bloqueia objetos, frames e mudança de origem de formulário/base. `style-src 'unsafe-inline'` acomoda estilos de componentes e bindings Angular; **scripts** não recebem `unsafe-inline`/`unsafe-eval`. O build produtivo desativa CSS crítico inline para não gerar o script inline do carregador de estilos. Manter frontend/API na mesma origem; revisar CSP conscientemente ao adicionar serviços externos. Não abrir CORS indiscriminadamente. Backend também fornece `Referrer-Policy: no-referrer`; o ingress é responsável por HSTS no caminho TLS.

Gerar `MOPLY_JWT_SECRET` com CSPRNG (por exemplo `openssl rand -base64 32`) e guardá-la no gerenciador de segredos; nunca versionar/logar a saída. Chave com comprimento mínimo não comprova entropia. Na rotação atual, substituir a chave coordenadamente em todas as instâncias numa janela de manutenção: tokens assinados pela chave antiga deixam de valer e usuários precisam entrar novamente. Evitar uma frota com chaves diferentes. Documentar responsável e periodicidade conforme política operacional; este trabalho não rotacionou segredos locais.

Antes da exposição pública, medir custo de hashing e latência/CPU do login/cadastro no hardware produtivo, tráfego legítimo/NAT e contenção do banco. Medição local em 2026-10-09, 10 verificações após aquecimento: BCrypt custo 10, mediana 63,1 ms e máximo 74,8 ms; custo 12, mediana 246,0 ms e máximo 252,5 ms. Preservamos o custo 10 do encoder delegante existente. Essa amostra é uma referência de desenvolvimento, não benchmark de carga nem calibração de produção.

Começar com proteção habilitada, observar 429 por operação sem registrar identidades, espera respeitada pelo cliente, erros 503 e latência/CPU. Calibrar rajada/reposição por esses dados e testes de carga; não simplesmente desativar controles. Publicar alterações de política e avisar clientes afetados antes de apertar limites em operação. Reduções emergenciais por estabilidade devem ser registradas/comunicadas. Mudar parâmetros coordenadamente entre réplicas; considerar a retenção dos buckets calculada com a configuração anterior e aguardar a reposição completa antes de apertar sua expiração. Não apresentar limite por instância como global.

## Dependências e divulgação

CI executa Grype 0.120.1 no JAR Spring Boot empacotado, incluindo bibliotecas transitivas runtime, em push/PR, execução manual e semanal. A action de scan está fixada por SHA. Achados altos/críticos bloqueiam, inclusive sem correção disponível; o relatório JSON também conserva achados menores. Falha de download/atualização do banco não é considerada aprovação. A varredura inicial encontrou 17 associações de avisos a Tomcat 11.0.24 e Jackson 2.21.5/3.1.5. Associação de versão não demonstra explorabilidade em cada configuração (por exemplo, não utilizamos DIGEST Tomcat). Como há correções, atualizamos Tomcat para 11.0.26 e as BOMs Jackson 2/3 para 2.21.7/3.1.7. Maven Central não disponibilizava outra versão estável da linha Boot 4.1 além de 4.1.1 na consulta: os três overrides são temporários, alinhados por BOM, até Boot gerenciar os patches. Não há troca arbitrária de major/minor nem supressão desses achados.

Dependabot propõe atualização de Maven/GitHub Actions semanalmente; preferir atualizar a BOM Boot a sobrescrever dependências sem necessidade.

Não há supressões configuradas. Triar severidades menores no relatório; para achados bloqueantes, atualizar/remover a dependência ou avaliar aplicabilidade. Qualquer exceção futura exige alteração revisada e versionada, identificador CVE/GHSA, pacote/versão, evidência de não aplicabilidade, responsável, prazo de expiração e issue vinculada; nunca aceitar um ignore amplo/permanente ou contornar falha de scanner. Testes funcionais não substituem essa análise. O scanner cobre as dependências runtime empacotadas; não cobre segredos, todo histórico Git, código da aplicação nem ferramentas de build/teste.

Para vulnerabilidade nova com exploração prática ou dados sensíveis, usar comunicação privada/GitHub private vulnerability reporting quando habilitado. Não publicar credenciais, dados pessoais ou scripts de exploração em issue pública. A issue #10 registra controles visíveis no código de uma aplicação de desenvolvimento.
