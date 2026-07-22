# MKT Essentials

A comprehensive, **all-in-one** server-side utility mod for **NeoForge 1.21.1**. Everything you
need to run a Minecraft server — teleportation, rich chat, cosmetics, moderation, authentication,
a Discord bridge, inventory backups and more — in one lightweight package. Players join with a
**vanilla client** and need nothing installed.

---

## ✨ Why MKT Essentials?

- **All-in-one** — replaces a stack of separate mods with one.
- **Server-side only** — works with vanilla clients.
- **Self-sufficient** — tab list, nametags, MOTD and placeholders are all native; no companion mod required.
- **Fully configurable** — clean YAML, per-command toggles, English & Polish language files.
- **Permission-based** — a node for every command; works great with LuckPerms.

---

## 💬 Rich Chat

- **Markdown formatting** — `*italic*`, `**bold**`, `__underline__`, `~~strikethrough~~`, `??matrix??` — in chat and on anvil/book/sign text
- **Interactive chat** — clickable URLs, `:emoji:` shortcodes, and `||spoilers||` that reveal on hover
- **Inline objects** — `[item]` shows the item you're holding (with tooltip), `<head>` shows your player head
- **Mentions** — `@player` highlights and pings them; opt out with `/mentions`
- **Personal chat colour** — `/chatcolor`
- **Chat settings GUI** — `/chatsetting`, plus an ignore-list menu
- **Local / per-world chat** — nearby-only or per-world messaging with a global prefix
- **Roleplay, anonymous & stream** — `/me`, `/do`, `/anon`, `/stream`, plus `/symbol` glyphs

## 🎨 Cosmetics & Display

- **Nicknames** — `/nick` shows in chat, tab list, **and** above the player's head (skin preserved)
- **Native tab list** — custom header/footer, animations, optional ping column
- **Above-head nametags** — rank prefix/suffix via scoreboard teams
- **Below-name values, sidebar scoreboard & boss bar** — placeholders + animations
- **Server brand** — customise the F3 debug-screen server name
- **Welcome greeting** — personalised join message that can render the player's **skin face** as pixel art
- **MOTD & favicon** — set the server-list description and icon, with a separate maintenance MOTD

## 🔗 Discord Integration

Built-in **JDA** bot — no external plugins needed:

- **Account linking** — generate a 6-digit code in-game, verify with a Discord slash command; optional linked role
- **2-way chat relay** — in-game chat and join/leave mirror to a Discord channel (with webhook avatars showing each player's name and skin); channel messages appear in-game
- **Live status** — bot shows the current online player count
- **Safe by default** — blocks `@everyone` pings from the game, sanitises colour codes both ways

> Discord features require the **KotlinForForge** mod.

## 🔨 Moderation

- **Ban / Tempban / Unban** — duration parsing (`1d12h`, `30m`) and a ban screen with reason + time
- **IP bans** — `/banip`, `/unbanip` (works on offline players via last known IP)
- **Mute / Tempmute / Unmute** — timed or permanent, offline supported
- **Warnings** — `/warn` with automatic tempban escalation at a configurable limit
- **History** — `/history` full punishment record
- **Shadowban** — 4 methods including **phantom full isolation**: the player joins an empty world, sees no one, is seen by no one, gets no chat, and is silently muted in voice chat

## 🔐 Authentication

- **5 modes** — full, auth-only, link-only, optional, disabled
- **Register & Login** — bcrypt-hashed passwords
- **Freeze system** — unauthenticated players can't move, interact, or chat
- **Newbie protection** — invulnerability window for first-time players
- **Session management** — auto-login from the same IP within a timeout
- **Optional web sync** — two-way sync with an external website/backend

## 🏠 Teleportation

- **Homes** with **per-rank limits** via LuckPerms (`mktessentials.homes.<number>`)
- **Warps** (clickable GUI), **TPA**, **RTP** (async, biome blacklist, safe landing)
- **`/back`** (works after death), **Spawn**, **Top**, **TpAll**
- **Per-type cooldowns** — TPA, RTP and Warps run on independent clocks

## 📦 Kits & Items

- **Kits** — create via chest GUI or from inventory, per-kit cooldowns, clickable `/kits` browser
- **Virtual workstations** — `/workbench`, `/anvil`, `/grindstone`, `/stonecutter`, `/smithing`, plus `/trash`

## 💾 Inventory Backups

- Automatic on death/join/quit, optional scheduled backups, GUI browser, **Curios** support

## 🧹 Item Cleaner

- Auto-despawn with hologram countdown, entity stacking, whitelist (modded items & wildcards), manual sweep

## 🛡️ Admin & Utility

- Fly / God / Vanish (persist), gamemode shortcuts, `/invsee` & `/enderchest` (offline + Curios),
  `/speed`, `/repair`, `/enchant`, `/exp`, `/whois`, `/geolocate`, `/sudo`, `/tps` & `/lag`
- Auto-broadcasts, config-defined text commands, mail, polls, per-player time/weather

---

## 🔗 Optional Integrations

Auto-detected — installed = used, absent = ignored.

| Mod | What it adds |
|-----|-------------|
| **LuckPerms** | Permissions, prefix/suffix, per-group chat |
| **Text Placeholder API** | `%mktessentials:*%` placeholders in chat/tab |
| **TAB** | MKT's native tab/nametags step aside so TAB manages them |
| **MiniMOTD** | MKT's MOTD steps aside |
| **SkinsRestorer** | Greeting face uses the player's SkinsRestorer skin |
| **Plasmo Voice / Simple Voice Chat** | In-game mutes also mute voice chat |
| **Curios API** | Curios slots in `/invsee` and backups |

The mod works perfectly without any of them.

---

## ⚠️ Compatibility Notes

- **LuckPerms** on NeoForge 1.21.1 — the official build has a known bug; use the patched version: https://github.com/onmydestiny/LuckPerms-PATCHED
- **Text Placeholder API** — no official NeoForge 1.21.1 release; use the port: https://github.com/MakotoPD/TextPlaceholderAPI-NeoForge
- **Discord** — requires the **KotlinForForge** mod, and the bot's **Message Content Intent** (relay) / **Server Members Intent** (linking) enabled in the Discord Developer Portal.

## 📋 Requirements

- **Minecraft** 1.21.1 · **NeoForge** 21.1.228+ · **Java** 21

## 🔒 Permissions

Every command has its own node (`mktessentials.command.home`, `mktessentials.admin.fly`,
`mktessentials.moderation.ban`, …). Full list in-game via `/mkt permissions`. Some limits scale
per rank through LuckPerms (home limits, per-type teleport cooldowns).

## 📄 Links

- Source: https://github.com/MakotoPD/MKT-Essentials
- Issues: https://github.com/MakotoPD/MKT-Essentials/issues
- License: GPL-3.0-only
