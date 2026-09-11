# Alpha 0.6 validation

- Production UI build and TypeScript checking pass; all 18 browser-logic tests pass.
- `AiDecisionTest` passes 208 native assertions: existing threat/attack/block checks, missing commander colors, repeated colored pips, native land tutor selection, bounded commander/Elf strategy preferences, preservation value, and Myriad copies/defenders after an opponent concedes.
- `choices.py` with `ChoicesScenarioServer` casts Opt, Preordain, Cultivate, Demonic Tutor and Boros Charm through real human controllers and HTTP. It verifies scry 1/2, pay and decline choices for Rhystic Study, public versus private tutor announcements, and the chosen indestructible mode.
- `etb.py` passes native cast/ETB, optional target bounds, protection, damage and provisional blockers. Its fixture also verifies hideaway visibility, omission of incidental exile links, inclusion of return-on-leaving links, and tutor identity permissions.
- `smoke.py` passes catalog/version checks, two humans plus two AIs, private zones, automatic priority and authenticated restart.
- Compiled Edge checks cover the English switch, automatic disabling at a human turn, the scry window and return flow, zone arrivals, sickness indicator, and existing desktop/mobile regressions. No browser exceptions were reported. Remote artwork is stubbed in the visual test.
- These tests validate specific decisions and rules/UI flows; they do not establish general AI piloting accuracy or improved match win rates. Commander strategy preferences use existing Forge deck hints, and exile grouping uses recognized native relationships and ability references.

Previous validation history follows.

---

# Alpha 0.5 validation

## Local AI-series preview

- `AiSeriesTest` checks duplicate result suppression, draws, aggregation when the same deck occupies two seats, stopping after the current result, and excluding an interrupted game's artificial concession winner.
- `ai_series.py` with `SeriesScenarioServer` exercises three native games with deterministic outcomes: seat 1 wins, a draw, then seat 2 wins. It verifies custom names, exact game count, no unrequested native draw replay, per-seat totals, retained results in the lobby, and stop/interruption behavior. These forced-outcome fixtures test lifecycle/accounting, not strategic win rates.
- `smoke.py` still passes a normal two-human/two-AI match, automatic priority, private zones and live restart against the new HostedMatch lifecycle overlay.
- `restart_ai.py` passes three successive early restarts of ordinary four-AI matches, retaining the same authenticated lobby.
- Compiled Edge checks cover the summoning-sickness badge, series setup and submitted names/count/speed, the results table and JSON download, alongside previous interface regressions. TypeScript and production builds pass. Screenshots disable remote artwork requests.
- The run is bounded to 100 games, but there is no turn/time cutoff for a game that never ends. The host can stop future games or interrupt the current one. Results live in server memory and should be exported before starting another run or restarting Java.

## Local automatic-priority preview

- Compiled Edge regression verifies identical battlefield position and dimensions before and during the combat popup. The notice is dismissible and leaves the battlefield layout intact.
- Browser checks verify marking Sem interação sends automatic passes during an AI turn, unmarking stops scheduled passes, and a human turn does not pass automatically. The switch remains independent of the normal action buttons' busy state.
- `smoke.py` uses the actual `passAI` action in a two-human/two-AI match and checks auto-pass is unavailable during human turns or pending decisions; the match progresses and live restart still passes.
- The native guard accepts only a current-version ordinary InputPassPriority OK input in an AI turn. It cannot confirm targets, blockers, optional abilities, or Forge's longer-term yield suggestions. Automatic passing is per browser/seat and does not transfer the human deck to AI control.

## Local interaction preview — 2026-09-10

