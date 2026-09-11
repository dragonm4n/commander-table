# Commander Table alpha 0.6

Commander-focused AI, clearer choices and public action feedback. Includes the previous local interface and AI-series improvements.

- Land tutors consider missing colored pips in the AI's hand and command zone, including repeated pips and colored mana sources. Native balancing remains the fallback.
- A bounded casting preference favors commanders and cards matching Forge's explicit commander deck hints. Commander creatures receive additional value in native preservation and target comparisons. These heuristics do not provide complete strategic understanding or measured win-rate gains.
- Tutor announcements identify chosen cards and actual destinations, including library top/bottom. Unrevealed searches keep the identity private to the searching player; opponents and spectators see a hidden-card notice.
- Modal spell and ability announcements display the chosen modes.
- A spiral marks summoning sickness. Minimal animations above Hand, Graveyard and Exile indicate card arrivals without exposing identities or changing the battlefield layout; reduced-motion settings are respected.
- Myriad's per-opponent copies exclude eliminated players, including elimination before the trigger resolves.
- **No interaction** sits above the End Turn controls and automatically switches off when any human turn begins. Required choices still wait for the player.
- Scry offers explicit top/bottom choices in a window, with View battlefield and Return to choice. Multiple-card arrangements support separate top and bottom groups and ordering.
- Optional effect taxes offer **Pay [cost]** and **Don't pay**, followed by native payment when accepted.
- Exiled cards sit beneath a permanent only when there is a recognized future relationship, such as hideaway, imprint or return when the permanent leaves.

Install the complete package into a new folder; the engine, Java bridge and UI all changed. Local build, not yet published to GitHub.

---

# Commander Table alpha 0.5

Twelve original Commander precons with twelve separate AI adaptations, expanded card inspection, four-AI tables and initial Commander AI improvements.

## Local AI-series preview (not yet published)

- Creatures with summoning sickness display an hourglass badge, using Forge's current state and its haste exception.
- Four-AI setup supports custom names and series of 1–100 consecutive games with the selected decks. Each new game reshuffles and starts through the native engine.
- Optional fast testing removes spectator presentation delays. The server runs the series even if the viewing browser disconnects; restarting the Java server loses the in-memory run.
- The results window tracks wins and games per AI/deck, draws, aggregate deck appearances and individual outcomes. Export the report as JSON before starting another run.
- Stop after the current game, or interrupt through Restart. Completed results remain available in the lobby; interrupted or failed games are excluded from win totals.
- Controlled match cleanup prevents Forge from automatically replaying draws outside the requested series count. These are matchup results from the tested decks, not a general deck-strength or piloting-accuracy rating.

## Local auto-pass preview (not yet published)

- Combat declaration notices now float above the table and can be dismissed without moving or resizing any battlefield.
- Adds the toggleable **Sem interação** switch to the action dock. While enabled, your browser passes your priority during AI turns, with a short delay. It pauses on any human turn and on required choices, target selection or blocking. Each human controls their own switch; it starts off when entering a match.
- Unchecking cancels the next scheduled pass. The server rechecks the current input, AI turn and control version before accepting an automatic pass, so a stale request cannot confirm another kind of decision. The switch remains available while other actions are busy.

## Local interaction preview — September 10, 2026 (not yet published)

- Linked exiled cards, including hideaway cards, appear beneath their source while it remains on the battlefield. The linked-card viewer preserves Forge's face-down visibility rules.
- Increased hand/action dock height. Card art inspection now opens explicitly through the magnifier; hovering battlefield cards or choice rows no longer opens an obstructing inspector.
- Dedicated attacker/blocker instructions, defender selection buttons and an outlined selected defender make combat choices visible before arrows appear.
- Target selection displays its source, minimum/maximum and selected count. Continuing with zero optional targets requires confirmation. Angel of the Ruins uses “up to two targets,” rather than a separate may confirmation.
- Card-backed confirmation dialogs retain their source card. Casts, activations and triggers have queued four-second announcements in human and spectator matches. Human-match announcements do not intercept clicks and shrink during target selection; no extra engine delays are added to human matches.
- Deck selection uses a larger searchable catalog, original/adapted filters, commander art, complete main-deck lists and visible substitutions.
- A conservative Commander-specific Akroma's Will timing gate preserves it outside an actual combat or removal response and avoids low-impact attacks. Native mode and threat evaluation still decides whether to cast. This is not a complete combat simulation or a change to every mass-pump spell.

