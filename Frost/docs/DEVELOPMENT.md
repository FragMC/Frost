# Development

Notes for building and working on Frost locally.

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Paper 1.21.1+ test server

## Build

```bash
mvn clean package
```

Artifacts are written to `target/` (e.g., `frost-1.0.0.jar`). The shade plugin relocates Gson into a private package to avoid conflicts.

## Install to Server

1. Copy the built jar into your server’s `plugins/` directory.
2. Start the server to generate `plugins/Frost/config.yml` and `plugins/Frost/database.db`.
3. Edit `config.yml`, then run `/frost reload` to apply changes.

## Project Structure

- Core plugin entry: `com.stufy.fragmc.frost.Frost`.
- Public API: `com.stufy.fragmc.frost.api.FrostAPI` (exposed via `Frost#getAPI()`).
- Managers: configuration, profiles, cosmetics, particles, GUI, database, player data.
- Resources: `plugin.yml`, `config.yml`.

## Testing Tips

- Use a fresh test world and a clean `plugins/Frost/` folder for baseline behavior.
- Exercise particle triggers: jump, sprint, sneak, glide, swim, damage, kill, block break/place.
- Verify Hotbar Lock: enable with `/togglelock`, ensure locked slots resist edits/drops.
- Check Bedrock UIs with Floodgate if available; otherwise Java inventory UIs are used.

## Coding Notes

- Target API: Paper 1.21.1; compile level: Java 21.
- Avoid logging secrets or writing sensitive data to disk.
- Keep `provided` scope for server APIs; shade only what you must relocate.

