# MKT Essentials

A powerful and lightweight Essentials mod for NeoForge 1.21.1, designed with stability and compatibility in mind.

> ⚠️ **Important Compatibility Notes**
>
> - **LuckPerms** — The official NeoForge 1.21.1 build has a known bug. Use the [patched version](https://github.com/onmydestiny/LuckPerms-PATCHED) instead.
> - **Text Placeholder API** — No official NeoForge 1.21.1 release exists. Use the [NeoForge port](https://github.com/MakotoPD/TextPlaceholderAPI-NeoForge).

## 🚀 Features

### 🏠 Teleportation
- **Homes** — `/sethome`, `/home`, `/delhome`, `/listhomes`
- **Warps** — `/setwarp`, `/warp`, `/delwarp`, `/warps` (clickable GUI), `/listwarps`
- **TPA** — `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel` (multiple pending requests)
- **TP toggle** — `/tptoggle` block incoming teleport requests
- **Spawn** — `/spawn`, `/setspawn` custom server spawn point
- **RTP** — `/rtp` async random teleport with biome blacklist
- **Back** — `/back` return to previous location (works after death too)
- **Top** — `/top` teleport to highest block
- **TpAll** — `/tpall` teleport all players to you
- **TP shortcuts** — `/tp <player>`, `/tphere <player>`, `/tppos <x> <y> <z>`

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

## ⚙️ Configuration

YAML-based configuration in `config/mktessentials/`:

```
config/mktessentials/
├── settings.yml      — All settings (teleport, RTP, AFK, backups, auth, discord, items, warns)
├── commands.yml      — Enable/disable individual commands
├── messages.yml      — Chat format, join/quit messages, broadcasts, text commands
├── accounts.db       — SQLite database (auth system)
└── lang/
    ├── en_us.yml     — English messages
    └── pl_pl.yml     — Polish messages
```

Player data, warps, kits, bans, IP bans, punishment history and the spawn point are stored per-world in `<world>/mktessentials/`.

### Command Toggle
```yaml
admin:
  fly: true
  god: true
  vanish: false  # disabled
```

### Internationalization
```yaml
language: "pl_pl"
```

### Per-Group Chat Format
```yaml
chat:
  group-formats:
    admin: "&c[Admin] &f%mktessentials:name%&8: &f{message}"
    vip: "&6[VIP] &f%mktessentials:name%&8: &f{message}"
```

### Text Commands
```yaml
text-commands:
  rules:
    aliases: ["rules", "zasady"]
    messages:
      - "&6--- &eServer Rules &6---"
      - "&71. Be respectful to other players."
```

### Warn Escalation
```yaml
moderation:
  max-warns: 3              # active warns that trigger an automatic tempban (0 = off)
  warn-ban-duration: "1d"   # tempban length when the limit is reached
```

## 🛠️ Requirements

- **NeoForge** 1.21.1 (v21.1.228+)
- **Java** 21

## 🔗 Optional Integrations

| Mod | Integration |
|-----|-------------|
| **LuckPerms** | Permissions, prefixes/suffixes, per-group chat format |
| **Text Placeholder API** | Placeholder support in chat/tab (`%mktessentials:...%`) |
| **TAB** | Tab list placeholders (`%mkt_full_name%`, `%mkt_prefix%`, `%mkt_suffix%`) |
| **Curios API** | /invsee shows Curios slots |
| **Discord** | Embedded bot for account linking (JDA) |

All integrations are optional — the mod works without them.

## 📦 Building

```bash
./gradlew build
```

Output JAR: `build/libs/mktessentials-1.0.0.jar`

## 📄 License

GPL-3.0-only
