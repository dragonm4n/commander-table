# Commander Table alpha 0.5

Seven bundled Commander precons, expanded card inspection, four-AI tables and initial Commander AI improvements.

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
