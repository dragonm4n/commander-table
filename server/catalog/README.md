# Commander Table deck catalog

`catalog.json` preserves each original and defines explicit one-for-one AI substitutions. The loader validates every list using Forge Commander rules and rejects unavailable or AI-flagged replacements.

New decklists are transcribed from the linked official Wizards articles. `sourceSha256` is the SHA-256 of the original main-deck text after HTML entity decoding and outer whitespace trimming. Commit /// Memory is normalized to Commit // Memory for Forge; card quantities are unchanged.

Coverage = 100 × unflagged card copies / all Main + Commander copies, including lands. It is not measured piloting accuracy. Adaptations keep commanders, remain separately named, and expose every replacement through the health API.
