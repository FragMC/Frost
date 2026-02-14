# Configuration

The plugin is configured via `plugins/Frost/config.yml`. Key sections and formats are below with inline examples taken from the default file.

## settings

```yaml
settings:
  instant-item-replace: true # if true, locked hotbar slots are refreshed frequently
  default-profile: "warrior" # profile used for new players
```

- `instant-item-replace` controls how quickly the Hotbar Lock refresh routine runs.
- `default-profile` is assigned when a new player first joins (and when loading defaults).

## profiles

Profiles define hotbar layouts. Each profile maps slot indices to items. Use MiniMessage for names/lore.

```yaml
profiles:
  warrior:
    display-name: "<red><bold>Warrior Profile</bold></red>"
    description: "Melee combat focused loadout"
    hotbar-items:
      0:
        material: DIAMOND_SWORD
        custom-name: "<aqua>Spear"
        lore:
          - "<aqua>Your spear for your journey"
        enchantments:
          sharpness: 5
          unbreaking: 3
        flags:
          - HIDE_ENCHANTS
          - HIDE_ATTRIBUTES
        custom-model-data: 1001
      1:
        material: BOW
        custom-name: "<gold>Longbow"
        enchantments:
          power: 4
          infinity: 1
        custom-model-data: 1002
      8:
        material: COOKED_BEEF
        custom-name: "<yellow>Rations"
```

Notes:

- Slots are 0–8 (hotbar). Any slot you define becomes locked when Hotbar Lock is enabled.
- Items support `material`, `custom-name`, `lore`, `enchantments`, `flags`, `custom-model-data`.
- Players change profiles only via `/frost setprofile` or other plugins through the API.

## cosmetics

Top‑level categories hold purchasable/equippable items. Built‑in categories include:

- `weapon-skins` – per‑slot weapon overrides (e.g., change the item’s model/lore).
- `armor-cosmetics` – whole sets or individual pieces.
- `particle-effects` – visual effects with triggers and types.

### Enchantment Key Format

Use the Minecraft namespaced key form for enchantments. The parser accepts:

- Hyphens: `fire-aspect`, `wind-burst`
- Underscores: `fire_aspect`, `wind_burst`
- Spaces: `fire aspect`, `wind burst`

All three map to the same enchantment internally.

### Admin-Only Cosmetics

You can mark any cosmetic as admin-only so it is hidden from the shop for non-admins and cannot be purchased by them. Admins automatically have access to all cosmetics and can equip them without purchase.

```yaml
cosmetics:
  weapon-skins:
    items:
      dev-spear:
        admin-only: true
        name: "<gold><bold>Developer Spear</bold>"
        price: 0.0
        icon: STICK
        applies-to:
          profile: "warrior"
          slot: 0
        modifications:
          custom-model-data: 9001
          lore:
            - "<gold>Internal use only"
```

Example: weapon skins

```yaml
cosmetics:
  weapon-skins:
    category-name: "<red>Weapon Skins"
    category-icon: DIAMOND_SWORD
    category-description: "Change the appearance of your weapons"
    items:
      golden-spear:
        name: "<gold><bold>Golden Spear</bold>"
        description:
          - "<gold>A luxurious golden finish"
        price: 1000.0
        icon: GOLD_INGOT
        applies-to:
          profile: "warrior"
          slot: 0
        modifications:
          custom-model-data: 2001
          lore:
            - "<gold>Shiny and golden!"
```

Example: particle effect

```yaml
cosmetics:
  particle-effects:
    category-name: "<light_purple>Particle Effects"
    category-icon: DRAGON_BREATH
    category-description: "Visual particle effects triggered by events"
    items:
      flame-trail:
        name: "<red><bold>Flame Trail</bold>"
        price: 2500.0
        icon: FIRE_CHARGE
        particle: FLAME
        effect-type: TRAIL # TRAIL | SURROUND | BURST
        trigger: ALWAYS # ALWAYS | JUMP | SPRINT | SNEAK | GLIDE | SWIM | RIPTIDE | MACE_SMASH | DAMAGE_DEALT | DAMAGE_TAKEN | KILL | BLOCK_BREAK | BLOCK_PLACE
        count: 3
        offset-x: 0.2
        offset-y: 0.1
        offset-z: 0.2
        speed: 0.01
```

Notes:

- Particle availability depends on your server’s Minecraft version; unsupported effects are skipped.
- Equipped cosmetics persist and can be toggled in the `/equip` menu.

## shop

```yaml
shop:
  title: "<gradient:aqua:blue>Cosmetics Shop</gradient>"
  rows: 6 # 1–6
```

Controls the Java inventory GUI layout. Bedrock players see Floodgate forms.

## permissions

Defined in `plugin.yml`:

- `frost.admin` – admin commands.
- `frost.shop` – open the shop.
- `frost.equip` – open the equip menu.
- `frost.profile.<profile-name>` – optional profile gating.

## Reloading

- Edit `config.yml`, then run `/frost reload` to re‑load settings, profiles, and cosmetics.
