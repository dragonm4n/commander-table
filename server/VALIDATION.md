# Alpha 0.5 validation

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
