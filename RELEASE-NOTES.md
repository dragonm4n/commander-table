# Commander Table alpha 0.4

This release improves battlefield readability, card selection and multiplayer seat recovery.

- English interface, Forge prompts, launchers and documentation.
- Overlapping Equipment, Auras and other attached permanents, with individual inspection and interaction.
- Identical tokens grouped with quantity badges; tokens with different game states remain separate.
- Searchable popup windows for library choices, reveals and card ordering.
- Commander names beside player names, including partners.
- Full-table Round counter, separate from Forge's individual turn tracking.
- Spell and ability announcements clear when their stack item resolves or leaves the stack.
- Reconnection restores the same seat, deck, private hand and pending choice while the server remains running.
- Separate Disconnect and Release seat actions, plus a private rejoin link for another browser or device.

Stack interactions, native attachment/search actions, round tracking and multiplayer recovery are covered by automated regression scenarios. The test coverage and remaining limits are documented in `server/VALIDATION.md`.

To update, finish the current game, stop the old server and extract the complete runnable ZIP into a new folder. Run `start-windows.bat` on Windows or `bash start.sh` on Linux/macOS, then reload the interface. Both the Java server and web files must be updated together. Existing games do not survive a server restart.

This is an experimental release. New humans join before the game starts; unused seats are filled by Forge AI. There is no saved-game recovery across server restarts.
