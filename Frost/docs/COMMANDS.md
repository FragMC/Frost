# Commands

## Player Commands

- `/shop`
  - Opens the cosmetics shop GUI.
  - Permission: `frost.shop` (default: true)

- `/equip`
  - Opens your equip menu to toggle owned cosmetics.
  - Permission: `frost.equip` (default: true)

- `/togglelock`
  - Toggles Hotbar Lock on/off for your character and refreshes locked items when enabling.
  - Permission: none (player self‑service)

## Admin Command

- `/frost reload`
  - Reloads `config.yml`, profiles, and cosmetics from disk.
  - Permission: `frost.admin`

- `/frost setprofile <player> <profile>`
  - Sets a player’s active profile to one defined under `profiles` in `config.yml`.
  - Permission: `frost.admin`

- `/frost listprofiles`
  - Lists all available profile IDs and display names.
  - Permission: `frost.admin`

- `/frost givecosmetic <player> <cosmetic>`
  - Grants a cosmetic directly (bypasses economy), saving it to the player’s data.
  - Permission: `frost.admin`

## Permissions

- `frost.admin` – access to admin commands (default: op)
- `frost.shop` – can open the cosmetics shop (default: true)
- `frost.equip` – can open the equip menu (default: true)
- `frost.profile.<profile-name>` – gate access to specific profiles if you choose to enforce it

Defined in `plugin.yml`. See also [CONFIGURATION.md](CONFIGURATION.md) for how profiles and cosmetics are declared.
