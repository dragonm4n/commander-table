# Commander Table — alpha 0.3

Mesa web de Commander para quatro assentos, com regras e IAs executadas pelo Forge. O computador anfitrião executa Java; os convidados precisam apenas do navegador. A interface está incluída neste pacote e é servida pelo próprio programa.

## Atualizar de uma versão anterior

Encerre a partida e o servidor antigo. Extraia este ZIP inteiro em uma pasta nova e inicie o programa dessa pasta; esta atualização inclui alterações na interface e na ponte Java. Abra http://localhost:8787 ou o novo endereço do túnel e atualize a página. Salas e sessões antigas não sobrevivem ao reinício.

## Iniciar no Windows

1. Extraia o ZIP inteiro para uma pasta. Não execute diretamente dentro do ZIP.
2. Instale Java **17 ou superior**, 64 bits, se necessário: https://adoptium.net/temurin/releases/?version=17 . O servidor usa até 3 GB de memória; um computador com 8 GB ou mais é uma referência prática para o teste.
3. Abra `iniciar-windows.bat` e aguarde “servidor pronto”. Deixe a janela aberta. A **chave do anfitrião** aparece nela.
4. Para jogar sozinho, abra http://localhost:8787 no navegador, conecte, informe seu nome e a chave e crie uma mesa.
5. Para jogar pela internet, abra também `tunel-windows.bat`. No primeiro uso ele baixa o programa **cloudflared para Windows x64**, do GitHub oficial da Cloudflare. O túnel encaminha apenas este servidor local para um endereço HTTPS temporário.
6. Abra no navegador o endereço `https://...trycloudflare.com` exibido pelo túnel. Conecte esse mesmo endereço na interface e crie a mesa usando sua chave.
7. Use **Convidar amigos** e envie o link gerado. Os amigos entram antes de a partida começar. Cada humano confirma seu deck; o anfitrião pode escolher os decks das IAs.
8. Clique em **Iniciar partida**. Lugares vazios passam a ser controlados pelo Forge.

Para Linux/macOS, use `bash iniciar.sh` e, após instalar cloudflared, `bash tunel.sh`. Preferências e cache ficam na pasta `profile` deste pacote. O Java é multiplataforma; os scripts de Windows e macOS ainda precisam de validação nesses sistemas. Esta entrega foi compilada e exercitada em Linux com Java 17.

## Jogar

- Os quatro campos têm o mesmo tamanho: terrenos na faixa inferior, criaturas e outras permanentes na superior. Cada faixa pode ser rolada quando há muitas cartas. A mão privada fica na parte inferior.
- As bolinhas no topo mostram as etapas em ordem. A etapa atual fica dourada; passe o cursor para ver o nome completo.
- Clique numa carta para jogar, ativar uma habilidade ou escolher um alvo. O Forge oferece somente habilidades que podem ser usadas naquele momento.
- Para escolher um jogador (inclusive quem começa), clique no retrato **ou no número de vida** dele.
- A prioridade e as escolhas obrigatórias ficam no canto inferior direito, ao lado da mão. Confirme/avance ali. O painel rola internamente quando a decisão tem muitas opções. As mensagens de regras são do Forge e podem misturar português e inglês.
- O cabeçalho de cada jogador mostra sua última mágica ou habilidade. Clique na mensagem para inspecionar a carta de origem.
- Ao escolher um alvo na pilha, clique em **Escolher como alvo** no item destacado, ou marque a opção com a miniatura e clique em **Confirmar alvo**. **Cancelar escolha** cancela explicitamente a seleção; confirmar sem alvo fica desabilitado.
- Clique nas zonas para ver comando, cemitério, exílio e cartas do grimório que as regras tenham revelado. O grimório continua oculto normalmente.
- Passe o cursor sobre uma carta, ou toque na lupa, para ver a arte e o texto numa janela de inspeção. A prévia do cursor desaparece ao sair da carta. A lupa fixa a inspeção; feche com × para liberar o campo.
- Dano, vida, marcadores, viradas de cartas e mudança de zona são determinados pelo motor de regras. Esta versão não é uma mesa de manipulação livre.
- Para declarar ataques/bloqueios, siga a instrução do Forge e selecione as cartas/jogadores. Distribuições e ordenações aparecem no painel de decisões.
- Abra Pilha, Chat e Histórico pelos botões do topo. Esses painéis ficam recolhidos para aproveitar a área da mesa.
- Clique em **Som** no topo para ativar os efeitos originais do Forge. O som começa desligado e depende desse clique para ser autorizado pelo navegador. O mesmo botão silencia. Compras, magias, dano, fichas e outros eventos usam os efeitos disponíveis no pacote do Forge.
- Setas douradas ligam itens da pilha aos seus alvos, inclusive outras mágicas na pilha. Vermelhas mostram ataques; azuis, bloqueios. O botão Setas liga/desliga a exibição. As setas indicam alvos declarados enquanto o item está na pilha, não efeitos que apenas afetam algo sem dar alvo.
- Efeitos sobre jogadores aparecem junto dos contadores. O Um Anel mostra “Proteção contra tudo” enquanto a proteção está ativa; fichas têm a marca FICHA mesmo se a arte não carregar.
- Para reconectar após uma queda de rede, mantenha a mesma aba. A sessão fica no armazenamento da aba. Atualizar a página preserva a sessão; limpar os dados ou fechar a aba pode perdê-la.

