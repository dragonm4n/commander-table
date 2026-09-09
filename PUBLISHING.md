# Update Commander Table on GitHub

This source package updates **Commander Table alpha 0.5**. The existing repository is https://github.com/dragonm4n/commander-table . No additional Forge fork is needed for the current bridge and interface.

## Update an existing clone

1. Finish and commit any local changes you want to keep.
2. Pull the repository's latest changes.
3. Copy the contents of this source package into the existing clone, preserving its `.git` directory.
4. Delete the obsolete alpha 0.3 files listed below; their English replacements are included.
5. Review, commit and push the changes:

```sh
git status
git add .
git commit -m "Release Commander Table alpha 0.5"
git push
```

Obsolete files: `PUBLICAR-NO-GITHUB.md`, `server/LEIA-ME.md`, `server/VALIDACAO.md`, `server/iniciar.sh`, `server/iniciar-windows.bat`, `server/tunel.sh`, `server/tunel-windows.bat`. The replacements are `PUBLISHING.md`, `server/README.md`, `server/VALIDATION.md`, `server/start.sh`, `server/start-windows.bat`, `server/tunnel.sh` and `server/tunnel-windows.bat`.

If you do not have a clone yet, install Git, then run:

```sh
git clone https://github.com/dragonm4n/commander-table.git
cd commander-table
```

Then follow the update steps above. Complete GitHub authentication in the browser or Git Credential Manager when requested. Do not put account passwords or access tokens in project files.

## Create the alpha 0.5 release

After the source update is pushed:

1. Open **Releases** in the repository and start a new release.
2. Create tag **`v0.5.0-alpha.5`**, targeting the updated `main` branch.
3. Use title **Commander Table alpha 0.5**.
4. Paste the text from `RELEASE-NOTES.md`.
5. Attach **`Commander-Table-alpha-0.5.zip`**, the runnable package with its corresponding source.
6. Mark this experimental release as a **pre-release**, then publish when ready.

The GitHub source ZIP contains source files only; it does not replace the runnable ZIP. GitHub hosting does not run the Java server. Continue starting the server on the host computer and sharing table invitations through its tunnel.

The project ignores dependencies, builds, local profiles, logs and environment files. Keep the Forge revision pinned in `FORGE_REVISION`; changing it requires rebuilding and rerunning the native scenarios. Detailed build instructions are in `server/BUILD.md`.