- `AiDecisionTest` passes 197 native checks. New Akroma's Will scenarios cover empty/quiet boards, no attackers, low-impact attacks, lethal damage, a meaningful attacking board and postcombat. A native Charm AI call verifies the timing gate is actually used. Existing 189 threat/attack/block checks pass. The gate retains native removal/mode evaluation; it does not measure match strength or solve every mass-pump spell.
- `etb.py` with `ScenarioServer` passes a real Angel of the Ruins cast, source-aware 0–2 target selection and exile of both selected artifacts, then the existing protection, token, damage and provisional-blocker add/remove/re-add regressions. Fixture mana was expanded to accommodate the extra seven-mana spell. Native face-down linked-exile projection preserves its source ID without exposing card text or artwork to another player.
- `smoke.py` passes against the runnable interaction package: all 24 catalog choices, private zones, human and AI play, host-only live restart and a second match in the same room.
- All 18 Node tests pass, including linked exile grouping and missing-source behavior. TypeScript and the production build pass.
- `ui-preview.cjs` passes compiled Edge checks for explicit-only inspection, larger dock, attack/block guides, selected defender, source-backed optional choices, confirmation before zero targets, normal-mode nonblocking queued announcements and the searchable deck catalog. Existing turn, zoom, minimized-choice, monarch, damage, restart and outcome checks remain covered. Desktop/mobile screenshots are inspected with artwork requests disabled.
- Automatic announcement queues are bounded: the server keeps up to 64 recent public events for 15 seconds and each player's browser queue holds up to 12. Ordinary announcements display for four seconds each; this is not durable replay after disconnect. Hidden card identities remain governed by Forge.

## Local deck and combat preview — 2026-09-09

- The runnable `Commander-Table-decks-preview` package passes `DeckCatalogTest`: 12 originals and 12 separately named adaptations, all legal 100-card Commander decks. All 56 substitutions remove native AI-exclusion flags; all adapted lists have zero flagged copies. Coverage includes lands and is not measured decision accuracy.
- `AiDecisionTest` passes 189 checks against the packaged engine. The additional 60 repeated scenarios verify Lathril attacks through zero or one Baneslayer Angel but avoids two lethal blockers. Four checks cover direct self-triggered combat rewards. The unpatched engine incorrectly declined the one-blocker attack despite menace.
- TypeScript checking, production UI build and all 17 Node tests pass. Headless Edge checks desktop/mobile layout, Command before other zones, reduced hand dock, original/adapted selection, warning details, replacement lists and the coverage disclaimer, alongside the existing interface regressions. Screenshots were inspected; card artwork requests were disabled.
- Original source lists and adaptation mappings ship in `server/catalog`; the five new originals come from linked Wizards decklists. Forge validates card availability, color identity, singleton rules and card counts at startup.
- `smoke.py` passes against the final runnable package: all 24 deck/commander selections, rejection of four retired entries, two human clients and two native AIs, hidden hands/libraries, human land play, AI permanents and live host-only restart retaining credentials and decks. A new match starts in the same room.
- This section records the latest Windows package checks; older platform and catalog statements below describe earlier builds. Full-match strategic quality and every card interaction remain unmeasured.

## Local gameplay preview — 2026-09-09

- `AiDecisionTest` contains 125 real-engine checks. Twenty repeated 1/1-token versus 8/8 scenarios pass at 40 life. Separate cases cover trample, flying, menace, tapped blockers, small attacks and token copies with mana abilities and damage that bypasses blockers. This is a bounded blocking heuristic, not a full-match strength benchmark.
- `restart_ai.py` passes three consecutive early restarts of the production four-AI match.
- `smoke.py` additionally verifies host-only restart during a live two-human match, retained credentials/decks, cleared ready flags and starting another match in the same room.
- `spectator.py` additionally covers native monarch transfer, cast/ability announcement spacing and restarting both a finished and an active four-AI match. Runtime logs are checked for exceptions.
- `ui-preview.cjs` additionally covers crown transfer, per-commander damage details, mobile hand/command indicators, timed card balloons surviving stack removal and restart confirmation without another host-key entry.

## Seven-precon release — alpha 0.5

