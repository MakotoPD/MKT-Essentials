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
- **Inventory** — `/invsee <player>`, `/enderchest <player>` (supports offline players + Curios)
- **Clear** — `/clearinv`
- **Repair** — `/repair` item in hand
- **Enchant** — `/enchant <enchantment> <level>`

### 🔨 Moderation
- **Kick** — `/kick <player> [reason]`
- **Ban** — `/ban <player> [reason]`, `/tempban <player> <duration> [reason]`, `/unban <player>`
- **Mute** — `/mute <player> [duration]`, `/unmute <player>` (supports offline players)
- **Ban Screen** — Banned players see reason + remaining time on connect

### 💾 Inventory Backups
- **Auto-backup** — On death, join, quit (configurable)
- **Manual** — `/invbackup save <player> [note]`
- **Browse** — `/invbackup list <player>` opens GUI with backup history
- **Restore** — Click a backup in GUI or `/invbackup restore <player> <file>`
- **Delete** — `/invbackup delete <player> <file>`
- **Scheduled** — Optional periodic backups for all online players

### 📢 Broadcasts
- **Automated** — Configurable interval, random or sequential order
- **Custom prefix** — Per-broadcast formatting

### ⏰ Time & Weather
- `/day`, `/night`, `/sun`, `/rain`

## ⚙️ Configuration

MKT Essentials uses a YAML-based configuration system in `config/mktessentials/`:

```
config/mktessentials/
├── settings.yml      — Teleportation, RTP, AFK, backups, general settings
├── commands.yml      — Enable/disable individual commands
├── messages.yml      — Chat format, join/quit messages, broadcasts
└── lang/
    ├── en_us.yml     — English messages
    └── pl_pl.yml     — Polish messages
```

### Command Toggle
Disable any command by setting it to `false` in `commands.yml`:
```yaml
admin:
  fly: true
  god: true
  vanish: false  # disabled
```

### Internationalization
Change language in `settings.yml`:
```yaml
language: "pl_pl"
```
Add custom languages by creating new files in `config/mktessentials/lang/`.

### Per-Group Chat Format
In `messages.yml`:
```yaml
chat:
  format: "%mktessentials:dot%%mktessentials:prefix%%mktessentials:name%%mktessentials:suffix%&8: &f{message}"
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
| `mktessentials.admin.reload` | /mkt reload | OP 3 |
| `mktessentials.utility.repair` | /repair | OP 2 |
| `mktessentials.utility.enchant` | /enchant | OP 2 |
| `mktessentials.moderation.kick` | /kick | OP 2 |
| `mktessentials.moderation.ban` | /ban | OP 3 |
| `mktessentials.moderation.tempban` | /tempban | OP 3 |
| `mktessentials.moderation.unban` | /unban | OP 3 |
| `mktessentials.admin.mute` | /mute | OP 2 |

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

All integrations are optional — the mod works without them.

## 📦 Building

```bash
./gradlew build
```

Output JAR: `build/libs/mktessentials-1.0.0.jar`

## 📄 License

GPL-3.0-only
