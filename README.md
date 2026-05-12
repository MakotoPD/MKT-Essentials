# MKT Essentials

A powerful and lightweight Essentials mod for NeoForge 1.21.1, designed with stability and compatibility in mind.

## 🚀 Features

### 🏠 Teleportation
- **Homes** — `/sethome`, `/home`, `/delhome`, `/homes`
- **Warps** — `/setwarp`, `/warp`, `/delwarp`, `/warps`
- **TPA** — `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny` (multiple pending requests)
- **Spawn** — `/spawn`
- **RTP** — `/rtp` async random teleport with biome blacklist
- **Back** — `/back` return to previous location (death/teleport)
- **Top** — `/top` teleport to highest block
- **TpAll** — `/tpall` teleport all players to you
- **TP shortcuts** — `/tp <player>`, `/tphere <player>`, `/tppos <x> <y> <z>`

### 💬 Chat & Identity
- **Nicknames** — `/nick` custom display names
- **Chat Format** — Per-group chat formats via LuckPerms integration
- **Hover Info** — Hover over player names to see rank, ping, UUID
- **Recording/Streaming** — `/recording`, `/streaming` status indicators
- **AFK Detection** — Automatic [AFK] prefix after inactivity

### 📦 Kits
- **Create** — `/createkit <name> <cooldown>` from your inventory
- **Claim** — `/kit <name>` with cooldown tracking
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
- **Sudo** — `/sudo <player> <command>`

### 🔨 Moderation
- **Kick** — `/kick <player> [reason]`
- **Ban** — `/ban <player> [reason]`, `/tempban <player> <duration> [reason]`, `/unban <player>`
- **Mute** — `/mute <player> [duration]`, `/unmute <player>` (supports offline players)
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
- **Automated** — Configurable interval, random or sequential order
- **Custom prefix** — Per-broadcast formatting

### ⏰ Time & Weather
- `/day`, `/night`, `/sun`, `/rain`

### 🔍 Utility
- `/near [radius]` — List nearby players
- `/seen <player>` — When player was last online
- `/ping` — Show your latency
- `/hat` — Put item on head

## 📋 Command Reference

| Command | Description | Permission |
|---------|-------------|------------|
| `/home <name>` | Teleport to a saved home | `mktessentials.command.home` |
| `/sethome <name>` | Save current location as home | `mktessentials.command.sethome` |
| `/delhome <name>` | Delete a saved home | `mktessentials.command.delhome` |
| `/homes` | List all your homes | `mktessentials.command.listhomes` |
| `/warp <name>` | Teleport to a warp point | `mktessentials.command.warp` |
| `/setwarp <name>` | Create a warp point | `mktessentials.admin.setwarp` |
| `/delwarp <name>` | Delete a warp point | `mktessentials.admin.delwarp` |
| `/warps` | List all warp points | `mktessentials.command.listwarps` |
| `/spawn` | Teleport to world spawn | `mktessentials.command.spawn` |
| `/back` | Return to previous location | `mktessentials.command.back` |
| `/top` | Teleport to highest block above you | `mktessentials.command.top` |
| `/rtp` | Random teleport to a safe location | `mktessentials.command.rtp` |
| `/tpa <player>` | Request teleport to a player | `mktessentials.command.tpa` |
| `/tpahere <player>` | Request a player teleports to you | `mktessentials.command.tpahere` |
| `/tpaccept [player]` | Accept a teleport request | `mktessentials.command.tpa` |
| `/tpdeny [player]` | Deny a teleport request | `mktessentials.command.tpa` |
| `/tp <player>` | Instant teleport to player (admin) | `mktessentials.admin.tp` |
| `/tphere <player>` | Teleport player to you (admin) | `mktessentials.admin.tp` |
| `/tppos <x> <y> <z>` | Teleport to coordinates | `mktessentials.admin.tp` |
| `/tpall` | Teleport all players to you | `mktessentials.admin.tpall` |
| `/msg <player> <message>` | Send private message | `mktessentials.command.msg` |
| `/reply <message>` | Reply to last private message | `mktessentials.command.msg` |
| `/nick <nickname>` | Set your display name | `mktessentials.command.nick` |
| `/recording` | Toggle recording status indicator | `mktessentials.command.recording` |
| `/streaming` | Toggle streaming status indicator | `mktessentials.command.streaming` |
| `/kit <name>` | Claim a kit | `mktessentials.kit.<name>` |
| `/createkit <name> <cooldown>` | Create kit from inventory | `mktessentials.admin.kits` |
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
| `/sudo <player> <command>` | Force player to run command | `mktessentials.admin.sudo` |
| `/clearitems [radius]` | Remove ground items | `mktessentials.admin.clearitems` |
| `/invbackup save <player> [note]` | Create inventory backup | `mktessentials.admin.backup` |
| `/invbackup list <player>` | Browse backups in GUI | `mktessentials.admin.backup` |
| `/invbackup restore <player> <file>` | Restore a backup | `mktessentials.admin.backup` |
| `/invbackup delete <player> <file>` | Delete a backup | `mktessentials.admin.backup` |
| `/kick <player> [reason]` | Kick player from server | `mktessentials.moderation.kick` |
| `/ban <player> [reason]` | Permanently ban player | `mktessentials.moderation.ban` |
| `/tempban <player> <duration> [reason]` | Temporarily ban player | `mktessentials.moderation.tempban` |
| `/unban <player>` | Remove ban | `mktessentials.moderation.unban` |
| `/mute <player> [duration]` | Mute player | `mktessentials.admin.mute` |
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
| `/ping` | Show your latency in ms | `mktessentials.command.ping` |
| `/hat` | Put held item on your head | `mktessentials.command.hat` |
| `/day`, `/night` | Set world time | `mktessentials.admin.time` |
| `/sun`, `/rain` | Set weather | `mktessentials.admin.weather` |
| `/mkt help` | Show command help | All |
| `/mkt reload` | Reload configuration | `mktessentials.admin.reload` |
| `/mkt permissions` | List all permission nodes | `mktessentials.admin.permissions` |