## Decks

Há seis precons incluídos. Para importar, cole a lista em inglês, uma carta por linha, como `1 Sol Ring`, e informe comandante(s) no campo separado. Linhas `1 Nome (SET) número` também são aceitas. A lista precisa cumprir o formato Commander do Forge. Cartas desconhecidas ou ainda não implementadas são rejeitadas com os respectivos nomes. O Forge pode avisar que a IA não joga algumas cartas corretamente: isso é uma limitação da IA nativa, não uma decisão de um modelo de linguagem.

Ao confirmar uma lista importada que o Forge aceita, ela é salva automaticamente com o nome informado. Para reutilizar, abra **Meus decks**, escolha **Usar lista** e confirme o deck para aquele assento. Reimportar a mesma lista atualiza seu nome sem duplicá-la.

As listas ficam neste navegador e neste endereço de site, mesmo após reiniciar o servidor. **Exportar backup** gera um JSON com os decks; **Importar backup** restaura e combina as listas. Exporte antes de mudar de computador, limpar os dados do navegador ou trocar o endereço do túnel. `localhost` e cada endereço de túnel têm armazenamentos distintos. Não há sincronização de decks entre jogadores nem salvamento da partida.

## Pilha, mana, fichas e marcadores

A escolha de mágicas na pilha agora identifica a carta de origem e o item exato. O Forge continua determinando a validade do alvo, a resolução e as respostas. A regressão verifica Mana Drain anulando Blade Splicer, Counterspell anulando outro Mana Drain e a criatura criando sua ficha quando consegue resolver.

Também foi corrigido o identificador de mana incolor: o reservatório do Forge usa `ManaAtom.COLORLESS` (32), enquanto a ponte anterior consultava 0. A correção exibe a mana gerada e encaminha o botão de uso com o identificador correto.

Fichas usam a edição e o número de impressão registrados pelo Forge para buscar a arte no Scryfall. Cópias de cartas usam a imagem da carta original. Marcadores usam o nome legível do motor, como **+1/+1 ×2**, em vez de `P1P1`.

## Correção de habilidades de entrada

A ponte anterior aplicava o filtro de ativação manual também às habilidades disparadas que o Forge encaminha automaticamente. Essas habilidades podiam ser descartadas antes de entrar na pilha. A alpha 0.2 distingue os cliques do jogador das escolhas fornecidas pelo motor, seguindo a interface desktop do Forge. O Um Anel e Blade Splicer são os casos da regressão. A proteção do Anel exige que ele tenha sido conjurado; simplesmente colocá-lo no campo não concede essa proteção.

## Limites desta alpha

- Uma sala ativa por servidor, com 1–4 humanos e IAs preenchendo os demais assentos.
- Entrada de novos humanos apenas no lobby. Nesta versão uma IA não pode ser substituída após começar; um humano desconectado mantém o assento e a partida pode esperar por ele.
- A partida existe na memória do processo: encerrar Java perde a sala. Não há gravação, replay ou retomada após reinício.
- O túnel de teste depende da Cloudflare, muda de endereço a cada abertura e não tem garantia de disponibilidade. Para um servidor permanente, use um túnel nomeado ou HTTPS num servidor próprio.
- Ainda não há chamadas de voz/vídeo, espectadores dedicados, drag-and-drop livre, marcações livres sobre a mesa ou paridade completa com o EDHLab.
- A integração exercita o motor real. Não foi possível validar todas as combinações de cartas e decisões raras nesta primeira entrega. Ordenações especiais e atribuições complexas de dano merecem testes com decks concretos.
- Artes dependem do Scryfall. Se falharem, o nome e o texto continuam disponíveis. O programa não inclui um acervo de imagens.
- O acesso externo depende de o computador ficar ligado, Java e o túnel estarem abertos e a rede permitir a conexão. Desligar o túnel remove o acesso pela internet.

## Chaves e privacidade

A chave do anfitrião cria salas. Os convites dão acesso a um lugar; cada jogador recebe uma sessão individual. Não compartilhe a chave do anfitrião nem copie o armazenamento da aba para outra pessoa. O estado enviado a cada jogador omite mãos alheias e a ordem do grimório, exceto revelações autorizadas pelas regras. O operador do computador hospeda o motor e, como qualquer servidor de jogo, controla o processo que contém o estado completo.

O resumo dos testes executados está em `VALIDACAO.md`.

## Código e licenças

Forge original: https://github.com/Card-Forge/forge

Revisão usada: `53a103721d627ecb76a2ea52b2febe894844f288` (2.0.15-SNAPSHOT).

O código correspondente dos módulos usados, recursos de cartas, os arquivos de construção e a nova ponte Java estão em `source/`. A interface React e seu código estão em `source/web-ui/`. Veja `source/BUILD.md`. As dependências mantêm suas licenças; os JARs incluem os avisos em META-INF. Consulte `LICENSE` e os recursos de licenças do Forge. Magic: The Gathering e suas artes pertencem aos respectivos titulares; este projeto é independente.

Os efeitos MP3 de `web/sounds` foram copiados sem alterações de `forge-gui/res/sound` da revisão acima. O mapeamento de eventos usa o `EventVisualizer` do Forge; sons específicos de cartas definidos por scripts não são carregados.

Cloudflare: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/
Scryfall: https://scryfall.com/docs/api
