# MKT Essentials

A powerful and lightweight Essentials mod for **NeoForge 1.21.1**, designed with stability and compatibility in mind. Teleportation, chat, kits, moderation, an authentication system with an embedded Discord bot, inventory backups, item cleanup and much more — all configurable via YAML.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge 21.1.228+
- **Java:** 21
- **Mod ID:** `mktessentials`
- **Current version:** 0.4.0

---

## 📑 Table of Contents

1. [Installation](#-installation)
2. [Dependencies & Compatibility](#-dependencies--compatibility)
3. [First-Run & Config Layout](#-first-run--config-layout)
4. [Tutorial: Authentication System](#-tutorial-authentication-system)
5. [Tutorial: Discord Bot](#-tutorial-discord-bot)
6. [Tutorial: Permissions (LuckPerms)](#-tutorial-permissions-luckperms)
7. [Tutorial: Placeholders & TAB](#-tutorial-placeholders--tab)
8. [Tutorial: Website / Backend Sync](#-tutorial-website--backend-sync)
9. [Features](#-features)
10. [Command Reference](#-command-reference)
11. [Configuration Reference](#-configuration-reference)
12. [Building from Source](#-building-from-source)
13. [Troubleshooting](#-troubleshooting)
14. [License](#-license)

---

## 📥 Installation

1. Install **NeoForge 1.21.1** (version 21.1.228 or newer) on your server.
2. Download `mktessentials-0.4.0.jar`.
3. Drop the JAR into your server's `mods/` folder.
4. (Optional) Install the integration mods below into the same `mods/` folder.
5. Start the server once to generate the config files, then stop it, edit the configs, and start again.

All bundled libraries (JDA, SQLite, jBCrypt, Jackson, etc.) are shipped **inside** the mod JAR — you do not need to install them separately.

---

## 🔌 Dependencies & Compatibility

The mod runs standalone. Every integration is **optional** and auto-detected at startup.

| Mod | What it unlocks | Required? |
|-----|-----------------|-----------|
| [LuckPerms (patched)](https://github.com/onmydestiny/LuckPerms-PATCHED) | Permissions, prefixes/suffixes, per-group chat | Recommended |
| [Text Placeholder API (NeoForge port)](https://github.com/MakotoPD/TextPlaceholderAPI-NeoForge) | `%mktessentials:...%` placeholders in chat/tab | Optional |
| TAB | Tab-list placeholders | Optional |
| Curios API | `/invsee` shows Curios slots | Optional |

> ⚠️ **Important compatibility notes**
>
> - **LuckPerms** — The official NeoForge 1.21.1 build has a known bug. Use the **patched version** linked above.
> - **Text Placeholder API** — No official NeoForge 1.21.1 release exists. Use the **NeoForge port** linked above.

Without LuckPerms the mod falls back to vanilla OP levels for permission checks.

---

## 🗂️ First-Run & Config Layout

On first launch the mod creates:

```
config/mktessentials/
├── settings.yml      — All settings (teleport, RTP, AFK, backups, auth, discord, items, warns)
├── commands.yml      — Enable/disable individual commands
├── messages.yml      — Chat format, join/quit messages, broadcasts, text commands
├── accounts.db       — SQLite database (auth system — created when auth is enabled)
└── lang/
    ├── en_us.yml     — English messages
    └── pl_pl.yml     — Polish messages
```

Per-world data (player homes, warps, kits, bans, IP bans, punishment history, spawn point) lives in `<world>/mktessentials/`.

After editing configs, apply changes with `/mkt reload` (most settings) or restart the server (auth/Discord/web changes require a restart).

To switch language, set `language: "pl_pl"` (or `en_us`) at the top of `settings.yml`.

---

## 🔐 Tutorial: Authentication System

The auth system forces players to register/login and/or link a Discord account before they can play. Unauthenticated players are **frozen** (can't move, interact or chat).

### Step 1 — Pick a mode

In `settings.yml`:

```yaml
auth:
  mode: "disabled"   # change this
```

| Mode | Requires password | Requires Discord link | Use case |
|------|:-:|:-:|----------|
| `disabled` | – | – | Auth turned off (default) |
| `optional` | no | no | Players may register but aren't forced to |
| `auth-only` | ✅ | – | Classic password login |
| `link-only` | – | ✅ | Discord link only, no passwords |
| `full` | ✅ | ✅ | Link Discord **and** set a password |

### Step 2 — Tune the behaviour

```yaml
auth:
  mode: "full"
  session-timeout-hours: 24      # auto-login from the same IP within this window
  max-login-attempts: 5          # failed logins before a kick
  login-timeout-seconds: 60      # kick if not authenticated in time
  newbie-protection-minutes: 30  # invulnerability for brand-new players (0 = off)
```

### Step 3 — Player flow

- `/register <password> <password>` then `/login <password>`
- In `full` / `link-only` mode players must link Discord first — see the next tutorial.

### Admin commands

| Command | Effect |
|---------|--------|
| `/auth reset <player>` | Wipe the account (password + link) |
| `/auth unlink <player>` | Force-unlink the player's Discord |
| `/auth info <player>` | Show UUID, Discord, registration & login info |

> Passwords are hashed with **bcrypt** and stored in `accounts.db` — never in plaintext.

---

## 🤖 Tutorial: Discord Bot

The mod embeds a **JDA** Discord bot that handles account linking via a slash command and can assign a "linked" role.

### Step 1 — Create the application & bot

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications) → **New Application**.
2. Open the **Bot** tab → **Reset Token** → copy the token (you'll paste it into the config).
3. Still on the **Bot** tab, scroll to **Privileged Gateway Intents** and enable:
   - ✅ **Server Members Intent**

   > 🚨 This step is **mandatory**. The bot requests the `GUILD_MEMBERS` intent to manage the linked role. If it is not enabled, the bot connects and is immediately disconnected with `CloseCode 4014 (DISALLOWED_INTENTS)` and you'll see `Failed to start Discord bot` in the console.

4. Open **OAuth2 → URL Generator**, tick `bot` and `applications.commands`, give it at least **Manage Roles**, then open the generated URL to invite the bot to your server.
5. Make sure the bot's role is **above** the role it should assign in your server's role list (otherwise Discord forbids the assignment).

### Step 2 — Get the IDs

Enable **Developer Mode** in Discord (User Settings → Advanced), then right-click to **Copy ID** for:
- your **server** (guild ID)
- the **role** you want linked players to receive (optional)

### Step 3 — Configure the mod

In `settings.yml`:

```yaml
discord:
  enabled: true
  bot-token: "YOUR_BOT_TOKEN"
  guild-id: "YOUR_GUILD_ID"
  link-command-name: "link"     # slash command players use, e.g. /link
  linked-role-id: "ROLE_ID"     # leave "" to disable role assignment
  show-player-count: true       # show online player count as bot status
```

Auth must also be enabled (`auth.mode` set to `full` or `link-only`) for linking to make sense.

### Step 4 — Test the flow

1. Restart the server. The console should print `Discord bot connected successfully.`
2. In-game, run `/link` → you receive a 6-digit code (valid 5 minutes).
3. On Discord, run the slash command (`/link <code>`).
4. The bot links the account, assigns the role, and the player is let through.

`/unlink` (in-game) or `/auth unlink <player>` (admin) reverses it.

---

## 🛡️ Tutorial: Permissions (LuckPerms)

1. Install the [patched LuckPerms](https://github.com/onmydestiny/LuckPerms-PATCHED) into `mods/`.
2. Grant permission nodes the usual way, e.g.:

   ```
   /lp group default permission set mktessentials.command.home true
   /lp group vip permission set mktessentials.command.rtp true
   /lp group admin permission set mktessentials.admin.* true
   ```

3. Use `/mkt permissions` in-game to print every node the mod registers.

Prefixes/suffixes and primary groups set in LuckPerms are used by the chat format and TAB placeholders automatically. Without LuckPerms, the mod uses vanilla OP levels.

### Per-rank limits & cooldowns

Beyond simple on/off command nodes, a few limits scale per rank:

| Node | Type | Effect |
|------|------|--------|
| `mktessentials.homes.<number>` | permission | Maximum homes for the rank. The **highest granted number wins**; `mktessentials.homes.*` or `mktessentials.homes.unlimited` grants unlimited homes. |
| `mktessentials.max_homes` | meta | Alternative home limit (used when no `mktessentials.homes.<n>` permission is granted). Config `general.max-homes` is the final fallback. |
| `mktessentials.teleport_cooldown.tpa` / `.rtp` / `.warp` | meta | Per-type teleport cooldown in seconds. |
| `mktessentials.teleport_cooldown` | meta | Cooldown applied to **all** teleport types (legacy/global override). |
| `mktessentials.teleport_delay` | meta | Warmup delay (seconds) before a teleport executes. |
| `mktessentials.teleport.bypass` | permission | Skip teleport delay and all cooldowns. |

```
# Default players: 3 homes, 60s RTP cooldown (from config)
# VIP: 10 homes + faster RTP
/lp group vip permission set mktessentials.homes.10
/lp group vip meta set mktessentials.teleport_cooldown.rtp 15

# MVP: 25 homes, Admin: unlimited
/lp group mvp permission set mktessentials.homes.25
/lp group admin permission set mktessentials.homes.unlimited
```

> Each teleport type tracks its own cooldown clock, so using `/tpa` does not start the `/rtp` or `/warp` cooldown.

---

## 🏷️ Tutorial: Placeholders & TAB

### Text Placeholder API

Install the [NeoForge port](https://github.com/MakotoPD/TextPlaceholderAPI-NeoForge). The mod then exposes placeholders such as:

```
%mktessentials:name%        %mktessentials:nick%        %mktessentials:real_name%
%mktessentials:prefix%      %mktessentials:suffix%      %mktessentials:full_name%
%mktessentials:tab_full_name%
```

Use them in `messages.yml` chat/join/quit formats.

### TAB

If [TAB](https://github.com/NEZNAMY/TAB) is installed, reference the placeholders above directly in TAB's `config.yml`. The mod registers them on server start (`TAB — Tab list placeholders active` appears in the log).

### Chat format example (`messages.yml`)

```yaml
chat:
  format: "%mktessentials:dot%%mktessentials:prefix%%mktessentials:name%%mktessentials:suffix%&8: &f{message}"
  group-formats:
    admin: "&c[Admin] &f%mktessentials:name%&8: &f{message}"
    vip:   "&6[VIP] &f%mktessentials:name%&8: &f{message}"
```

---

## 🌐 Tutorial: Website / Backend Sync

MKT Essentials can integrate with an external backend (e.g. a website panel sharing the same player database). There are **two independent channels**.

### Channel 1 — Outbound (mod → backend)

Whenever a player links or unlinks, the mod sends a `POST` to your backend so it stays in sync. The mod's SQLite is always the **source of truth** for auth; HTTP failures never affect the local state.

```yaml
auth:
  web-sync:
    enabled: true
    url: "https://example.com/api/mc/link"   # receives the POST
    secret: "A_LONG_RANDOM_SHARED_SECRET"    # sent as: Authorization: Bearer <secret>
    backfill-on-start: true                   # on boot, push every linked account (idempotent)
```

Payload sent to your endpoint:

```json
{
  "action": "link",              // or "unlink"
  "minecraftUuid": "…",
  "minecraftUsername": "…",
  "discordId": "…",
  "discordUsername": "…",
  "discordAvatar": "…"
}
```

### Channel 2 — Inbound (backend → mod)

This is the reverse direction: it lets the backend tell the mod to **unlink an account in-game** — for example a "Disconnect" button in a web admin panel. Without it, clearing the account only on the backend leaves it linked on the server, and the next `backfill-on-start` would overwrite the change.

```yaml
auth:
  web-sync:
    api:
      enabled: true
      bind: "127.0.0.1"   # keep localhost if the backend runs on the same machine/VPS
      port: 8766
      secret: ""           # leave empty to reuse web-sync.secret above
```

> 🔒 **Security:** keep `bind: "127.0.0.1"` unless the backend lives on another host. If you must expose it, put it behind a firewall/reverse proxy and use a strong, unique `secret`. The endpoint authenticates with `Authorization: Bearer <secret>`.

**Endpoint:**

```
POST http://<bind>:<port>/unlink
Authorization: Bearer <secret>
Content-Type: application/json

{ "discordId": "123456789012345678" }
```

You may send `{"minecraftUuid": "…"}` instead of `discordId` (with or without dashes).

**Responses:**

| Status | Meaning |
|--------|---------|
| `200 {"ok":true}` | Account unlinked in-game (role removed, session cleared, player re-frozen if online) |
| `401` | Missing/invalid bearer secret |
| `400` | Neither `discordId` nor `minecraftUuid` provided |
| `404 {"error":"account_not_found"}` | No matching linked account in the mod's database |

When both channels are enabled, an inbound unlink also fires an outbound `unlink` echo, keeping the backend consistent even if the unlink originated in-game.

#### Example: Nuxt/Node backend calling the mod

```ts
await fetch(`${process.env.MC_MOD_API_URL}/unlink`, {   // e.g. http://127.0.0.1:8766
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${process.env.MC_LINK_SECRET}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ discordId })
})
```

---

## 🚀 Features

### 🏠 Teleportation
- **Homes** — `/sethome`, `/home`, `/delhome`, `/listhomes` with **per-rank home limits** (dynamic LuckPerms permission `mktessentials.homes.<number>`)
- **Warps** — `/setwarp`, `/warp`, `/delwarp`, `/warps` (clickable GUI), `/listwarps`
- **TPA** — `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel` (multiple pending requests)
- **TP toggle** — `/tptoggle` block incoming teleport requests
- **Spawn** — `/spawn`, `/setspawn` custom server spawn point
- **RTP** — `/rtp` async random teleport with biome blacklist
- **Back** — `/back` return to previous location (works after death too)
- **Top** — `/top` teleport to highest block
- **TpAll** — `/tpall` teleport all players to you
- **TP shortcuts** — `/tp <player>`, `/tphere <player>`, `/tppos <x> <y> <z>`
- **Per-type cooldowns** — Independent cooldowns for TPA, RTP and Warps; set globally in config or per rank via LuckPerms meta (`/tpa` no longer blocks `/rtp`)

### 💬 Chat & Identity
- **Nicknames** — `/nick` custom display names
- **Chat Format** — Per-group chat formats via LuckPerms integration
- **Hover Info** — Hover over player names to see rank, ping, UUID
- **Private messages** — `/msg`, `/reply`, `/msgtoggle` block PMs, `/ignore <player>` hide a player's chat and PMs
- **Staff contact** — `/helpop <message>`, `/report <player> <reason>` (sent to online staff, with anti-spam cooldown)
- **Social Spy** — `/socialspy` see private messages (staff)
- **Recording/Streaming** — `/recording`, `/streaming` status indicators
- **AFK** — Automatic [AFK] prefix after inactivity + manual `/afk` toggle

### 📦 Kits
- **Create** — `/createkit <name> <cooldown>` opens a chest GUI to compose the kit (or `frominv` to snapshot your inventory)
- **Claim** — `/kit <name>` with cooldown tracking, or click in the `/kits` GUI
- **Browse** — `/kits` clickable GUI with icons, cooldowns and ready status
- **Delete** — `/deletekit <name>`

### 🛡️ Admin
- **Heal/Feed** — `/heal`, `/feed`
- **Fly/God** — `/fly`, `/god` (persists across reconnects)
- **Vanish** — `/vanish` (persists, fake join/quit messages)
- **Speed** — `/speed fly|walk <0-10>`
- **Gamemode** — `/gmc`, `/gms`, `/gma`, `/gmsp`, `/gm <0-3> [player]`
- **Inventory** — `/invsee <player>`, `/enderchest <player>` (supports offline players + Curios)
- **Items** — `/i <item> [amount]`, `/more`, `/skull <player>`
- **Clear** — `/clearinv`, `/clearitems [radius]`
- **Repair** — `/repair` item in hand
- **Enchant** — `/enchant <enchantment> <level>`
- **Experience** — `/exp give|set <player> <levels>`
- **Player info** — `/whois <player>` (IP, ping, location, playtime, statuses; works offline)
- **Sudo** — `/sudo <player> <command>`

### 🔨 Moderation
- **Kick** — `/kick <player> [reason]`
- **Ban** — `/ban <player> [reason]`, `/tempban <player> <duration> [reason]`, `/unban <player>`
- **IP Ban** — `/banip <player|ip> [reason]`, `/unbanip <ip>` (works with offline players via last known IP)
- **Mute** — `/mute <player> [duration]`, `/tempmute <player> <duration> [reason]`, `/unmute <player>` (supports offline players)
- **Warns** — `/warn <player> [reason]`, `/unwarn`, `/warns <player>` with automatic tempban escalation after a configurable warn limit
- **History** — `/history <player>` full punishment record (warns, bans, mutes, kicks)
- **Shadowban** — `/shadowban <player> [reason]`, `/unshadowban <player>`, `/shadowbanlist`
  - Methods: timeout, full, internal-error, phantom (configurable)
- **Ban Screen** — Banned players see reason + remaining time on connect

### 🔐 Authentication & Discord Link
- **Auth modes** — full, auth-only, link-only, optional, disabled
- **Register/Login** — `/register <password> <password>`, `/login <password>`
- **Discord Link** — `/link` generates 6-digit code, use on Discord to link accounts
- **Embedded Discord Bot** — JDA-based, registers slash commands, assigns roles
- **Freeze System** — Unauthenticated players can't move/interact/chat
- **Newbie Protection** — Configurable invulnerability for first-time players
- **Session Management** — Auto-login from same IP within timeout
- **Web sync** — Optional two-way sync with an external backend / website panel
- **Admin** — `/auth reset|unlink|info <player>`

### 💾 Inventory Backups
- **Auto-backup** — On death, join, quit (configurable)
- **Manual** — `/invbackup save <player> [note]`
- **Browse** — `/invbackup list <player>` opens GUI with backup history
- **Restore** — Click a backup in GUI or `/invbackup restore <player> <file>`
- **Delete** — `/invbackup delete <player> <file>`
- **Scheduled** — Optional periodic backups for all online players

### 🧹 Item Management
- **Auto-despawn** — Configurable timer per item (default 5 min)
- **Item Stacking** — Merges identical nearby items to reduce entity count
- **Holograms** — Floating name + countdown above ground items
- **Whitelist** — Items that never despawn (supports modded: `create:brass_ingot`, wildcards: `minecraft:netherite_*`)
- **Global Sweep** — Optional periodic cleanup with broadcast warning
- **Manual** — `/clearitems [radius]`

### 📢 Broadcasts
- **Automated** — Configurable interval, random or sequential order, pauses on an empty server
- **Manual** — `/broadcast <message>`
- **Custom prefix** — Per-broadcast formatting

### 📜 Text Commands
- **Config-defined info commands** — define your own commands in `messages.yml` with custom aliases and message lines
- **Defaults** — `/rules` (alias `/zasady`), `/www`, `/vote`, `/discordinvite` (alias `/dc`)

### 🛠️ Virtual Workstations
- `/workbench` (alias `/craft`), `/anvil`, `/grindstone`, `/stonecutter`, `/smithing` — open vanilla menus anywhere
- `/trash` — disposal chest, items are destroyed on close

### ⏰ Time & Weather
- **Server** — `/day`, `/noon`, `/night`, `/midnight`, `/sun`, `/rain`, `/storm`
- **Per-player (client-side)** — `/ptime day|noon|night|midnight|<ticks>|reset`, `/pweather clear|rain|reset`

### 🔍 Utility
- `/near [radius]` — List nearby players
- `/seen <player>` — When player was last online
- `/playtime [player]` — Total play time
- `/ping` — Show your latency
- `/tps` — Server TPS/MSPT, `/lag` — detailed performance info (staff)
- `/hat` — Put item on head

## 📋 Command Reference

| Command | Description | Permission |
|---------|-------------|------------|
| `/home <name>` | Teleport to a saved home | `mktessentials.command.home` |
| `/sethome <name>` | Save current location as home | `mktessentials.command.sethome` |
| `/delhome <name>` | Delete a saved home | `mktessentials.command.delhome` |
| `/listhomes` | List all your homes | `mktessentials.command.listhomes` |
| `/warp <name>` | Teleport to a warp point | `mktessentials.command.warp` |
| `/setwarp <name>` | Create a warp point | `mktessentials.admin.setwarp` |
| `/delwarp <name>` | Delete a warp point | `mktessentials.admin.delwarp` |
| `/warps` | Browse warps in a clickable GUI | `mktessentials.command.listwarps` |
| `/listwarps` | List all warp points (text) | `mktessentials.command.listwarps` |
| `/spawn` | Teleport to spawn | `mktessentials.command.spawn` |
| `/setspawn` | Set the server spawn point | `mktessentials.admin.setspawn` |
| `/back` | Return to previous location (incl. death) | `mktessentials.command.back` |
| `/top` | Teleport to highest block above you | `mktessentials.command.top` |
| `/rtp` | Random teleport to a safe location | `mktessentials.command.rtp` |
| `/tpa <player>` | Request teleport to a player | `mktessentials.command.tpa` |
| `/tpahere <player>` | Request a player teleports to you | `mktessentials.command.tpahere` |
| `/tpaccept [player]` | Accept a teleport request | `mktessentials.command.tpa` |
| `/tpdeny [player]` | Deny a teleport request | `mktessentials.command.tpa` |
| `/tpacancel` | Cancel your outgoing teleport requests | `mktessentials.command.tpa` |
| `/tptoggle` | Block incoming teleport requests | `mktessentials.command.tptoggle` |
| `/tp <player>` | Instant teleport to player (admin) | `mktessentials.admin.tp` |
| `/tphere <player>` | Teleport player to you (admin) | `mktessentials.admin.tp` |
| `/tppos <x> <y> <z>` | Teleport to coordinates | `mktessentials.admin.tp` |
| `/tpall` | Teleport all players to you | `mktessentials.admin.tpall` |
| `/msg <player> <message>` | Send private message | `mktessentials.command.msg` |
| `/reply <message>` | Reply to last private message | `mktessentials.command.msg` |
| `/msgtoggle` | Block private messages | `mktessentials.command.msgtoggle` |
| `/ignore <player>` | Ignore a player's chat and PMs | `mktessentials.command.ignore` |
| `/socialspy` | See private messages (staff) | `mktessentials.admin.socialspy` |
| `/broadcast <message>` | Broadcast a message to everyone | `mktessentials.admin.broadcast` |
| `/helpop <message>` | Send a message to online staff | `mktessentials.command.helpop` |
| `/report <player> <reason>` | Report a player to online staff | `mktessentials.command.report` |
| `/afk` | Toggle AFK status | `mktessentials.command.afk` |
| `/nick <nickname>` | Set your display name | `mktessentials.command.nick` |
| `/recording` | Toggle recording status indicator | `mktessentials.command.recording` |
| `/streaming` | Toggle streaming status indicator | `mktessentials.command.streaming` |
| `/kit <name>` | Claim a kit | `mktessentials.kit.<name>` |
| `/kits` | Browse kits in a clickable GUI | All |
| `/createkit <name> <cooldown>` | Create kit via chest GUI (`frominv` = from inventory) | `mktessentials.admin.kits` |
| `/deletekit <name>` | Delete a kit | `mktessentials.admin.kits` |
| `/heal [player]` | Restore full health | `mktessentials.admin.heal` |
| `/feed [player]` | Restore full hunger | `mktessentials.admin.feed` |
| `/fly [player]` | Toggle flight mode (persists) | `mktessentials.admin.fly` |
| `/god [player]` | Toggle invulnerability (persists) | `mktessentials.admin.god` |
| `/vanish` | Toggle invisibility (persists) | `mktessentials.admin.vanish` |
| `/speed fly\|walk <0-10> [player]` | Set movement speed | `mktessentials.admin.speed` |
| `/gmc [player]` | Set gamemode creative | `mktessentials.admin.gamemode` |
| `/gms [player]` | Set gamemode survival | `mktessentials.admin.gamemode` |
| `/gma [player]` | Set gamemode adventure | `mktessentials.admin.gamemode` |
| `/gmsp [player]` | Set gamemode spectator | `mktessentials.admin.gamemode` |
| `/gm <0-3> [player]` | Set gamemode by number | `mktessentials.admin.gamemode` |
| `/invsee <player>` | View/edit player inventory (online + offline) | `mktessentials.admin.invsee` |
| `/enderchest <player>` | View/edit player ender chest (online + offline) | `mktessentials.admin.enderchest` |
| `/clearinv [player]` | Clear player inventory | `mktessentials.admin.clearinv` |
| `/i <item> [amount]` | Give item to yourself | `mktessentials.admin.give` |
| `/more` | Set held item stack to max | `mktessentials.admin.more` |
| `/skull <player>` | Get a player head | `mktessentials.admin.skull` |
| `/repair` | Repair held item | `mktessentials.utility.repair` |
| `/enchant <enchantment> <level>` | Enchant held item | `mktessentials.utility.enchant` |
| `/exp give\|set <player> <levels>` | Manage experience levels | `mktessentials.admin.exp` |
| `/whois <player>` | Detailed player info (works offline) | `mktessentials.admin.whois` |
| `/sudo <player> <command>` | Force player to run command | `mktessentials.admin.sudo` |
| `/trash` | Open a disposal chest | `mktessentials.command.trash` |
| `/workbench` (`/craft`) | Open a crafting table | `mktessentials.command.workbench` |
| `/anvil` | Open an anvil | `mktessentials.command.anvil` |
| `/grindstone` | Open a grindstone | `mktessentials.command.grindstone` |
| `/stonecutter` | Open a stonecutter | `mktessentials.command.stonecutter` |
| `/smithing` | Open a smithing table | `mktessentials.command.smithing` |
| `/clearitems [radius]` | Remove ground items | `mktessentials.admin.clearitems` |
| `/invbackup save <player> [note]` | Create inventory backup | `mktessentials.admin.backup` |
| `/invbackup list <player>` | Browse backups in GUI | `mktessentials.admin.backup` |
| `/invbackup restore <player> <file>` | Restore a backup | `mktessentials.admin.backup` |
| `/invbackup delete <player> <file>` | Delete a backup | `mktessentials.admin.backup` |
| `/kick <player> [reason]` | Kick player from server | `mktessentials.moderation.kick` |
| `/ban <player> [reason]` | Permanently ban player | `mktessentials.moderation.ban` |
| `/tempban <player> <duration> [reason]` | Temporarily ban player | `mktessentials.moderation.tempban` |
| `/unban <player>` | Remove ban | `mktessentials.moderation.unban` |
| `/banip <player\|ip> [reason]` | Ban an IP address | `mktessentials.moderation.banip` |
| `/unbanip <ip>` | Remove an IP ban | `mktessentials.moderation.banip` |
| `/warn <player> [reason]` | Warn a player (auto-tempban at limit) | `mktessentials.moderation.warn` |
| `/unwarn <player>` | Revoke the latest warn | `mktessentials.moderation.warn` |
| `/warns <player>` | List a player's active warns | `mktessentials.moderation.warn` |
| `/history <player>` | Full punishment history | `mktessentials.moderation.history` |
| `/mute <player> [duration]` | Mute player | `mktessentials.admin.mute` |
| `/tempmute <player> <duration> [reason]` | Timed mute with reason | `mktessentials.admin.mute` |
| `/unmute <player>` | Unmute player | `mktessentials.admin.unmute` |
| `/shadowban <player> [reason]` | Shadowban player | `mktessentials.moderation.shadowban` |
| `/unshadowban <player>` | Remove shadowban | `mktessentials.moderation.shadowban` |
| `/shadowbanlist` | List shadowbanned players | `mktessentials.moderation.shadowban` |
| `/register <password> <password>` | Register account | All |
| `/login <password>` | Login to account | All |
| `/changepassword <old> <new> <new>` | Change password | All |
| `/link` | Generate Discord link code | All |
| `/unlink` | Unlink Discord account | All |
| `/discord` | Show linked Discord info | All |
| `/auth reset <player>` | Reset player account | `mktessentials.auth.admin.reset` |
| `/auth unlink <player>` | Force unlink Discord | `mktessentials.auth.admin.unlink` |
| `/auth info <player>` | Show player auth info | `mktessentials.auth.admin.info` |
| `/near [radius]` | List nearby players (default 200) | `mktessentials.command.near` |
| `/seen <player>` | Check when player was last online | `mktessentials.command.seen` |
| `/playtime [player]` | Show total play time | `mktessentials.command.playtime` |
| `/ping` | Show your latency in ms | `mktessentials.command.ping` |
| `/tps` | Show server TPS and MSPT | `mktessentials.command.tps` |
| `/lag` | Detailed performance info | `mktessentials.admin.lag` |
| `/hat` | Put held item on your head | `mktessentials.command.hat` |
| `/day`, `/noon`, `/night`, `/midnight` | Set world time | `mktessentials.admin.time` |
| `/sun`, `/rain`, `/storm` | Set weather | `mktessentials.admin.weather` |
| `/ptime <preset\|ticks\|reset>` | Personal client-side time | `mktessentials.command.ptime` |
| `/pweather clear\|rain\|reset` | Personal client-side weather | `mktessentials.command.pweather` |
| `/rules`, `/www`, `/vote`, ... | Config-defined text commands | `mktessentials.command.text.<name>` |
| `/mkt help` | Show command help | All |
| `/mkt reload` | Reload configuration | `mktessentials.admin.reload` |
| `/mkt permissions` | List all permission nodes | `mktessentials.admin.permissions` |

## ⚙️ Configuration Reference

### Command Toggle (`commands.yml`)
```yaml
admin:
  fly: true
  god: true
  vanish: false  # disabled
```

### Internationalization (`settings.yml`)
```yaml
language: "pl_pl"
```

### Per-Group Chat Format (`messages.yml`)
```yaml
chat:
  group-formats:
    admin: "&c[Admin] &f%mktessentials:name%&8: &f{message}"
    vip: "&6[VIP] &f%mktessentials:name%&8: &f{message}"
```

### Text Commands (`messages.yml`)
```yaml
text-commands:
  rules:
    aliases: ["rules", "zasady"]
    messages:
      - "&6--- &eServer Rules &6---"
      - "&71. Be respectful to other players."
```

### Warn Escalation (`settings.yml`)
```yaml
moderation:
  max-warns: 3              # active warns that trigger an automatic tempban (0 = off)
  warn-ban-duration: "1d"   # tempban length when the limit is reached
```

### Teleportation (`settings.yml`)
```yaml
general:
  max-homes: 3              # default home limit (override per rank with mktessentials.homes.<n>)

teleportation:
  delay: 3                  # warmup seconds before a teleport executes (0 = instant)
  cooldown: 10              # global/default cooldown between teleports (0 = none)
  cooldown-tpa: -1          # per-type overrides (-1 = inherit "cooldown" above)
  cooldown-rtp: -1
  cooldown-warp: -1
  effects: true
  tpa-timeout: 60
```
Per-rank cooldowns are set with LuckPerms meta (`mktessentials.teleport_cooldown.tpa|rtp|warp`), which takes priority over these config values.

### RTP (`settings.yml`)
```yaml
rtp:
  min-distance: 500
  max-distance: 5000
  relative-to-player: true
  center-x: 0.0
  center-z: 0.0
  biome-blacklist:
    - "minecraft:ocean"
    - "minecraft:deep_ocean"
    - "minecraft:river"
```

### Item Management (`settings.yml`)
```yaml
items:
  despawn-time: 300       # seconds (0 = disabled)
  stacking: true
  stacking-radius: 3
  show-hologram: true
  sweep-interval: 0       # seconds (0 = disabled)
  sweep-warning: 30
  max-stack-size: 64
  whitelist:
    - "minecraft:netherite_*"
    - "create:brass_ingot"
```

## 📦 Building from Source

```bash
./gradlew build
```

Output JAR: `build/libs/mktessentials-0.4.0.jar`

## 🩺 Troubleshooting

| Symptom | Cause & Fix |
|---------|-------------|
| `Failed to start Discord bot` + `CloseCode 4014 (DISALLOWED_INTENTS)` | **Server Members Intent** is not enabled in the Discord Developer Portal. Enable it on the **Bot** tab and restart. |
| `Discord bot token is empty — bot will not start.` | Set `discord.bot-token` in `settings.yml` and `discord.enabled: true`. |
| Slash command shows "application did not respond" / `NoClassDefFoundError` | Re-download the latest JAR — all JDA transitive libraries are bundled in current builds. |
| Web "Disconnect" button doesn't actually unlink | Enable the [inbound web API](#channel-2--inbound-backend--mod) (`auth.web-sync.api.enabled: true`) and point your backend at it; clearing only the backend DB gets reverted by `backfill-on-start`. |
| LuckPerms not loading on 1.21.1 | Use the [patched LuckPerms](https://github.com/onmydestiny/LuckPerms-PATCHED). |
| Permissions ignored | Without LuckPerms the mod uses vanilla OP. Install LuckPerms and grant `mktessentials.*` nodes. |

## 📄 License

GPL-3.0-only