- The packaged server starts with exactly seven precons: Draconic Domination, Vampiric Bloodlust, Breed Lethality, Elven Empire, Undead Unleashed, Lorehold Legacies and Planar Portal. Every list passes Forge's Commander deck-conformance validation at startup.
- `smoke.py` checks the `alpha-0.5` API version, all seven commander identities, successful selection of each deck and rejection of all four retired catalog entries. A real game with two humans and two AIs passes the existing privacy and land/permanent checks.
- TypeScript checking, the production UI build and all 17 Node tests pass. UI/package metadata is `alpha 0.5` / `0.5.0-alpha.5`; the Forge engine revision remains unchanged.
- The earlier sections below record the interface/AI checks performed before this catalog refresh. They are not a full-match benchmark for every new deck.

## Interface and four-AI preview — 2026-09-08

- `spectator.py` runs four native AIs through the real server. Checks host-only mode selection, refusal while human guests remain, spectator action rejection, game progress, visible public permanents and private hands/libraries. A test-only fixture concedes players on the engine's turn event to verify eliminated seats remain visible and final winner metadata survives spectator match cleanup.
- `ui-preview.cjs` checks the compiled UI in Edge with synthetic API data: own-turn color, end-turn confirm/cancel, stale confirmation dismissal, no extra confirmation on an opponent's turn, card zoom, choice minimization with preserved order, zone-window minimization, defeat and victory. Desktop and mobile screenshots are written to `server/target` (artwork requests are disabled).
- Java compilation, TypeScript checking, standalone production build and all 17 Node tests pass. These are targeted interface/integration checks, not a comprehensive test of every card or full AI match strategy.

## Local AI and combat preview — 2026-09-08

- Compiled the bridge and AI overlay with JDK 17 against the packaged Forge dependencies.
- The generated runnable preview passes `smoke.py` via `java -jar`: two human clients plus two patched native AIs, private zones, human land play and AI permanents (32 actions).
- `AiDecisionTest.java`: 98 assertions across real-engine scenarios, including 30 repeated selections for each of three defender-ranking cases. Covers resource threat versus low life, individual commander lethal, incoming commander danger, tapped/sick commanders, blockers, separate partner damage, Platinum Angel, Propaganda and a commander owned by another player. This is scenario validation, not a full-match win-rate benchmark.
- All 17 Node tests pass, including provisional arrow redirection, multiple blockers, original-defender preservation and undo. TypeScript checks and the standalone production build pass.
- The real-engine HTTP ETB scenario passes and verifies adding, removing and re-adding a blocker reaches all four seats during `COMBAT_DECLARE_BLOCKERS`, before confirmation.
- Inspected the compiled UI in headless Edge at 1440×900 and 390×844 using a synthetic API fixture: active name/icon, both combat arrows, removal and turn transfer pass, with no browser JavaScript errors. Artwork requests were disabled for this layout check.
- The preview changes AI sources through `apply_forge_patches.py`; the original alpha 0.4 validation below describes the earlier, unmodified engine release.

The Java bridge is built with Forge core, game, AI and GUI modules at revision `53a103721d627ecb76a2ea52b2febe894844f288`, using Java 17. The UI passes TypeScript checks and production builds for the hosted frontend and standalone Java distribution. No upstream rules-engine source was changed.

## New table and multiplayer regression

`tests/table.py` uses four real human controllers over HTTP. `TableScenarioServer.java` installs the initial board while the game waits for input; subsequent casting, mana payment, targeting, attachment, search, token creation and turn progression use the normal API and native Forge engine. Test entry points are compiled separately and are not in the production JAR.

Verified cases:

