# Commander Table — alpha 0.5

A four-seat web Commander table powered by Forge's rules engine and native AI. The host runs the Java server; guests only need a browser. This package includes the web interface, served by the same program.

## Update from an older version

Finish the current game and stop the old server. Extract this entire ZIP into a **new folder** and run the new launcher. Both the Java bridge and the web interface changed. Open `http://localhost:8787` or the tunnel address and reload the page. Games and old room credentials do not survive a server restart. Saved deck lists remain in the browser when the site address stays the same.

## Start on Windows

1. Extract the whole ZIP. Do not run the launcher from inside the ZIP.
2. Install 64-bit Java **17 or later** if needed: https://adoptium.net/temurin/releases/?version=17 . The launcher allows Java to use up to 3 GB of memory.
3. Run `start-windows.bat` and wait for **server ready**. Keep the window open. It displays your **Host key**.
4. To play locally, open `http://localhost:8787`, connect, enter your name and host key, and create a table.
5. To invite remote friends, also run `tunnel-windows.bat`. On first use, it downloads **cloudflared for Windows x64** from Cloudflare's official GitHub repository.
6. Open the tunnel's `https://...trycloudflare.com` address. Use that same address in the interface and create the table with your host key.
7. Click **Invite friends** and share the invitation. Guests join before the game starts. Each human confirms a deck; the host can also select AI decks.
8. Click **Start game**. Forge controls the empty seats.

On Linux/macOS, use `bash start.sh` and, after installing cloudflared, `bash tunnel.sh`. Preferences and cache stay in the package's `profile` folder. This release was built and exercised on Linux with Java 17; the Windows and macOS launchers still need validation on those systems.

## Play

- Before starting, the host can enable **Watch 4 AIs** in the lobby. All four seats use native Forge AI; the host can choose all four decks and then watch. Human guests must release their seats first. The spectator cannot play cards or see private hands/libraries.
- Card previews have magnifying-glass buttons and a zoom slider below the image. Enlarged art can be scrolled. Click a card's magnifier to keep its preview open.
- **View battlefield** minimizes a large choice or zone window. Use **Return to choice** (or **Return to** the zone) to reopen it. Pending selections, their order and entered amounts are preserved.
- During your own turn, the action panel beside your hand is teal. **End Turn** asks for confirmation before passing your turn; ordinary confirmations and passing on another player's turn keep their normal behavior.
- Eliminated players retain a **Defeated** notice on their battlefield. The winner receives a **Victory!** notice and a result banner after the game ends.

- All four battlefields are visible and equally sized. Lands occupy the lower lane; creatures and other permanents occupy the upper lane. Scroll each lane when needed. Your private hand sits at the bottom.
- **Round** counts one full rotation of normal turns around the table. It starts at 1 and advances when the next rotation begins. Extra turns stay in the current round; skipped turns and eliminated players do not block the counter. Forge still tracks individual turns for card rules; hover over Round to see that number.
- The dots show the phases in order; the current phase is gold. Hover for the full name. Commander names appear beside player names, including both partners.
- The active player's name is bold and gold, with an illuminated avatar and header, including on small screens.
- Equipment, Auras and other attached permanents appear behind their host with an offset. Click the link badge to inspect or select any attached card. An Aura controlled by another player is displayed with its host. Effects attached to a player show that player's name.
- Identical tokens form a group marked **×N**. Click the group to select an individual token. Different counters, tapped states, damage, power/toughness, combat status or selection state keep tokens separate. Attached tokens remain separate as well.
- Click cards to cast, activate or select them. Click a player's portrait or life total to select that player. Forge decides what is legal and handles all changes to life, zones, mana and counters.
- Priority controls stay beside your hand in the bottom-right corner. Library searches, reveals and ordering choices open a larger window with card images, search and inspection. Confirm the selection there. Only options authorized by Forge are shown.
- Player-header announcements link to the source card while a spell or ability is on the stack. They disappear when it resolves, is countered or otherwise leaves the stack.
- When choosing a stack target, select the highlighted item or its thumbnail option, then **Confirm target**. Use **Cancel selection** when cancellation is allowed.
- Gold arrows indicate declared targets; blue arrows indicate blocks as you assign them, before confirmation. A blocked attack's red arrow points to each assigned blocker; removing all blockers restores the arrow to its original defender. This is a visual aid, not a change to damage or trample rules. Toggle them with **Arrows**. Effects without targets do not create target arrows.
- Use the zone buttons for the command zone, graveyard, exile and authorized library reveals. Other players' hands and hidden library cards stay private.
- Hover over a card, or click its magnifying glass, to inspect it. Inside a library or token/attachment window, the preview appears within that window.
- Open **Stack**, **Chat** and **History** from the top bar. Enable **Sound** to hear the bundled Forge effects; click again to mute.
- Follow Forge's prompts for attacks, blockers, damage assignment and ordering. This is an automated rules engine, not a freeform card table.

