# MoreJcraft (by trandaj4437) — v4.15.0 (Forge 1.20.1)

All-in-one custom **Minecraft Forge 1.20.1** (`47.4.28`, Java 17, `mod_id: jcraftnodashbhop`) mod built via GitHub Actions (Gradle 8.8 + JDK 17), designed for **JCraft 0.18.2**, **TaCZ 1.2.0-hotfix**, **Shoulder Surfing Reloaded 5.3.1**, **Third Person Shooting: Zero 6.0.1**, **Custom Crosshair Mod 1.5.5**, and **Silver Chariot Requiem Addon 1.5.3** (+ AzureLib 3.2.9).

---

## Features

### 1. JCraft Ability Wheel & HUD Overhaul
- **Dynamic Ability Wheel**: Loads exact per-Stand & per-Spec ability icons from JCraft and addons (including `jcraft_scr`), dynamically expands active slices, and hides JCraft's default right-side icon HUD and cooldown text.
- **Controls**:
  - **Hold `X`**: Open transparent PvP Ability Wheel (**`Shift + Hold X`** keeps crouching while swapping slices that have Shift variants).
  - **Tap `X`**: Quick-cycle active skill.
  - **Press / Hold `C`**: Cast selected skill directly on the server (auto-summons Stand in 1 tick if unsummoned; supports holdable skills; prioritizes Spec skills when in Spec wheel).
  - **`STAND ⇄ SPEC` Toggle**: Only appears when the player owns both a Stand and a Spec (defaults to Stand wheel).
- **Mandom Fixes**: 2-half 180° wheel (`Countdown` & `Rewind`, with Rewind hub icon), bare-hand left-click M1 unlocked, and Barrage key triggers Rewind.
- **Visual Polish**: Inward-shrinking radial cooldown overlay synced with Cooldown Cancel, bound keybind labels under skill names, hover-only English + Vietnamese descriptions (`/jwheel reload`), thematic Stand colors with animated `§k` glitch characters for evolved Stands, and conditional `RequireEvo:` label.
- **Default JCraft Keybinds**: Applies custom default JCraft keybinds automatically on first launch and Reset.

### 2. JCraft NoDash (OP-Only, Server-Synced)
- Locks JCraft **Dash**, **Combo Breaker**, and **Cooldown Cancel** whenever a player has neither a Stand nor a Spec (automatically unlocks as soon as a Stand or Spec is obtained).

### 3. CS2 BHop & Velocity HUD (No `LivingEntityMixin`)
- Smooth CS2-style air-strafe BHop without modifying vanilla walk speed or using `LivingEntityMixin` (prevents JCraft clone crashes).
- Includes **Whitesnake Remote/Pilot Mode** fix (pauses BHop while piloting a Stand).
- Compact bottom-right **`u/s`** speedometer (off by default; any player can toggle with `/bhophud`).

### 4. Server-Synced Third-Person Camera (`/forcecam`)
- **`replace` (Default)**: Completely removes/replaces Minecraft's vanilla back third-person view (`isThirdPersonReplaced = true`, `isThirdPersonBackEnabled = false`) with **Shoulder Surfing Reloaded 5.3.1** over-the-shoulder view configured like gun aiming (`isCameraDecoupled = false`, `crosshairType = DYNAMIC`, `crosshairVisibility = ALWAYS`). Players can still press **F5** freely to switch between 1st-person and over-the-shoulder 3rd-person.
- **`lock` / `on`**: Locks **F5** and Shoulder Surfing toggle keys to keep all players in over-the-shoulder 3rd-person, while remaining fully compatible with **TaCZ 1.2.0-hotfix** ADS / sniper scopes.
- **`off`**: Disables all camera overrides.

### 5. Time Stop & King Crimson Time Erase Free Flight
- Hold **`Space`** during Time Stop or King Crimson's Time Erase Ultimate to fly slowly in 3D with a brief fall-damage grace period afterward.

### 6. Stand Skin Gacha & Custom Purple Haze Evolution
- **Skin Gacha**: Configurable chance (default **35%**) to roll an alternate Stand skin (`#1–#3`) on **Stand Arrow** use AND **all Stand evolutions** (`Requiem Arrow`, `Living Arrow`, `DIO's Diary`, `Diary Page`, `Green Baby`, and Purple Haze Flower evolution). Announces `Skin #N - <Skin Name>` on the hotbar actionbar with firework sounds.
- **Custom Purple Haze Evolution**: Defeat **1 Stand User** (counts both mobs with a Stand and players with a Stand) while owning Purple Haze, then right-click to gift/use **1 Flower** to evolve into **Purple Haze: Distortion** (`RequireEvo: Flower (1 Stand User Kill)`).

### 7. Built-in Stand Skin Pack & Clean Silver Chariot Requiem Integration
- **Built-in Stand Skins (Top-Priority Client Resource Pack, Auto-Enabled)**:
  - `Manga TW` → The World (`skin1`)
  - `The World Ultimate` → The World (`skin3`)
  - `Vinegar Crimson` → King Crimson (`skin1`)
  - `Garfield KQ / BTD` → Killer Queen (`skin1`) & Killer Queen: Bites the Dust (`skin1`)
- **Clean Silver Chariot Requiem Integration (Top-Priority Server Datapack, Auto-Enabled when `jcraft_scr` is present)**:
  - Bundles `jcraft-scr-forge-1.5.3.jar` automatically via Forge Jar-in-Jar (only requires `azurelib >= 3.1.10`, e.g. `azurelib-neo-1.20.1-3.2.9.jar`, in `mods/`).
  - Automatically disables Rome worldgen, boss spawns, extra chest loot, and Passione Computer crafting — keeping only **Silver Chariot Requiem** (all 4 skins, Stand Discs, and skills).
  - Changes Silver Chariot → Silver Chariot Requiem evolution to use vanilla JCraft's **`jcraft:requiem_arrow`** (`RequireEvo: Requiem Arrow`).

---

## Commands

| Command | Permission | Description |
| :--- | :---: | :--- |
| `/jnodash on \| off \| status` | OP | Toggle NoDash lock when player has no Stand & no Spec (server-synced) |
| `/cs2bhop on \| off \| autohop \| scrolljump \| preset mm\|community \| maxspeed <280-950>` | OP | Configure CS2 BHop physics server-wide |
| `/bhophud` | Everyone | Toggle personal bottom-right `u/s` speedometer (default: off) |
| `/forcecam replace \| lock \| on \| off \| status` | OP | Control server-wide third-person camera mode (default: `replace`) |
| `/jstandskin [0-100]` | OP | View or set Stand skin gacha chance % for arrows & evolutions (default: `35%`) |
| `/jwheel reload` | Client | Manually refresh Ability Wheel descriptions from `/stand about` |

---

## Building via GitHub Actions

1. Upload/push this repository to GitHub (includes `.github/workflows/build.yml`).
2. Open the **Actions** tab → **Build MoreJcraft (Forge 1.20.1)** runs automatically on push (JDK 17 + Gradle 8.8).
3. Download the built `.jar` from the **`MoreJcraft-v4.15.0-1.20.1`** artifact and place it in both client and server `mods/` folders.

---

## Credits

- Created by **trandaj4437** (`by trandaj4437`)