- Two simultaneous join requests with the same persisted recovery key receive the same seat and token, rather than occupying two seats. Repeated host creation also restores the existing table.
- Disconnect preserves deck confirmation and the occupied seat. Resume restores that exact seat, room, session token and private recovery key.
- Releasing a lobby seat revokes its old session and private rejoin link. Another player using the same display name cannot claim a reserved seat.
- Public room state does not contain session tokens or recovery keys. New humans cannot join after the start, and a playing seat cannot be released through the lobby operation.
- A foreign-controlled Aura exports its actual controller and the public permanent it enchants.
- A native Equip activation attaches Equipment to its chosen creature. A cast Aura targets and attaches to that same creature; native power changes are reflected in the snapshot.
- A native token spell creates two separately addressable tokens. Applying the browser grouping code to this actual Forge snapshot produces one ×2 group, with both Equipment and Auras under their host across controllers.
- A library search exports visible, authorized card options with `presentation: library`. Other players still receive no private hand or library identities.
- Disconnecting while that search is pending and resuming preserves its decision ID, options and original seat. Selecting a basic land resolves it into the battlefield and clears the search's stack announcement.
- Returning the enchanted creature to hand through a native spell detaches its Equipment and removes its Aura from the battlefield.
- Four normal player turns show Round 1; the next normal rotation shows Round 2. Initial Round 1 is seeded after Forge enters its first turn, before normal event subscriptions begin.

`RoundTrackerTest.java` separately covers first-player order, duplicate event delivery, extra turns, skipped turns and eliminated players. These edge cases are tracker tests; the native HTTP scenario covers a normal four-player rotation.

## Stack and existing feature regression

`tests/stack.py` passed with real casting, targeting, responses and resolution:

- A counterspell selects and counters the intended creature spell. A second counterspell answers another counterspell in a three-item stack, allowing the original creature and its entry trigger to resolve.
- Exported stack targets identify their exact items and source cards.
- Cast and activated-ability announcements exist while pending and disappear after resolution or removal, including countered spells.
- Delayed mana adds exactly the expected colorless amount at the next main phase; a countered spell adds none. A subsequent artifact is paid for using that mana, including the explicit mana button, while lands remain untapped.
- Two counters are exported with the readable `+1/+1` name.
- Token printing metadata and the native token sound event are present; no card-script-specific audio filenames are forwarded.
- Other players' hands remain hidden.

`tests/smoke.py` passed with two human HTTP clients and two native Forge AIs: host authentication, invitation, deck confirmation, private hands/libraries, human land play and AI permanents. The alpha 0.4 startup log confirms that `en-US` is loaded.

`tests/etb.py` also passed against alpha 0.4: cast-dependent protection, a token entry trigger, player/permanent targeting, protection expiration on the next individual turn, and real attack/block relationships. The fixture waits for its completion marker before normal client actions begin.

## Browser logic and packaging

Fourteen isolated Node tests pass across `lib/battlefield.test.mjs`, `lib/sessions.test.mjs` and `lib/decks.test.mjs`:

- Identical token grouping and preservation of each member ID; different counters, damage, combat/tapped states, power, text and selection status stay separate.
- Nested attachments, foreign-controlled Auras, missing hosts and ungrouped attached or face-down cards.
- Recovery after tab closure/disconnect, stable join keys before HTTP, isolation between rooms/servers, credential replacement, explicit release, private-link fields and damaged/full storage.
- Deck list persistence, duplicate-free renaming, complete backup merging and preservation of existing data when an import/write fails.

The 39 UI sound files are unchanged copies of the pinned Forge resources. The release includes English run/tunnel scripts and installation/build instructions. ZIP contents include the matching production bridge, UI files and corresponding source; test-only entry points are excluded from the production JAR.

## Limits of verification

There was no automated browser visual inspection or audible playback check. The layout and responsive styles were implemented and compiled; ergonomics still need feedback from actual player screens. Windows and macOS launchers were not executed on those operating systems; native integration testing was on Linux.

These scenarios do not cover a full match or every card interaction. Unusual ordering, replacement effects and complex combat may need their own regression case when a concrete problem is reported. Card/token image availability still depends on Scryfall. Audio also depends on the browser's Sound click and the device.

Recovery tests cover a running server. No game recovery is supported after a Java restart, and browser storage does not synchronize deck lists across devices.
