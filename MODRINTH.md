# MKT Essentials

A comprehensive, all-in-one server-side utility mod for NeoForge 1.21.1. Everything you need to run a Minecraft server — homes, warps, moderation, authentication, inventory backups, and much more — in a single lightweight package.

---

## ✨ Why MKT Essentials?

- **All-in-one** — No need for 10+ separate mods. Teleportation, moderation, kits, chat formatting, auth, and admin tools in one place.
- **Server-side only** — Players don't need to install anything. Works with vanilla clients.
- **Fully configurable** — YAML-based config with per-command toggles, i18n support (English & Polish), and per-group chat formatting.
- **Lightweight** — Designed for performance with async operations, entity stacking, and smart caching.
- **Permission-based** — Fine-grained permission nodes for every command. Works with LuckPerms out of the box.

---

## 🏠 Teleportation

- **Homes** — Set, teleport to, and manage personal home locations
- **Warps** — Server-wide warp points for admins to create
- **TPA** — Teleport requests with support for multiple pending requests
- **RTP** — Async random teleport with biome blacklist and safe-landing checks
- **Back** — Return to your previous location after death or teleport
- **Spawn / Top / TpAll** — Quick navigation commands

## 🔐 Authentication & Discord Linking

Built-in authentication system with an embedded Discord bot — no external plugins needed.

- **5 Auth Modes** — Full, auth-only, link-only, optional, or disabled
- **Register & Login** — Password-based with BCrypt hashing
- **Discord Account Linking** — Generate a 6-digit code in-game, verify on Discord
- **Embedded Discord Bot** — JDA-powered, registers slash commands, assigns roles on link
- **Freeze System** — Unauthenticated players can't move, interact, or chat
- **Newbie Protection** — Configurable invulnerability period for first-time players
- **Session Management** — Auto-login from same IP within configurable timeout

## 🔨 Moderation

- **Ban / Tempban / Unban** — With duration parsing (`1d12h`, `30m`) and ban screen with reason + remaining time
- **Mute / Unmute** — Timed or permanent, works on offline players
- **Kick** — With optional reason
- **Shadowban** — 4 methods: timeout, full disconnect, fake internal-error, or phantom mode (hidden from tab + silenced chat)

## 💾 Inventory Backups

- **Automatic** — On death, join, and quit (each toggleable)
- **Scheduled** — Optional periodic backups for all online players
- **GUI Browser** — Click to preview, right-click to restore
- **Curios Support** — Backs up and restores Curios API slots

## 🧹 Item Cleaner

- **Auto-despawn** — Configurable timer with floating hologram countdown
- **Entity Stacking** — Merges identical nearby ground items to reduce lag
- **Whitelist** — Items that never despawn (supports modded items and wildcards)
- **Manual Sweep** — `/clearitems [radius]` for instant cleanup

## 💬 Chat & Identity

- **Per-group Chat Format** — Different formats per LuckPerms group
- **Hover Info** — Hover over names to see rank, ping, and UUID
- **Nicknames** — Custom display names with formatting
- **Recording/Streaming** — Status indicators in chat
- **AFK Detection** — Automatic [AFK] prefix after configurable inactivity

## 🛡️ Admin Tools

- **Gamemode Shortcuts** — `/gmc`, `/gms`, `/gma`, `/gmsp`
- **Fly / God / Vanish** — All persist across reconnects
- **Inventory Editing** — `/invsee` and `/enderchest` work on offline players too
- **Curios Integration** — `/invsee` displays Curios slots in the GUI
- **Give / More / Skull** — Quick item commands
- **Repair / Enchant** — Fix or enchant held items
- **Speed** — Separate fly and walk speed control
- **Sudo** — Force a player to execute a command

## 📦 Kits

- Create kits from your current inventory
- Per-kit cooldowns
- Permission-based access (`mktessentials.kit.<name>`)

## 📢 Broadcasts

- Automated broadcast messages at configurable intervals
- Random or sequential order
- Custom prefix per broadcast

---

## ⚙️ Configuration

Clean YAML-based configuration split into logical files:

```
config/mktessentials/
├── settings.yml      — All feature settings
├── commands.yml      — Enable/disable any command
├── messages.yml      — Chat format, join/quit, broadcasts
└── lang/
    ├── en_us.yml     — English
    └── pl_pl.yml     — Polish
```

Every command can be individually toggled on or off. Every player-facing message is customizable through the language files.

---

## 🔗 Optional Integrations

| Mod | What it adds |
|-----|-------------|
| **LuckPerms** | Permissions, prefix/suffix, per-group chat format |
| **Text Placeholder API** | Placeholders in chat and tab (`%mktessentials:name%`, etc.) |
| **TAB** | Tab list placeholders |
| **Curios API** | Curios slots visible in `/invsee` and included in backups |

All integrations are fully optional — the mod works perfectly without any of them.

---

## ⚠️ Known Issues & Compatibility Notes

### LuckPerms on NeoForge 1.21.1
The official LuckPerms build for NeoForge 1.21.1 has a known bug. Use the patched version from [here](https://github.com/onmydestiny/LuckPerms-PATCHED) instead.

### Text Placeholder API (NeoForge port)
The original Text Placeholder API does not have an official NeoForge 1.21.1 release. Use the community NeoForge port:
👉 [TextPlaceholderAPI-NeoForge](https://github.com/MakotoPD/TextPlaceholderAPI-NeoForge)

---

## 📋 Requirements

- **Minecraft** 1.21.1
- **NeoForge** 21.1.228+
- **Java** 21

---

## 🔒 Permissions

Every command has its own permission node (e.g. `mktessentials.command.home`, `mktessentials.admin.fly`, `mktessentials.moderation.ban`). Use any permission manager — LuckPerms recommended.

Full permission list available in-game via `/mkt permissions`.

---

## 📄 Links

- [Source Code](https://github.com/MakotoPD/MKT-Essentials)
- [Issue Tracker](https://github.com/MakotoPD/MKT-Essentials/issues)
