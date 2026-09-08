# Commander Table

Interface web de Commander para quatro jogadores, com regras e inteligência
artificial executadas pelo [Forge](https://github.com/Card-Forge/forge).
O computador anfitrião executa o servidor Java; convidados usam o navegador.
Os assentos vazios são preenchidos pelas IAs nativas do Forge.

Versão dos recursos: **alpha 0.3**. Projeto independente, sem afiliação com
Wizards of the Coast, Forge, EDHLab ou Scryfall.

## Recursos atuais

- Quatro campos simultâneos, terrenos abaixo das outras permanentes e mão privada.
- Prioridade no canto inferior direito e etapas do turno compactas.
- Avisos de mágicas e habilidades com inspeção da carta de origem.
- Pilha controlada pelo Forge, seleção de alvos e setas de alvo/combate.
- Artes de cartas e fichas obtidas do Scryfall e marcadores legíveis.
- Sons originais do Forge, ativados pelo jogador no navegador.
- Importação de decks, listas salvas no navegador e backup em JSON.
- Multiplayer com 1–4 humanos; demais lugares ocupados pelas IAs.

## Relação com o Forge

Este repositório contém a interface e o módulo Java `forge-web`, cuja fonte fica
em `server/`. Ele utiliza o Forge como dependência, na revisão registrada em
`FORGE_REVISION`. As regras das cartas continuam no Forge.

Na compilação, a ponte é adicionada ao conjunto de módulos Maven do Forge.
Não há alterações nas fontes de seu motor de regras nesta versão. Por isso,
um repositório próprio é suficiente. Se forem necessárias alterações no motor,
um fork separado do Forge pode manter essas mudanças e receber atualizações
do projeto original; este repositório então aponta para a revisão desse fork.

## Organização

| Caminho | Conteúdo |
| --- | --- |
| `app/`, `components/`, `lib/` | Interface, projeção do estado e decks salvos |
| `standalone/` | Entrada da aplicação no navegador |
| `public/sounds/` | Efeitos MP3 do Forge e sua procedência |
| `server/src/` | Ponte Java entre o navegador e o Forge |
| `server/tests/` | Cenários de integração com o Forge real |
| `server/BUILD.md` | Compilação da ponte e execução dos testes |
| `server/LEIA-ME.md` | Instalação e uso do pacote executável |
| `server/VALIDACAO.md` | Casos testados e limites da validação |
| `PUBLICAR-NO-GITHUB.md` | Como enviar este projeto ao GitHub |

## Desenvolver a interface

Requisitos: Node.js 22.13 ou superior e npm.

```sh
npm ci
npm run dev
```

Abra o endereço exibido pelo Vite. Para jogar, conecte a interface ao servidor
Java local ou a seu endereço HTTPS. O servidor pode ser o da distribuição
alpha 0.3; ele continua necessário mesmo durante o desenvolvimento da interface.

```sh
npm run typecheck
npm test
npm run build
```

O build escreve a interface pronta em `server/web`.

## Compilar o servidor e montar uma distribuição

Também são necessários Java/JDK 17+, Maven 3.9+, Python 3 e Git. Os passos
detalhados estão em [server/BUILD.md](server/BUILD.md).

1. Clone o Forge em `.deps/forge` e faça checkout da revisão de `FORGE_REVISION`.
2. Copie a pasta `server` deste projeto para `.deps/forge/forge-web`.
3. No `pom.xml` da raiz do Forge, ajuste a lista de módulos conforme `server/BUILD.md`.
4. Na raiz do Forge, execute a compilação Maven indicada nesse documento.
5. De volta à raiz deste repositório, execute `npm ci` e `npm run build`.
6. Com os fontes registrados no Git, execute:

```sh
python server/distribute.py .deps/forge releases
```

O script gera `releases/Commander-Table-alpha-0.3.zip`, com servidor, dependências,
recursos, interface e os fontes correspondentes. O diretório `releases` é ignorado
pelo Git. Para disponibilizar o programa, anexe esse ZIP a uma **Release**.

## Limites

O motor precisa ficar aberto no computador anfitrião. Novos humanos entram antes
do início; uma IA não é substituída durante a partida. Reiniciar o servidor perde
a sala. Não há gravação de partidas, chamada de voz/vídeo ou paridade completa
com o EDHLab.

Listas salvas pertencem ao navegador e à origem do site. Exporte o backup antes
de trocar de computador, endereço de túnel ou limpar dados do navegador.
As artes dependem da disponibilidade do Scryfall.

## Licenças e créditos

O código da ponte e as novas partes deste projeto são distribuídos sob
**GPL-3.0-or-later**, em continuidade com o Forge. Consulte [LICENSE](LICENSE).
As bibliotecas e os recursos de terceiros mantêm suas próprias licenças e avisos,
incluindo `vendor/` e os avisos dos recursos do Forge.

Os MP3s foram copiados sem alterações de `forge-gui/res/sound` na revisão indicada
em `FORGE_REVISION`; veja `public/sounds/README.md`. Magic: The Gathering e suas
artes pertencem aos respectivos titulares. As artes não integram este repositório.

Referências: [Forge](https://github.com/Card-Forge/forge),
[Scryfall API](https://scryfall.com/docs/api).
