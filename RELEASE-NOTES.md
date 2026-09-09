# Commander Table alpha 0.5

Twelve original Commander precons with twelve separate AI adaptations, expanded card inspection, four-AI tables and initial Commander AI improvements.

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
