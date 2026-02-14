# Frost

Paper plugin for Minecraft 1.21.x that gives your server a profile‑based hotbar system, a cosmetics shop and equip menu, particle effects, and tight Bedrock (Floodgate) support. Player data persists to SQLite and integrates with Vault for economy.

## Features

- Profile hotbars: define per‑profile items in `config.yml`; enforce via Hotbar Lock.
- Cosmetics shop: category‑driven GUI with prices through Vault.
- Equip menu: equip/unequip owned cosmetics, including particles.
- Particle effects: always‑on, trail, surround, and burst triggers for common events.
- Bedrock UI: Floodgate + Cumulus forms for shop/equip on Bedrock players.
- SQLite storage: players’ profile, lock, owned/equipped cosmetics saved server‑side.

## Requirements

- Paper 1.21.1+
- Java 21
- Vault (economy provider)
- Optional: Geyser‑Spigot and Floodgate for Bedrock support

## Installation

1. Build or download the plugin jar (see Build).
2. Copy the jar into your server’s `plugins/` directory.
3. Start the server to generate `plugins/Frost/config.yml` and the SQLite database.
4. Edit `config.yml` to suit your server.
5. Run `/frost reload` to apply changes.

## Usage

- `/shop` – open the cosmetics shop GUI.
- `/equip` – open your equip menu to toggle owned cosmetics.
- `/togglelock` – enable/disable Hotbar Lock for your character.
- `/frost <reload|setprofile|listprofiles|givecosmetic>` – admin tools.

See more in [docs/COMMANDS.md](docs/COMMANDS.md) and [docs/CONFIGURATION.md](docs/CONFIGURATION.md).

## Configuration

Everything is driven by `config.yml`:

- `settings` – defaults and behavior (e.g., `instant-item-replace`, `default-profile`).
- `profiles` – named hotbar layouts mapped to slot numbers and items.
- `cosmetics` – categories like weapon skins, armor sets, and particle effects.
- `shop` – GUI title and rows.

Details and examples: [docs/CONFIGURATION.md](docs/CONFIGURATION.md).

## Build

This project uses Maven with shading.

```bash
mvn -v        # verify Maven
mvn package   # build shaded jar
```

Output jar is written to `target/` (e.g., `frost-1.0.0.jar`). Drop it into your server’s `plugins/`.

Dependencies are marked `provided` for server‑side APIs (Paper, Vault, Geyser/Floodgate).

## API

Other plugins can interact with Frost via a small public API.

- Set a player’s profile
- Query available profiles
- Check/give/remove owned cosmetics

See `com.stufy.fragmc.frost.api.FrostAPI` in source for method list.

## Commands & Permissions

- Commands: `/shop`, `/equip`, `/togglelock`, `/frost …`
- Core permissions: `frost.admin`, `frost.shop`, `frost.equip`, `frost.profile.<name>`

Reference: [docs/COMMANDS.md](docs/COMMANDS.md).

## Support

- Paper 1.21.x, Java 21
- Economy via Vault
- Optional Bedrock support via Floodgate