## Disconnect and rejoin

Your browser saves your private seat credential before joining. Refreshing, closing and reopening a tab, or reopening the browser can recover the same seat, deck, hand and pending decision while the server remains running. Repeated join requests from the same browser also reuse that seat.

Use **Disconnect** to leave the interface while reserving your seat. The table may wait for your decisions. **Rejoin table** returns you to it. Before a game starts, **Release my seat** explicitly gives the place back; the host can instead **Close table for everyone**. During a game, use **Concede** if you want to be eliminated rather than temporarily disconnecting.

The Disconnect menu also provides a **Private rejoin link**. Keep it for yourself to resume from another device or browser. It grants access to your seat and private hand. The ordinary invitation is different: it lets new guests occupy free seats before the game starts. A matching display name does not grant access to an existing seat.

Saved credentials belong to the browser and site address. Changing tunnel addresses, clearing browser data or using private browsing can remove local recovery; keep your private rejoin link before switching devices. If the server restarts, the room is gone and neither kind of link restores the game. There is no game save or replay in this release.

## Decks

Seven preconstructed decks are included. To import, enter English card names, one per line, such as `1 Sol Ring`, and put commander names in their separate field. Partners use separate lines. Set/collector suffixes such as `1 Card Name (SET) 123` are accepted. Forge validates Commander construction and rejects unrecognized or unimplemented cards.

| Precon | Commander | Theme |
| --- | --- | --- |
| Draconic Domination | The Ur-Dragon | Dragons |
| Vampiric Bloodlust | Edgar Markov | Vampires |
| Breed Lethality | Atraxa, Praetors' Voice | Counters and proliferate |
| Elven Empire | Lathril, Blade of the Elves | Elves |
| Undead Unleashed | Wilhelt, the Rotcleaver | Zombies and sacrifice |
| Lorehold Legacies | Osgir, the Reconstructor | Artifacts and graveyard recursion |
| Planar Portal | Prosper, Tome-Bound | Casting from exile and Treasures |

These are the precon lists bundled with the pinned Forge resources, without custom upgrades.
The server validates all seven decks during startup and reports a missing or invalid list.

A successfully confirmed imported list is saved in **My decks** using the supplied name. Choose **Use list** to reuse it and confirm it for the seat. Reimporting the same list updates its name without duplication.

Lists are stored in this browser and site origin. **Export backup** creates a JSON file; **Import backup** restores and merges lists. Export before changing computer, browser, tunnel address or clearing site data. Deck storage is separate from game sessions; decks are not synchronized between players.

## Engine, images and sounds

Forge handles priority, counterspells, triggers, targeting, mana and combat. The bridge passes engine choices through even when they are triggered abilities rather than manual activations. Counter labels use readable engine names such as **+1/+1 ×2**. Token art uses the printing metadata supplied by Forge to request the correct Scryfall image; copied cards use their original card image. Names and rules text remain available if an image fails to load.

The 39 MP3 effects in `web/sounds` are copied unchanged from `forge-gui/res/sound` at the pinned revision. Event mapping uses Forge's `EventVisualizer`. Card-script-specific audio files are not loaded. Browser audio requires the player's Sound click.

## Limits

- One active table per server, with one to four humans and native Forge AIs in empty seats, or four AIs watched by the host.
- New humans join in the lobby. AI seats cannot be replaced after the game starts. A disconnected human keeps their seat.
- The host computer, Java process and tunnel must remain running. The temporary tunnel changes address when restarted and depends on Cloudflare availability.
- No game persistence across server restarts, additional spectator seats, voice/video, freeform drag-and-drop or full EDHLab feature parity. The host can spectate a four-AI table.
- The automated scenarios exercise real games but do not cover every card combination or a full match. See `VALIDATION.md` for coverage and remaining validation limits.
- Card images require Scryfall access. A full image collection is not bundled.

## Keys, source and credits

The host key creates tables; an invitation joins free seats; a private rejoin link or session token accesses one player's seat. Keep host and rejoin keys private. The host operates the process containing the complete game state; clients receive a player-specific projection.

Forge: https://github.com/Card-Forge/forge

Pinned revision: `53a103721d627ecb76a2ea52b2febe894844f288` (2.0.15-SNAPSHOT).

Corresponding Forge modules, build files and the Java bridge are in `source/forge`; the React UI source is in `source/web-ui`. See `source/BUILD.md`. Bridge and UI additions are GPL-3.0-or-later. Third-party libraries and assets retain their own notices, including those in JAR `META-INF` folders and the Forge resources. See `LICENSE`. Magic: The Gathering and card artwork belong to their respective owners. This is an independent project.

Cloudflare: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/

Scryfall: https://scryfall.com/docs/api