Validated with 197 native AI checks, 18 browser-logic tests, desktop/mobile Edge checks and real-engine ETB/combat and restart integration tests.

## Local deck and combat preview (not yet published)

- Adds original Veloci-Ramp-Tor, Explorers of the Deep, Blood Rites, Eldrazi Incursion and Tricky Terrain lists from Wizards' published decklists. The seven existing originals remain available.
- Adds separately named AI adaptations for all twelve decks, with 56 explicit substitutions in total. Each adaptation preserves its commander and passes Forge's 100-card Commander validation.
- Displays AI card coverage and the individual warnings and substitutions. Coverage counts card copies without Forge's AI-exclusion flag, including lands; **100% is not measured piloting accuracy** or a guarantee of correct decisions.
- Fixes an attack rejection when a lone first-strike creature cannot legally block a menace attacker, reproduced with Lathril. Commander combat evaluation also recognizes direct self-triggered token creation and card draw from attacks or combat damage. Lathril creates tokens on combat damage, not on attack declaration.
- Enlarges battlefield space, places Command on the left and the remaining zone controls on the right, and uses a pennant for commanders so the monarch retains a distinct crown.
- Validated with 189 native AI decision checks, all 24 deck lists, 17 browser-logic tests and desktop/mobile browser checks. These checks do not measure full-match win rates.

Official lists: [The Lost Caverns of Ixalan](https://magic.wizards.com/en/news/announcements/the-lost-caverns-of-ixalan-commander-decklists) and [Modern Horizons 3](https://magic.wizards.com/en/news/announcements/modern-horizons-3-commander-decklists). All substitutions are documented in `reports/precons-adaptados-ia.md`.

## Local gameplay preview (not yet published)

- Commander AI uses spare vanilla 1/1 tokens to absorb unblocked hits of six or more damage. It retains Forge's legal-block checks and skips trample, blocking taxes, valuable abilities and an available Fog.
- A crown identifies the monarch and follows the native game's ownership changes.
- Hand count joins the zone bar; the command zone has a gold highlight and an additional glow when your commander is actionable.
- A commander-damage button beside life shows the highest individual total. Open it to inspect each commander's separate damage history.
- The host can end a match and return everyone to deck selection, keeping the same room, credentials, players and decks. Human players reconfirm their decks before starting again.
- Four-AI matches show enlarged card announcements in the acting player's battlefield. Casts and stack abilities pause for three seconds, land plays and resolutions for one second, and phase transitions for 0.4 seconds. Announcements remain visible for four seconds unless replaced by that player's next action. Human tables retain their existing pace.

## Deck catalog

Retained: **Draconic Domination** (The Ur-Dragon) and **Vampiric Bloodlust** (Edgar Markov).

Added:
- **Breed Lethality** — Atraxa, Praetors' Voice: counters and proliferate.
- **Elven Empire** — Lathril, Blade of the Elves: Elves.
- **Undead Unleashed** — Wilhelt, the Rotcleaver: Zombies and sacrifice.
- **Lorehold Legacies** — Osgir, the Reconstructor: artifacts and recursion.
- **Planar Portal** — Prosper, Tome-Bound: exile casting and Treasures.

Feline Ferocity, Plunder the Graves, Swell the Host and Seize Control are removed from the selectable catalog. The lists are the precons bundled with Forge, without custom upgrades. All seven are validated during server startup.

## Gameplay and interface

- Initial AI adjustments reduce low-life targeting bias and account for individual commander damage when evaluating threats and defenders.
- Bold active-player names and illuminated avatars; a teal action panel on your turn.
- Confirmation before ending your own turn.
- Block arrows appear before declaration is confirmed; attack arrows follow assigned blockers.
- Card-art zoom controls and minimizable choice/zone windows with preserved selections.
- Host spectator mode for four native AIs, with private hands and libraries.
- Persistent defeated-player notices and a visible victory banner.

## Install

Extract the complete runnable ZIP into a new folder. Stop the old server before using port 8787, then run `start-windows.bat` or `start.sh`. Java 17 or later is required. Games do not survive a server restart.

Forge remains pinned to `53a103721d627ecb76a2ea52b2febe894844f288` (2.0.15-SNAPSHOT), with the reproducible Commander Table AI overlay. Package version: `0.5.0-alpha.5`. See `server/VALIDATION.md` for test coverage; this release does not claim a measured full-match AI win-rate improvement.
