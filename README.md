# MKT Essentials

A powerful and lightweight Essentials mod for NeoForge 1.21.1, designed with stability and compatibility in mind.

## 🚀 Key Features

### 🏠 Teleportation System
*   **Homes**: `/sethome`, `/home`, `/delhome`
*   **Warps**: `/setwarp`, `/warp`, `/delwarp`, `/listwarps`
*   **TPA**: `/tpa`, `/tpaccept`, `/tpdeny`, `/tpcancel`
*   **Spawn**: `/spawn`, `/setspawn`
*   **RTP**: `/rtp` with configurable distance and safety checks.
*   **Back**: `/back` to return to your previous location (on death or teleport).

### 💬 Chat & Identity
*   **Nicknames**: Set custom display names with `/nick`.
*   **Identity Modes**: Toggle Recording `/recording` or Streaming `/streaming` indicators.
*   **LuckPerms Support**: Fully integrated with LuckPerms for prefixes, suffixes, and colors.
*   **Custom Chat Format**: Highly configurable chat layout in `mktessentials-common.toml`.

### 📦 Kit System
*   **Kit Management**: Create kits from your inventory with `/createkit <name> <cooldown>`.
*   **Claiming**: `/kit <name>` to receive items with NBT preservation (enchants, custom names).
*   **Cooldowns**: Robust cooldown tracking per player.

### 🛡️ Admin Utilities
*   **Vanish**: Hide from other players and the Tab list with `/vanish`.
*   **Mute**: Mute problematic players with `/mute <player> <duration>`.
*   **God Mode**: Invulnerability with `/god`.
*   **Maintenance**: `/heal`, `/feed`, `/fly`, `/speed`, `/clearinv`.
*   **Global Broadcast**: `/broadcast` for server-wide announcements.

## 🔑 Permissions
MKT Essentials uses a granular permission system. You can view all nodes in-game using:
` /mkt permissions`

### 🔑 Full Permission List

| Permission Node | Description | Default Level |
|-----------------|-------------|---------------|
| `mktessentials.admin.permissions` | Access to `/mkt permissions` | OP 2 |
| `mktessentials.admin.kits` | Create and delete kits | OP 2 |
| `mktessentials.admin.mute` | Mute and unmute players | OP 2 |
| `mktessentials.admin.vanish` | Use vanish mode | OP 2 |
| `mktessentials.admin.speed` | Change flying speed | OP 2 |
| `mktessentials.admin.tpall` | Teleport all players to you | OP 2 |
| `mktessentials.admin.broadcast` | Send server-wide announcements | OP 2 |
| `mktessentials.admin.weather` | Change weather (sun/rain/thunder) | OP 2 |
| `mktessentials.admin.time` | Change world time | OP 2 |
| `mktessentials.admin.heal` | Heal yourself or others | OP 2 |
| `mktessentials.admin.feed` | Feed yourself or others | OP 2 |
| `mktessentials.admin.fly` | Toggle flight mode | OP 2 |
| `mktessentials.admin.god` | Toggle invulnerability | OP 2 |
| `mktessentials.admin.clearinv` | Clear player inventory | OP 2 |
| `mktessentials.admin.nick` | Change or reset another player's nickname | OP 2 |
| `mktessentials.kit.<name>` | Claim a specific kit | All |
| `mktessentials.command.home` | Use /home | All |
| `mktessentials.command.sethome` | Use /sethome | All |
| `mktessentials.command.delhome` | Use /delhome | All |
| `mktessentials.command.listhomes` | Use /listhomes | All |
| `mktessentials.command.spawn` | Use /spawn | All |
| `mktessentials.command.tpa` | Use /tpa, /tpaccept, /tpdeny | All |
| `mktessentials.command.tpahere` | Use /tpahere | All |
| `mktessentials.command.warp` | Use /warp | All |
| `mktessentials.command.listwarps` | Use /listwarps | All |
| `mktessentials.command.rtp` | Use /rtp | All |
| `mktessentials.command.back` | Use /back | All |
| `mktessentials.command.nick` | Use /nick and /nickname for yourself | All |
| `mktessentials.command.recording` | Use /recording | All |
| `mktessentials.command.streaming` | Use /streaming | All |

## ⚙️ Configuration
The mod generates a professional configuration file at `config/mktessentials-common.toml`.
You can customize:
*   **Chat Formats**: `{dot}{prefix}{name}{suffix}: {message}`
*   **Teleport Delays**: Set warmups for RTP and other teleports.
*   **Messages**: Customize join/quit, welcome, and vanish messages.

## 🛠️ Requirements & Compatibility
*   **NeoForge**: 1.21.1 (v21.1.228+)
*   **LuckPerms**: Recommended for full chat integration (Use v5.5+ for NeoForge stability).
*   **TAB Mod**: Fully compatible. To use MKT Essentials nicknames in your TabList, use `%mkt_full_name%`, `%mkt_prefix%`, or `%mkt_suffix%` in your TAB config.
