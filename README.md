# Commander Table

A four-seat web Commander table powered by the [Forge](https://github.com/Card-Forge/forge) rules engine and native AI. The host runs a Java server; guests connect in a browser. Empty seats are filled by Forge AI when the game begins.

Current feature release: **alpha 0.6**. Independent project, unaffiliated with Wizards of the Coast, Forge, EDHLab or Scryfall.

## Features

- Four simultaneous, symmetric battlefields with separate land and permanent lanes and a private hand.
- English interface, engine prompts, launchers and documentation.
- Overlapping Equipment and Auras, including attachments controlled by another player.
- Identical tokens grouped with a quantity badge and individual card interaction.
- Searchable library-choice windows with card art and inspection.
- Commander names beside players, compact phase dots and a full-table Round counter.
- Forge-controlled stack, targets, combat, counters and mana; announcements clear when their stack item leaves.
- One to four humans, AI in remaining seats, or a host watching four AIs; persistent browser seat recovery and private rejoin links.
- Zoomable card previews, minimizable choices, own-turn confirmation and visible defeat/victory notices.
- Scryfall card/token art, bundled Forge sounds, chat and game history.
- Deck import, reusable browser-saved lists and JSON backups.
- Twelve original precons and twelve separate AI adaptations, with visible substitutions and a percentage of cards without known Forge AI-exclusion flags. This percentage is not measured piloting accuracy.

## Source layout

| Path | Content |
| --- | --- |
| `app/`, `components/`, `lib/` | Browser UI, state projection, saved decks and connections |
| `standalone/` | Static browser entry point for the local Java server |
| `public/sounds/` | Unmodified Forge MP3 effects and provenance |
| `server/src/` | Java bridge between the browser and Forge |
| `server/tests/` | Real-engine HTTP regression scenarios |
| `server/BUILD.md` | Native build and test instructions |
| `server/README.md` | Installation and player guide |
| `server/VALIDATION.md` | Tested cases and validation limits |
| `PUBLISHING.md` | Update this repository and create a GitHub release |

## Forge relationship

The bridge is built as Forge's `forge-web` Maven module, against revision `53a103721d627ecb76a2ea52b2febe894844f288` (2.0.15-SNAPSHOT). Card rules remain in Forge. The local AI preview applies the reproducible overlay in `server/forge-patches` using `server/apply_forge_patches.py`: it reduces the low-life targeting bias in Commander and adds individual commander-damage pressure to defender/threat evaluation. Apply it before compiling Forge; see `server/BUILD.md`. This is an initial heuristic adjustment, not a measured improvement in full-match win rate.

## Develop and build

Requirements: Node.js 22.13+ and npm; Java/JDK 17+, Maven 3.9+, Python 3 and Git for the native server.

```sh
npm ci
npx vite --config vite.standalone.config.ts
npx tsc --noEmit
node --experimental-strip-types --test lib/*.test.mjs
npx vite build --config vite.standalone.config.ts
```

The standalone build writes `server/web`. Use an **alpha 0.6 Java server** while developing the interface; older bridges do not provide the new attachment, library-choice or recovery metadata. In this GitHub checkout, `npm run dev`, `npm run build`, `npm run typecheck` and `npm test` provide the equivalent standalone commands.

Follow [server/BUILD.md](server/BUILD.md) to build the bridge. With source files committed, `python server/distribute.py PATH_TO_FORGE NEW_OUTPUT_DIRECTORY` creates the runnable `Commander-Table-alpha-0.6.zip`, including corresponding source.

## API and session behavior

`GET /api/health` checks the server. `POST /api/rooms` requires the host key. `POST /api/rooms/:id/join` joins a free lobby seat with an invitation; a persisted private `resumeKey` makes retries return the same seat. `POST /api/rooms/:id/resume` uses that private key to recover an existing seat during the game as well.

Seat-token routes provide `state`, `deck`, `start`, `chat`, `action`, `disconnect` and `leave`. Disconnect keeps the seat reserved; leave explicitly releases it in the lobby. Actions carry a request ID, control version and, when applicable, decision ID. Clients receive a player-specific projection excluding other hands and unauthorized library identities. The browser polls serially; Forge remains authoritative.

Round is presentation metadata for normal turn rotations; native individual turns and card rules are unchanged. Extra turns, skipped turns and eliminated players are accounted for by the separate round tracker.

## Limits and credits

The game lives in the host process's memory. Rejoining restores a running room, but restarting Java loses it. New humans cannot replace AIs mid-game. Decks and recovery credentials are stored per browser/site origin. Keep your private rejoin link and export deck backups before switching devices or tunnel addresses. There is no voice/video, game replay, persistent game saving or full EDHLab feature parity.

Bridge and UI additions are **GPL-3.0-or-later**; see `LICENSE`. Third-party assets and libraries retain their own notices. The runnable package includes the corresponding Forge sources and resources. Sound provenance is in `public/sounds/README.md`. Magic: The Gathering and its artwork belong to their respective owners; card images are retrieved from Scryfall and are not bundled.
