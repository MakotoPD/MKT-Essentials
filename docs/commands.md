# Command Reference

Every command can be toggled in `commands.yml`. The authoritative, always-up-to-date permission
list is printed in-game with `/mkt permissions`; the table below is a convenient overview.

## Chat & Identity

| Command | Description | Permission |
|---------|-------------|------------|
| `/nick <nickname>` | Set your display name (chat + tab + above head) | `mktessentials.command.nick` |
| `/realname <nick>` | Look up the real name behind a nickname | `mktessentials.command.realname` |
| `/chatcolor <color>` | Set your personal chat colour | `mktessentials.command.chatcolor` |
| `/chatsetting` | Open the personal chat-settings GUI | `mktessentials.command.chatsetting` |
| `/mentions` | Toggle receiving `@` mention pings | `mktessentials.command.mentions` |
| `/anon <message>` | Send an anonymous message | `mktessentials.command.anon` |
| `/stream` | Post a stream announcement | `mktessentials.command.stream` |
| `/symbol` | Insert special symbols/glyphs | `mktessentials.command.symbol` |
| `/me <action>` | Roleplay action message | `mktessentials.command.me` |
| `/do <narration>` | Roleplay narration message | `mktessentials.command.do` |
| `/online` | Formatted online player list | `mktessentials.command.online` |
| `/mail <read\|send\|clear>` | Offline mail | `mktessentials.command.mail` |
| `/poll` | Create / vote in server polls | `mktessentials.command.poll` |
| `/msg <player> <message>` | Send private message | `mktessentials.command.msg` |
| `/reply <message>` | Reply to last private message | `mktessentials.command.msg` |
| `/msgtoggle` | Block private messages | `mktessentials.command.msgtoggle` |
| `/ignore <player>` | Ignore a player's chat and PMs | `mktessentials.command.ignore` |
| `/socialspy` | See private messages (staff) | `mktessentials.admin.socialspy` |
| `/afk` | Toggle AFK status | `mktessentials.command.afk` |
| `/recording`, `/streaming` | Status indicators | `mktessentials.command.recording` / `.streaming` |

## Teleportation

| Command | Description | Permission |
|---------|-------------|------------|
| `/home <name>` | Teleport to a saved home | `mktessentials.command.home` |
| `/sethome <name>` | Save current location as home | `mktessentials.command.sethome` |
| `/delhome <name>` | Delete a saved home | `mktessentials.command.delhome` |
| `/listhomes` | List all your homes | `mktessentials.command.listhomes` |
| `/warp <name>` | Teleport to a warp | `mktessentials.command.warp` |
| `/warps` | Browse warps in a clickable GUI | `mktessentials.command.listwarps` |
| `/setwarp <name>` / `/delwarp <name>` | Create / delete a warp | `mktessentials.admin.setwarp` / `.delwarp` |
| `/spawn` / `/setspawn` | Teleport to / set the server spawn | `mktessentials.command.spawn` / `mktessentials.admin.setspawn` |
| `/back` | Return to previous location (incl. death) | `mktessentials.command.back` |
| `/top` | Teleport to the highest block | `mktessentials.command.top` |
| `/rtp` | Random safe teleport | `mktessentials.command.rtp` |
| `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel` | Teleport requests | `mktessentials.command.tpa` |
| `/tptoggle` | Block incoming teleport requests | `mktessentials.command.tptoggle` |
| `/tp`, `/tphere`, `/tppos`, `/tpall` | Instant teleport / coordinates / all (staff) | `mktessentials.admin.tp` / `.tpall` |

## Kits & Items

| Command | Description | Permission |
|---------|-------------|------------|
| `/kit <name>` | Claim a kit | `mktessentials.kit.<name>` |
| `/kits` | Browse kits in a clickable GUI | All |
| `/createkit <name> <cooldown> [frominv]` | Create a kit (chest GUI or from inventory) | `mktessentials.admin.kits` |
| `/deletekit <name>` | Delete a kit | `mktessentials.admin.kits` |
| `/i <item> [amount]`, `/more`, `/skull <player>` | Give items | `mktessentials.admin.give` / `.more` / `.skull` |
| `/repair`, `/enchant <ench> <lvl>` | Fix / enchant held item | `mktessentials.utility.repair` / `.enchant` |
| `/hat` | Put held item on your head | `mktessentials.command.hat` |
| `/trash` | Disposal chest (items destroyed on close) | `mktessentials.command.trash` |
| `/workbench` (`/craft`), `/anvil`, `/grindstone`, `/stonecutter`, `/smithing` | Virtual workstations | `mktessentials.command.<name>` |

