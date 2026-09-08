# Construir a ponte Forge

Requisitos: Java 17+, Maven 3.9+ e Git. A interface requer Node 22.13+.

1. Clone https://github.com/Card-Forge/forge e faça checkout de `53a103721d627ecb76a2ea52b2febe894844f288`.
2. Copie esta pasta `server` para `forge-web` na raiz do Forge (fontes Java e pom.xml bastam; não copie pacotes de distribuição de volta).
3. No pom.xml da raiz, substitua a lista de módulos por `forge-core`, `forge-game`, `forge-ai`, `forge-gui`, `forge-web`. Mantenha as demais configurações de upstream.
4. Execute na raiz: `mvn -B -pl forge-web -am package -Dmaven.test.skip=true -Dcheckstyle.skip=true`.
5. Copie `forge-web/target/forge-web-2.0.15-SNAPSHOT.jar` para `commander-table.jar`, `forge-web/target/lib` para `lib`, e `forge-gui/res` para `forge/res`.
6. No projeto da interface, execute `npm ci` e `npx vite build --config vite.standalone.config.ts`. Copie a saída `server/web` para `web`, ao lado do JAR.
7. Copie os scripts de execução e `LEIA-ME.md`. Inicie com `java -Xmx3G -jar commander-table.jar --assets forge --port 8787`.

No pacote distribuído, `source/forge` contém os módulos Java correspondentes, pom raiz adaptado, configurações de build, e `forge-web` da ponte. Os recursos estão em `../../forge/res`, compartilhados com a distribuição; copie-os para `source/forge/forge-gui/res` para recompilar/executar na estrutura original. `source/web-ui` contém as fontes da interface (sem dependências, obtidas por npm ci). Nenhuma regra do Forge foi reimplementada no navegador.

O build ignora os testes de toda a distribuição nativa; isso não equivale a ter validado todos eles. O teste de integração da ponte é separado, em `forge-web/tests/smoke.py`, e executa uma partida real via HTTP.

## Executar a regressão de entrada e alvos

Na pasta da ponte (com o Forge compilado), compile o carregador de cenário fora das fontes de produção:

```sh
javac -cp 'target/forge-web-2.0.15-SNAPSHOT.jar:target/lib/*' -d test-classes tests/ScenarioServer.java
javac -cp 'target/forge-web-2.0.15-SNAPSHOT.jar:target/lib/*' -d test-classes tests/StackScenarioServer.java
python tests/etb.py target/forge-web-2.0.15-SNAPSHOT.jar ../forge-gui test-classes
python tests/stack.py target/forge-web-2.0.15-SNAPSHOT.jar ../forge-gui test-classes
python tests/smoke.py target/forge-web-2.0.15-SNAPSHOT.jar ../forge-gui
```

No Windows, use `;` para separar os elementos do classpath. Os testes iniciam e encerram seus próprios processos Java nas portas 8788/8789/8791. Os carregadores aceitam comandos apenas na entrada padrão do processo de teste; não adicionam rotas de teste ao servidor normal.

Na pasta da interface, execute `node --experimental-strip-types --test lib/decks.test.mjs` para verificar a recuperação de decks, a fusão de backups e a preservação dos dados em caso de falha. Os testes usam armazenamento isolado e não leem decks pessoais.

Os efeitos sonoros são copiados de `forge-gui/res/sound` para `public/sounds` da interface. Vite inclui essa pasta automaticamente em `server/web/sounds`; a distribuição mantém os recursos correspondentes do Forge.