## ⚙️ Configuration

YAML-based configuration in `config/mktessentials/`:

```
config/mktessentials/
├── settings.yml      — All settings (teleport, RTP, AFK, backups, auth, discord, items)
├── commands.yml      — Enable/disable individual commands
├── messages.yml      — Chat format, join/quit messages, broadcasts
├── accounts.db       — SQLite database (auth system)
└── lang/
    ├── en_us.yml     — English messages
    └── pl_pl.yml     — Polish messages
```

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

## 🔑 Permissions

| Node | Description | Default |
|------|-------------|---------|
| `mktessentials.command.home` | Home commands | All |
| `mktessentials.command.warp` | Warp commands | All |
| `mktessentials.command.spawn` | /spawn | All |
| `mktessentials.command.back` | /back | All |
| `mktessentials.command.rtp` | /rtp | All |
| `mktessentials.command.tpa` | TPA commands | All |
| `mktessentials.command.nick` | /nick | All |
| `mktessentials.command.hat` | /hat | All |
| `mktessentials.command.ping` | /ping | All |
| `mktessentials.command.near` | /near | All |
| `mktessentials.command.seen` | /seen | All |
| `mktessentials.admin.heal` | /heal | OP 2 |
| `mktessentials.admin.feed` | /feed | OP 2 |
| `mktessentials.admin.fly` | /fly | OP 2 |
| `mktessentials.admin.god` | /god | OP 2 |
| `mktessentials.admin.vanish` | /vanish | OP 2 |
| `mktessentials.admin.speed` | /speed | OP 2 |
| `mktessentials.admin.clearinv` | /clearinv | OP 2 |
| `mktessentials.admin.tpall` | /tpall | OP 2 |
| `mktessentials.admin.invsee` | /invsee | OP 2 |
| `mktessentials.admin.enderchest` | /enderchest | OP 2 |
| `mktessentials.admin.backup` | /invbackup | OP 2 |
| `mktessentials.admin.clearitems` | /clearitems | OP 2 |
| `mktessentials.admin.gamemode` | /gm, /gmc, /gms, /gma, /gmsp | OP 2 |
| `mktessentials.admin.tp` | /tp, /tphere, /tppos | OP 2 |
| `mktessentials.admin.give` | /i, /item | OP 2 |
| `mktessentials.admin.more` | /more | OP 2 |
| `mktessentials.admin.skull` | /skull | OP 2 |
| `mktessentials.admin.sudo` | /sudo | OP 3 |
| `mktessentials.admin.reload` | /mkt reload | OP 3 |
| `mktessentials.utility.repair` | /repair | OP 2 |
| `mktessentials.utility.enchant` | /enchant | OP 2 |
| `mktessentials.moderation.kick` | /kick | OP 2 |
| `mktessentials.moderation.ban` | /ban | OP 3 |
| `mktessentials.moderation.tempban` | /tempban | OP 3 |
| `mktessentials.moderation.unban` | /unban | OP 3 |
| `mktessentials.moderation.shadowban` | /shadowban | OP 3 |
| `mktessentials.admin.mute` | /mute | OP 2 |
| `mktessentials.auth.admin.reset` | /auth reset | OP 3 |
| `mktessentials.auth.admin.unlink` | /auth unlink | OP 3 |
| `mktessentials.auth.admin.info` | /auth info | OP 3 |

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
