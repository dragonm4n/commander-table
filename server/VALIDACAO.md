# Validação da alpha 0.3

Os módulos forge-core, forge-game, forge-ai, forge-gui e forge-web foram compilados juntos com Java 17 na revisão indicada em BUILD.md. A interface passou na verificação TypeScript e nos builds de produção para Sites e para o servidor local.

## Pilha e novos recursos — executados na alpha 0.3

`tests/stack.py` e `StackScenarioServer.java` usam quatro controladores humanos reais por HTTP. O cenário prepara apenas as cartas iniciais; conjurar, pagar, escolher alvos, passar prioridade e resolver seguem a API normal e o motor Forge. O carregador do cenário é compilado separadamente e não entra no JAR distribuído.

Casos verificados:

- Mana Drain seleciona o item correto da pilha e anula Blade Splicer. A criatura vai ao cemitério e não cria a ficha de entrada.
- Counterspell responde a outro Mana Drain, numa pilha com três itens. O Mana Drain é anulado, Blade Splicer resolve e cria uma ficha 3/3.
- As duas relações entre itens da pilha são exportadas com IDs e cartas de origem, usados pela seleção e pelas setas.
- O primeiro Mana Drain gera exatamente três manas incolores na próxima fase principal de seu controlador; o Mana Drain anulado não gera mana.
- Essa mana paga Manalith, incluindo um acionamento explícito do botão de mana incolor pela API. As ilhas permanecem desviradas e o reservatório termina vazio.
- Avisos de conjuração identificam Mana Drain. A ativação de Jayemdae Tome identifica a carta de origem e sua compra resolve.
- Dois marcadores são exportados como `+1/+1: 2`.
- A ficha exporta uma impressão do Scryfall (`tblc/38/en`). A consulta real retornou Phyrexian Golem 3/3 relacionado a Blade Splicer; a imagem retornou 96.370 bytes válidos.
- O evento de criação de ficha fornece o som `token`. Os eventos enviados não carregam nomes de arquivos definidos por scripts de cartas.
- Os 39 MP3s da interface coincidem byte a byte com os recursos do Forge. O servidor alpha 0.3 entregou `token`, `instant` e `draw` com o conteúdo correto e o tipo `audio/mpeg`.
- As quatro sessões continuam sem acesso às mãos dos outros jogadores.

`tests/smoke.py` também passou com dois humanos e duas IAs: autenticação, convites, início, mãos/grimórios ocultos, terrenos humanos e permanentes da IA.

`lib/decks.test.mjs` passou nos três casos: lista recuperável após uma nova leitura do armazenamento; atualização sem duplicação e backup completo para outro navegador; preservação das listas anteriores diante de backup inválido ou falha por falta de espaço. A validação de construção do deck continua no Forge antes do salvamento automático.

## Regressão de entrada e alvos — validada na alpha 0.2

`tests/etb.py` usa quatro sessões HTTP independentes e o controlador humano do Forge. `ScenarioServer.java` prepara um campo determinístico na thread de jogo; ele é um ponto de entrada exclusivo do teste e não está no JAR distribuído. Lançamentos, pagamento de mana, passagens de prioridade, escolhas e resolução usam a API normal da aplicação.

Casos verificados:

- O Um Anel colocado no campo sem ser conjurado não concede proteção, inclusive após passagens de prioridade.
- O Um Anel conjurado coloca sua habilidade de entrada na pilha. Depois da resolução, a proteção aparece no estado do jogador.
- O Forge rejeita o jogador protegido como alvo de Lightning Bolt. A proteção expira no próximo turno desse jogador.
- Blade Splicer conjurado coloca sua habilidade de entrada na pilha e cria uma ficha real de Golem 3/3.
- Lightning Bolt fornece o alvo jogador e causa 3 de dano; outro Bolt fornece o alvo permanente e destrói Grizzly Bears por dano letal.
- Um ataque e um bloqueio reais fornecem as relações atacante–defensor e bloqueador–atacante usadas pelas setas.
- Mãos alheias e identidades do grimório permanecem ocultas. A projeção de um alvo em mão privada omite nome e ID da carta.

`tests/smoke.py` verifica o fluxo com dois humanos e duas IAs nativas: autenticação do anfitrião, convite, decks, início, privacidade, prioridade, terreno jogado pelo humano e permanentes jogadas pela IA.

## Limites da validação

Esses cenários não cobrem uma partida completa ou todas as cartas. A carta criadora de ficha relatada pelo usuário ainda não foi identificada; Blade Splicer é o caso representativo. Decisões raras, ordenações especiais e combate complexo precisam de cenários próprios quando surgir um problema concreto.

Não houve inspeção visual automatizada por navegador. A organização e a responsividade foram implementadas e compiladas; a ergonomia das faixas, bolinhas e setas ainda depende do uso nas telas dos jogadores. Os scripts Windows/macOS precisam de execução nesses sistemas; a validação desta entrega ocorreu em Linux.

A reprodução audível no navegador depende do botão Som e da disponibilidade de áudio no dispositivo. A entrega verifica os eventos e os arquivos, sem afirmar uma escuta ou inspeção por navegador. Os decks são persistidos por navegador e origem; não há sincronização entre computadores.
