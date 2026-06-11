# Changelog

## [0.3.0]

### New Features

- **`/back` now works after death** — your death location is saved automatically, and a hint message tells you that `/back` will return you there.

- **`/setspawn`** — set a custom server spawn point at your current location. `/spawn` teleports to it instead of the world spawn (stored in `spawn.json`).

- **`/afk`** — manually toggle AFK status. Moving, chatting, or running `/afk` again clears it.

- **`/tpacancel` and `/tptoggle`** — cancel your outgoing teleport requests, or block incoming ones entirely. Staff with `mktessentials.admin.tptoggle.bypass` can still send you requests.

- **`/msgtoggle` and `/ignore <player>`** — block all private messages, or ignore a specific player (hides both their chat messages and their `/msg`). Staff with `mktessentials.admin.msgbypass` bypass these.

- **Warn system: `/warn`, `/unwarn`, `/warns`, `/history`** — warn players with a reason and browse a full punishment history (warns, bans, tempbans, mutes, kicks are all recorded in `punishments.json`). Reaching the configurable warn limit (`moderation.max-warns`, default 3) automatically tempbans the player for `moderation.warn-ban-duration` (default 1d).

- **`/tempmute <player> <duration> [reason]`** — timed mute with a required duration and an optional reason, recorded in the punishment history.

- **IP bans: `/banip <player|ip> [reason]` and `/unbanip <ip>`** — ban by player name (online or offline, using their last known IP) or by literal IP. Everyone connected from that IP is kicked immediately, and banned IPs are rejected at login (stored in `ipbans.json`).

- **`/whois <player>`** — staff overview of a player: UUID, nickname, IP, ping, gamemode, location, health/food, fly/god/vanish status, play time, first join, and mute status. Works for offline players too.

- **`/playtime [player]`** — total play time, tracked per session from now on.

- **`/trash`** — a disposal chest; items left inside are destroyed when you close it.

- **Virtual workstations: `/workbench` (alias `/craft`), `/anvil`, `/grindstone`, `/stonecutter`, `/smithing`** — open the vanilla menus anywhere, no block required.

- **`/exp give|set <player> <levels>`** — manage player experience levels.

- **`/ptime` and `/pweather`** — per-player, client-side time (`day`, `noon`, `night`, `midnight`, exact ticks, `reset`) and weather (`clear`, `rain`, `reset`). Affects only what you see; the server stays untouched.

- **Kits and warps GUI** — `/kits` opens a clickable menu showing each kit's icon, item count, cooldown, and ready status (click to claim). `/warps` opens a clickable warp list (click to teleport).

- **Kit creation GUI** — `/createkit <name> <cooldown>` now opens a chest: place the kit contents, close it, done — your items are given back. The old behavior (snapshot of your inventory) is available as `/createkit <name> <cooldown> frominv`.

- **`/helpop <message>` and `/report <player> <reason>`** — contact online staff (anyone with `mktessentials.admin.helpop`). Messages are also logged to the console, with a 30-second cooldown for regular players.

- **`/tps` and `/lag`** — server performance at a glance: TPS, MSPT, memory usage, player count, and loaded chunks/entities per dimension.

- **Configurable text commands** — define your own info commands in `messages.yml` (`text-commands` section), each with custom aliases and message lines. Ships with `/rules` (alias `/zasady`), `/www`, `/vote`, and `/discordinvite` (alias `/dc`) as editable examples.

### Improvements

- **`/mkt help` is now complete and accurate** — all commands added in this release are listed, plus previously missing ones (`/socialspy`, `/broadcast`, `/kickme`, `/noon`, `/midnight`, `/storm`, `/createkit`, `/deletekit`). A new **Account** section appears when the auth system is enabled, showing `/register`, `/login`, `/changepassword`, `/link`, `/unlink`, `/discord` based on the configured auth mode. Text commands are listed dynamically from the config, so renamed aliases (e.g. `/zasady`) show up correctly. Also fixed: the help claimed `/homes` exists — the actual command is `/listhomes`.

- **`/mkt permissions` lists all new permission nodes** — moderation nodes (`mktessentials.moderation.warn`, `.banip`, `.history`), admin nodes (`admin.whois`, `admin.exp`, `admin.lag`, `admin.setspawn`, `admin.helpop`, `admin.msgbypass`, `admin.tptoggle.bypass`), and player nodes for all new commands (including `command.text.<name>` for text commands).

- **Auto-broadcasts** — new `broadcast.enabled` toggle in `messages.yml`, and broadcasts now pause while the server is empty instead of firing the moment the first player joins.

- **New commands can be toggled** in `commands.yml`: `warn`, `banip`, `exp`, `whois`, `playtime`, `stations`, `ptime`, `helpop`, `tps`.

### Bug Fixes

- **Security: Discord linking now runs on the server thread** — completing a `/link` from Discord previously executed database queries and player state changes on the bot's thread, which could corrupt data or crash. It is now safely scheduled on the main thread.

- **Security: `/unlink` (and `/auth unlink`) now invalidates the session** — previously a relog after unlinking would auto-authenticate the player and bypass the Discord link requirement.

- **Security: failed login attempts are now counted per IP** in a 10-minute window — previously disconnecting and rejoining reset the counter, making the max-attempts limit useless against brute force.

- **Security: changing your password now invalidates old sessions** — a session created before the password change (e.g. by someone who knew the old password) no longer stays valid.

- **Players are now frozen immediately on join** when authentication is required — previously there was a 1-tick window before the freeze applied.

- **Kicks for too many login attempts show the correct message** — previously the "took too long to log in" message was shown (new lang key: `auth.kicked-max-attempts`).

- **Corrupted or empty player data files no longer break joining** — the mod now falls back to fresh data instead of caching `null` or crashing the join handler.

- **AFK announcements no longer reveal vanished or shadowbanned players.**

- **Auth database failures no longer cause crashes** — if the database fails to initialize, queries are skipped with a logged error instead of throwing.

- **Teleport cooldowns are cleaned up** when they expire and when players disconnect (slow memory leak fix), and chunk pre-loading now targets the correct chunk at negative coordinates.

- **Ban/mute durations are protected against overflow** — absurdly large values now show an error instead of silently producing an already-expired ban.

- **Items no longer become permanently undespawnable** after the item despawn feature is disabled in the config.

- **Player data is explicitly saved on server shutdown**, and data modified for offline players (e.g. offline mutes) is saved and released from memory immediately.

## [0.2.1]

### Bug Fixes

- **`/speed` command is now always available** — previously, the command would silently stop working if `/heal`, `/fly`, and `/god` were all disabled in the config. It now registers independently regardless of other commands being toggled off.

- **`/speed` values now make sense** — value `1` is the default (vanilla) speed for both walking and flying. The minimum allowed value is `1` (previously `0`, which would completely freeze the player).

- **`/speed` changes now persist across reconnects** — your custom walk and fly speed are saved and automatically restored when you rejoin the server. Previously they would reset every time you disconnected.

- **`/speed fly` now warns you if the target can't fly** — if the player doesn't have flight enabled (and isn't in Creative/Spectator), you'll get a notice that the speed was saved but won't take effect until flight is turned on.

- **Join/quit messages now work without PlaceholderAPI** — if the PlaceholderAPI mod is not installed on the server, join and leave messages will correctly show the player's name instead of displaying a raw placeholder like `%mktessentials:full_name/safe%`.