## Admin

| Command | Description | Permission |
|---------|-------------|------------|
| `/heal`, `/feed` | Restore health / hunger | `mktessentials.admin.heal` / `.feed` |
| `/fly`, `/god`, `/vanish` | Flight / invulnerability / invisibility (persist) | `mktessentials.admin.fly` / `.god` / `.vanish` |
| `/speed fly\|walk <1-10>` | Movement speed | `mktessentials.admin.speed` |
| `/gmc`, `/gms`, `/gma`, `/gmsp`, `/gm <0-3>` | Gamemode | `mktessentials.admin.gamemode` |
| `/invsee <player>`, `/enderchest <player>` | View/edit inventory (online + offline, Curios) | `mktessentials.admin.invsee` / `.enderchest` |
| `/clearinv [player]`, `/clearitems [radius]` | Clear inventory / ground items | `mktessentials.admin.clearinv` / `.clearitems` |
| `/exp give\|set <player> <levels>` | Manage experience | `mktessentials.admin.exp` |
| `/whois <player>` | Detailed player info (works offline) | `mktessentials.admin.whois` |
| `/geolocate <player>` | Approximate location of a connected IP | `mktessentials.admin.geolocate` |
| `/sudo <player> <command>` | Force a player to run a command | `mktessentials.admin.sudo` |
| `/maintenance` | Toggle maintenance mode | `mktessentials.admin.maintenance` |
| `/invbackup save\|list\|restore\|delete <player>` | Inventory backups | `mktessentials.admin.backup` |

## Moderation

| Command | Description | Permission |
|---------|-------------|------------|
| `/kick <player> [reason]` | Kick | `mktessentials.moderation.kick` |
| `/ban`, `/tempban <dur>`, `/unban` | Ban / tempban / unban | `mktessentials.moderation.ban` / `.tempban` / `.unban` |
| `/banip <player\|ip>`, `/unbanip <ip>` | IP ban (offline via last IP) | `mktessentials.moderation.banip` |
| `/mute`, `/tempmute <dur>`, `/unmute` | Mute (offline supported) | `mktessentials.admin.mute` / `.unmute` |
| `/warn`, `/unwarn`, `/warns` | Warnings (auto-tempban at limit) | `mktessentials.moderation.warn` |
| `/history <player>` | Full punishment history | `mktessentials.moderation.history` |
| `/shadowban`, `/unshadowban`, `/shadowbanlist` | Shadowban (see [Moderation](moderation.md)) | `mktessentials.moderation.shadowban` |

## Authentication & Discord

| Command | Description | Permission |
|---------|-------------|------------|
| `/register <pw> <pw>`, `/login <pw>`, `/changepassword <old> <new> <new>` | Accounts | All |
| `/link`, `/unlink`, `/discord` | Discord account linking | All |
| `/auth reset\|unlink\|info <player>` | Auth admin | `mktessentials.auth.admin.*` |

## Utility & Info

| Command | Description | Permission |
|---------|-------------|------------|
| `/near [radius]`, `/seen <player>`, `/playtime [player]`, `/ping` | Lookups | `mktessentials.command.<name>` |
| `/tps`, `/lag` | Server performance | `mktessentials.command.tps` / `mktessentials.admin.lag` |
| `/day`, `/noon`, `/night`, `/midnight`, `/sun`, `/rain`, `/storm` | World time & weather | `mktessentials.admin.time` / `.weather` |
| `/ptime <preset\|ticks\|reset>`, `/pweather clear\|rain\|reset` | Personal client-side time/weather | `mktessentials.command.ptime` / `.pweather` |
| `/broadcast <message>`, `/helpop <msg>`, `/report <player> <reason>` | Staff messaging | `mktessentials.admin.broadcast` / `mktessentials.command.helpop` / `.report` |
| `/rules`, `/www`, `/vote`, … | Config-defined text commands | `mktessentials.command.text.<name>` |
| `/mkt help\|reload\|permissions` | Mod management | `mktessentials.admin.reload` / `.permissions` |

← Back to the [README](../README.md)
