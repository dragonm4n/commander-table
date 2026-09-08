# Build the Forge bridge

Requirements: Java/JDK 17+, Maven 3.9+, Python 3 and Git. The interface requires Node.js 22.13+ and npm.

1. Clone https://github.com/Card-Forge/forge and check out `53a103721d627ecb76a2ea52b2febe894844f288`.
2. Copy this `server` directory into `forge-web` at the Forge root. Java sources and `pom.xml` are sufficient for compilation; do not copy distribution archives or local profiles back into the build.
3. In Forge's root `pom.xml`, replace the module list with `forge-core`, `forge-game`, `forge-ai`, `forge-gui`, `forge-web`. Keep the other upstream configuration.
4. From the Forge root, run:

```sh
mvn -B -pl forge-web -am package -Dmaven.test.skip=true -Dcheckstyle.skip=true
```

5. From the UI project root, run `npm ci` and `npx vite build --config vite.standalone.config.ts`. This writes the static interface into `server/web`.
6. Commit the project source so the distribution script includes every authored file, then run `python server/distribute.py PATH_TO_FORGE NEW_OUTPUT_DIRECTORY`.

The output is `Commander-Table-alpha-0.4.zip`. The output directory must not already contain a `Commander-Table` folder. It includes `commander-table.jar`, dependency JARs, resources, web files, English launchers and corresponding source. To assemble manually, copy the bridge JAR from `forge-web/target`, its `lib` folder, Forge's `forge-gui/res` into `forge/res`, and `server/web` into `web` beside the JAR. Also copy `support/forge.profile.properties` into `forge`, plus the launchers, README, validation notes and license. Start with `java -Xmx3G -jar commander-table.jar --assets forge --port 8787`.

## Rebuild from the runnable package's source

`source/forge` contains the four upstream modules, adapted root POM, build configuration and `forge-web`. The resources are shared with the runtime at `../../forge/res`; copy them into `source/forge/forge-gui/res` before running in the original Forge layout. `source/web-ui` contains the UI source; install dependencies with `npm ci`, then build with `npx vite build --config vite.standalone.config.ts`.

The Maven command skips upstream tests. That is separate from the bridge regression suite below and does not imply the entire upstream test suite passed.

## Native HTTP regression tests

From the bridge folder inside a compiled Forge checkout, compile the test-only entry points separately:

```sh
javac -cp 'target/forge-web-2.0.15-SNAPSHOT.jar:target/lib/*' -d test-classes tests/ScenarioServer.java tests/StackScenarioServer.java tests/TableScenarioServer.java tests/RoundTrackerTest.java
java -cp 'test-classes:target/forge-web-2.0.15-SNAPSHOT.jar:target/lib/*' table.RoundTrackerTest
python tests/table.py target/forge-web-2.0.15-SNAPSHOT.jar ../forge-gui test-classes
python tests/stack.py target/forge-web-2.0.15-SNAPSHOT.jar ../forge-gui test-classes
python tests/etb.py target/forge-web-2.0.15-SNAPSHOT.jar ../forge-gui test-classes
python tests/smoke.py target/forge-web-2.0.15-SNAPSHOT.jar ../forge-gui
```

On Windows, use `;` instead of `:` between classpath elements. Tests start and stop their own Java processes on ports 8788, 8789, 8791 and 8792. Scenario setup uses only the test process's standard input, never a production API endpoint. Fixtures must not be compiled into the production JAR.

`table.py` exercises concurrent joins, release/rejoin, exact seat recovery during a library choice, equipment and Aura attachment, token creation and table rounds. It writes a public battlefield snapshot for inspecting token grouping. `stack.py` checks counterspell interactions, cleared announcements, delayed colorless mana, counter labels and sound metadata. `etb.py` covers entry triggers and target/combat projections. `smoke.py` exercises two humans with two native AIs.

From the UI root, run:

```sh
npx tsc --noEmit
node --experimental-strip-types --test lib/*.test.mjs
```

These tests use isolated storage and do not read personal deck lists or credentials. They cover token grouping/attachments, session recovery and deck backups. The library search modal uses Forge's authorized choice options rather than exposing the entire library.

Sound assets are copied from the pinned Forge `forge-gui/res/sound` directory into UI `public/sounds`. Vite includes them automatically in the standalone build. Their provenance is in `public/sounds/README.md`.
