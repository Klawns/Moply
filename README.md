# Moply

O Moply é uma API para organizar serviços realizados por equipes e acompanhar os valores a receber dos clientes e a pagar aos colaboradores. Reúne cadastros, agenda, trabalhos recorrentes, divisão da remuneração e relatórios financeiros em uma conta.

## O que o sistema faz

- **Clientes e locais:** cadastra clientes, contatos e locais de atendimento para vincular aos trabalhos.
- **Colaboradores:** mantém os participantes da equipe, permite desativá-los preservando o histórico e oferece um valor por hora fixo opcional para cada um.
- **Trabalhos e agenda:** registra cliente, local, data, horário, descrição, horas contratadas, tarifa e participantes. Permite consultar, concluir, cancelar e reagendar trabalhos conforme as regras operacionais e financeiras.
- **Divisão da remuneração:** calcula o preço do serviço e as parcelas de cada participante, preservando o resultado aprovado no histórico.
- **Recorrência:** cria serviços semanais, quinzenais ou mensais, gera as próximas ocorrências e permite cancelar ou reagendar uma ocorrência ou uma sequência. As condições financeiras aprovadas ficam preservadas na série.
- **Controle financeiro:** registra o pagamento integral do cliente e acertos parciais dos colaboradores, acompanha os saldos e mantém o histórico de reversões.
- **Relatórios:** consulta trabalhos, recebimentos e valores dos colaboradores por período, com filtros, totais e detalhes paginados.
- **Preferências:** configura o fuso horário, o estado inicial dos novos trabalhos e uma tarifa por hora padrão para a conta.

Cada conta possui seus próprios dados. O acesso é autenticado por cookie, com proteção CSRF nas operações de escrita. Os valores monetários são registrados em libras esterlinas (GBP). Os lançamentos financeiros acompanham recebimentos e acertos informados pelo usuário; não realizam transferências bancárias.

## Como funciona o valor por hora

A tarifa padrão da conta pode preencher o cadastro de um novo trabalho e pode ser substituída naquele serviço. Colaboradores podem ter uma tarifa própria opcional.

Quando existem tarifas próprias diferentes da tarifa do trabalho, a API apresenta uma prévia das bases individuais e da divisão da sobra, exigindo confirmação antes de salvar. Se as bases ultrapassarem o preço do serviço, o usuário precisa ajustar os valores. Alterar uma tarifa cadastral não modifica trabalhos nem condições de séries já aprovadas.

