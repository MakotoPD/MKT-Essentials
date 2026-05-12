package pl.makoto.essentials.config;

/**
 * Contains static String constants with the full default YAML content (including comments)
 * for each configuration file. These are written verbatim to disk on first run.
 * Since SnakeYAML strips comments on write, we use pre-authored template strings.
 */
final class DefaultTemplates {

    private DefaultTemplates() {
        // Utility class — no instantiation
    }

    static final String SETTINGS_YML = """
            # ============================================
            #  MKT Essentials - Settings
            # ============================================
            # Main configuration file for MKT Essentials.
            # Edit values below to customize the plugin.
            # Changes require /mkt reload or server restart.

            # Language for server messages (file must exist in lang/ folder)
            # Available: en_us, pl_pl (or add your own)
            language: "en_us"

            # ============================================
            #  General Settings
            # ============================================
            general:
              # Prefix shown before all mod messages (supports & color codes)
              message-prefix: "&8[&6MKT&8] &r"
              # Maximum homes per player (LuckPerms meta can override: mktessentials.max_homes.<number>)
              max-homes: 3

            # ============================================
            #  Teleportation
            # ============================================
            teleportation:
              # Delay in seconds before teleport executes (0 = instant)
              delay: 3
              # Cooldown between teleports in seconds (0 = no cooldown)
              cooldown: 10
              # Play sound and particle effects on teleport
              effects: true
              # Seconds before a TPA request expires
              tpa-timeout: 60

            # ============================================
            #  Random Teleport (RTP)
            # ============================================
            rtp:
              # Minimum distance from center/player
              min-distance: 500
              # Maximum distance from center/player
              max-distance: 5000
              # true = distance relative to player, false = relative to center coordinates
              relative-to-player: true
              # Center coordinates (used when relative-to-player is false)
              center-x: 0.0
              center-z: 0.0
              # Biomes to exclude from RTP (players won't land in these)
              biome-blacklist:
                - "minecraft:ocean"
                - "minecraft:deep_ocean"
                - "minecraft:river"

            # ============================================
            #  AFK Detection
            # ============================================
            afk:
              # Seconds of inactivity before marking player as AFK (0 = disabled)
              timeout: 300

            # ============================================
            #  Data Management
            # ============================================
            data:
              # Interval in seconds between automatic player data saves (0 = disabled)
              auto-save-interval: 300

            # ============================================
            #  Vanish Settings
            # ============================================
            vanish:
              # Send fake join/quit messages when admins vanish/unvanish
              fake-messages: true

            # ============================================
            #  Inventory Backups
            # ============================================
            backup:
              # Automatically backup inventory on player death
              on-death: true
              # Automatically backup inventory when player joins the server
              on-join: false
              # Automatically backup inventory when player leaves the server
              on-quit: true
              # Interval in seconds between scheduled backups for all online players (0 = disabled)
              interval: 0
              # Maximum number of backups to keep per player (oldest are deleted)
              max-per-player: 10

            # ============================================
            #  Authentication & Discord Link
            # ============================================
            auth:
              # Mode: "full", "auth-only", "link-only", "optional", "disabled"
              mode: "disabled"
              # Hours before a session expires (auto-login from same IP)
              session-timeout-hours: 24
              # Maximum failed login attempts before kick
              max-login-attempts: 5
              # Seconds before an unauthenticated player is kicked
              login-timeout-seconds: 60
              # Minutes of invulnerability for first-time players (0 = disabled)
              newbie-protection-minutes: 30

            # ============================================
            #  Shadowban
            # ============================================
            moderation:
              # Shadowban method: "timeout", "full", "internal-error", "phantom"
              # timeout: Shows "Connection timed out" on join
              # full: Shows "Server is full!" on join
              # internal-error: Kicks after 2-3 seconds with fake internal error
              # phantom: Player joins but is invisible to others, chat hidden
              shadowban-method: "timeout"

            discord:
              # Enable the embedded Discord bot
              enabled: false
              # Discord bot token (from Discord Developer Portal)
              bot-token: ""
              # Guild (server) ID where the bot operates
              guild-id: ""
              # Name of the slash command for linking (language-dependent)
              link-command-name: "link"
              # Role ID to assign when a player links their account (empty = disabled)
              linked-role-id: ""
              # Show online player count in bot status
              show-player-count: true

            # ============================================
            #  Item Management
            # ============================================
            items:
              # Custom despawn time for ground items in seconds (0 = use vanilla behavior)
              despawn-time: 300
              # Merge nearby identical items into stacks to reduce entity count
              stacking: true
              # Radius in blocks to search for stackable items
              stacking-radius: 3
              # Show floating hologram above items with name and countdown
              show-hologram: true
              # Global sweep interval in seconds (0 = disabled, only individual timers)
              sweep-interval: 0
              # Seconds before sweep to broadcast warning
              sweep-warning: 30
              # Items that never despawn (supports modded items, e.g., "create:brass_ingot")
              whitelist:
                - "minecraft:netherite_sword"
                - "minecraft:netherite_pickaxe"
                - "minecraft:netherite_axe"
                - "minecraft:netherite_shovel"
                - "minecraft:netherite_hoe"
                - "minecraft:netherite_helmet"
                - "minecraft:netherite_chestplate"
                - "minecraft:netherite_leggings"
                - "minecraft:netherite_boots"
                - "minecraft:elytra"
                - "minecraft:shulker_box"
                - "minecraft:totem_of_undying"
            """;

    static final String COMMANDS_YML = """
            # ============================================
            #  MKT Essentials - Commands
            # ============================================
            # Enable or disable command groups.
            # Set to false to completely disable a command.
            # Changes require server restart.

            # ============================================
            #  Teleportation Commands
            # ============================================
            teleportation:
              home: true      # /home, /sethome, /delhome, /homes
              warp: true      # /warp, /setwarp, /delwarp, /warps
              spawn: true     # /spawn
              back: true      # /back
              top: true       # /top
              rtp: true       # /rtp (random teleport)
              tpa: true       # /tpa, /tpahere, /tpaccept, /tpdeny

            # ============================================
            #  Admin Commands
            # ============================================
            admin:
              heal: true      # /heal [player]
              feed: true      # /feed [player]
              fly: true       # /fly [player]
              god: true       # /god [player]
              vanish: true    # /vanish
              speed: true     # /speed fly|walk <value> [player]
              clearinv: true  # /clearinv [player]
              tpall: true     # /tpall
              invsee: true    # /invsee <player>
              enderchest: true # /enderchest <player>
              backup: true    # /invbackup save|list|restore|delete
              clearitems: true # /clearitems [radius]

            # ============================================
            #  Moderation Commands
            # ============================================
            moderation:
              kick: true      # /kick <player> [reason]
              ban: true       # /ban, /tempban, /unban
              mute: true      # /mute, /unmute
              shadowban: true  # /shadowban, /unshadowban, /shadowbanlist

            # ============================================
            #  Utility Commands
            # ============================================
            utility:
              repair: true    # /repair
              enchant: true   # /enchant <enchantment> <level>
              kit: true       # /kit, /createkit, /deletekit
              nick: true      # /nick [nickname]
              msg: true       # /msg, /reply
              shortcuts: true # /gm, /gmc, /gms, /gma, /gmsp, /tp, /tphere, /tppos, /i, /more, /skull, /near, /seen, /sudo
            """;

    static final String MESSAGES_YML = """
            # ============================================
            #  MKT Essentials - Messages & Chat
            # ============================================
            # Configure chat formatting, join/quit messages,
            # and automated broadcasts.

            # ============================================
            #  Chat Format
            # ============================================
            chat:
              # Chat format template. Available placeholders:
              # %mktessentials:dot% - recording/streaming indicator
              # %mktessentials:prefix% - LuckPerms prefix
              # %mktessentials:name% - player display name
              # %mktessentials:suffix% - LuckPerms suffix
              # {message} - the chat message content
              format: "%mktessentials:dot%%mktessentials:prefix%%mktessentials:name%%mktessentials:suffix%&8: &f{message}"
              # Per-group chat formats (LuckPerms primary group name → format)
              # If a player's group is listed here, this format is used instead of the default above.
              # Same placeholders available as in the default format.
              group-formats:
                # admin: "&c[Admin] &f%mktessentials:name%&8: &f{message}"
                # vip: "&6[VIP] &f%mktessentials:name%&8: &f{message}"

            # ============================================
            #  Join & Quit Messages
            # ============================================
            join-quit:
              # Enable custom join/quit messages
              enabled: true
              # Available: %mktessentials:full_name/safe%
              join-message: "&8[&a+&8] &7%mktessentials:full_name/safe% joined the game."
              quit-message: "&8[&c-&8] &7%mktessentials:full_name/safe% left the game."

            # ============================================
            #  Automated Broadcasts
            # ============================================
            broadcast:
              # Interval between broadcasts in seconds
              interval: 300
              # Prefix prepended to each broadcast message
              prefix: "&8[&bINFO&8] &r"
              # Order: "random" or "sequential"
              order: "random"
              # List of messages to broadcast
              messages:
                - "&7Welcome to our server!"
                - "&7Join our Discord: &b/discord"
                - "&7Use &6/rtp &7to start your adventure!"
            """;

    static final String LANG_EN_US = """
            # MKT Essentials - English (en_us)
            general:
              prefix: "&8[&6MKT&8] &r"
              no-permission: "&cYou don't have permission to do that."
              player-not-found: "&cPlayer '{player}' not found."
              player-only: "&cThis command can only be used by players."
              reload-success: "&aConfiguration reloaded successfully."
              reload-failed: "&cFailed to reload configuration. Check console for errors."

            teleportation:
              delay: "&7Teleporting in &6{seconds}&7 seconds... Don't move!"
              cancelled: "&cTeleportation cancelled."
              cooldown: "&cYou must wait &6{seconds}&c seconds before teleporting again."
              teleported: "&aTeleported successfully!"

            homes:
              set: "&aHome '&6{name}&a' has been set."
              deleted: "&aHome '&6{name}&a' has been deleted."
              not-found: "&cHome '&6{name}&c' not found."
              limit-reached: "&cYou have reached your home limit ({max})."
              list: "&7Your homes: &6{homes}"
              none: "&7You have no homes set."

            warps:
              set: "&aWarp '&6{name}&a' has been set."
              deleted: "&aWarp '&6{name}&a' has been deleted."
              not-found: "&cWarp '&6{name}&c' not found."
              list: "&7Available warps: &6{warps}"
              none: "&7No warps available."

            tpa:
              sent: "&aTeleport request sent to &6{player}&a."
              received: "&6{player} &7has requested to teleport {direction}."
              accepted: "&aTeleport request accepted."
              denied: "&cTeleport request denied."
              expired: "&cTeleport request has expired."
              no-request: "&cNo pending teleport requests found."
              no-request-from: "&cNo pending request from that player."
              to-you: "to you"
              you-to-them: "you to them"
              self: "&cYou cannot teleport to yourself!"

            admin:
              healed: "&aHealed &6{player}&a."
              fed: "&aFed &6{player}&a."
              fly-enabled: "&7Flight &aenabled &7for &6{player}&7."
              fly-disabled: "&7Flight &cdisabled &7for &6{player}&7."
              god-enabled: "&7God mode &aenabled &7for &6{player}&7."
              god-disabled: "&7God mode &cdisabled &7for &6{player}&7."
              vanish-enabled: "&7Vanish &aenabled&7. You are now hidden."
              vanish-disabled: "&7Vanish &cdisabled&7. You are now visible."
              speed-fly: "&7Flying speed set to &6{value} &7for &6{player}&7."
              speed-walk: "&7Walking speed set to &6{value} &7for &6{player}&7."
              clearinv: "&7Cleared inventory of &6{player}&7."
              tpall: "&aTeleported &6{count} &aplayers to your location."

            moderation:
              kicked: "&7Kicked &6{player}&7. Reason: &f{reason}"
              banned: "&7Banned &6{player}&7. Reason: &f{reason}"
              temp-banned: "&7Temporarily banned &6{player} &7for &e{duration}&7. Reason: &f{reason}"
              unbanned: "&aUnbanned &6{player}&a."
              not-banned: "&cPlayer is not banned."
              muted: "&7Muted &6{player} &7{duration}."
              unmuted: "&aUnmuted &6{player}&a."
              muted-notify: "&cYou have been muted {duration}."
              already-muted: "&cYou are muted for {remaining}."
              permanently-muted: "&cYou are permanently muted."
              shadowbanned: "&7Shadowbanned &6{player}&7. Method: &e{method}&7. Reason: &f{reason}"
              unshadowbanned: "&aRemoved shadowban from &6{player}&a."
              not-shadowbanned: "&cPlayer is not shadowbanned."
              shadowban-list-header: "&7--- Shadowbanned Players ---"
              shadowban-list-entry: "&7- &6{player} &7({method}) &8[{reason}]"
              shadowban-list-empty: "&7No shadowbanned players."

            utility:
              repair-success: "&aItem repaired successfully."
              repair-empty-hand: "&cYou must hold an item to repair."
              repair-not-damageable: "&cThis item cannot be repaired."
              enchant-success: "&aApplied &6{enchantment} &alevel &6{level} &ato your item."
              enchant-empty-hand: "&cYou must hold an item to enchant."
              enchant-invalid: "&cInvalid enchantment."

            afk:
              now-afk: "&7{player} is now AFK."
              no-longer-afk: "&7{player} is no longer AFK."

            rtp:
              searching: "&7Searching for a safe location..."
              failed: "&cCould not find a safe location after 50 attempts. Please try again."

            back:
              no-location: "&cNo back location found!"

            auth:
              welcome-link-required: "&7You must link your Discord account to play on this server."
              welcome-register: "&7Please register with &6/register <password> <password>"
              welcome-login: "&7Please login with &6/login <password>"
              link-code: "&7Your link code: &b&l{code}"
              link-instruction: "&7Use &b/{command} {code} &7on our Discord server to link your account."
              link-success: "&aYour Discord account has been linked successfully!"
              register-success: "&aRegistration successful! You are now logged in."
              login-success: "&aLogin successful! Welcome back."
              login-failed: "&cWrong password! Attempt {attempts}/{max}."
              already-registered: "&cYou are already registered."
              already-linked: "&cYour account is already linked to a Discord account."
              passwords-dont-match: "&cPasswords do not match."
              frozen-reminder: "&7Please authenticate to continue playing."
              kicked-timeout: "&cYou were kicked for not authenticating within {seconds} seconds."
              newbie-protection: "&aYou have newbie protection for &6{minutes} &aminutes."
              password-changed: "&aPassword changed successfully."
              wrong-old-password: "&cThe old password is incorrect."
              must-link-first: "&cYou must link your Discord account before registering."
              not-registered: "&cYou are not registered. Use &6/register &cto create an account."
              discord-info: "&7Linked Discord ID: &b{discord_id}"
              no-discord-linked: "&7No Discord account linked."
              unlink-success: "&aDiscord account unlinked successfully."
              code-invalid: "&cInvalid or expired link code."
              code-expired: "&cThis link code has expired. Generate a new one with /link."
              discord-already-linked: "&cThis Discord account is already linked to another player."
              password-too-short: "&cPassword must be at least 4 characters."
              password-too-long: "&cPassword must be at most 64 characters."
              admin-reset: "&aAccount for &6{player} &ahas been reset."
              admin-unlink: "&aDiscord unlinked for &6{player}&a."
              admin-info-header: "&7--- Account Info for &6{player} &7---"
              admin-info-uuid: "&7UUID: &f{uuid}"
              admin-info-discord: "&7Discord: &f{discord}"
              admin-info-registered: "&7Registered: &f{date}"
              admin-info-last-login: "&7Last login: &f{date}"
              admin-info-last-ip: "&7Last IP: &f{ip}"
              admin-no-account: "&cNo account found for that player."

            shortcuts:
              gamemode-self: "&7Gamemode set to &6{mode}&7."
              gamemode-other: "&7Set &6{player}&7's gamemode to &6{mode}&7."
              tp-to: "&7Teleported to &6{player}&7."
              tp-here: "&7Teleported &6{player} &7to you."
              tp-pos: "&7Teleported to &6{x}, {y}, {z}&7."
              invalid-item: "&cInvalid item ID: &6{item}"
              item-not-found: "&cItem not found: &6{item}"
              gave-item: "&7Gave &6{amount}x {item}&7."
              must-hold-item: "&cYou must be holding an item!"
              more-success: "&7Stack size set to &6{amount}&7."
              skull-given: "&7Gave skull of &6{player}&7."
              near-none: "&7No players within &6{radius} &7blocks."
              near-found: "&7Nearby (&6{count}&7): {list}"
              seen-online: "&6{player} &7is currently &aonline&7."
              seen-never: "&cPlayer '&6{player}&c' has never joined this server."
              seen-ago: "&6{player} &7was last seen &e{time} ago&7."
              seen-unknown: "&6{player} &7has played before but last seen time is unknown."
              sudo-executed: "&7Forced &6{player} &7to execute: &f/{command}"

            items:
              sweep-warning: "&e\u26A0 &7Ground items will be cleared in &6{seconds} &7seconds!"
              sweep-cleared: "&7Cleared &6{count} &7ground items."
              clearitems-success: "&aCleared &6{count} &aitems from the ground."
              clearitems-radius: "&aCleared &6{count} &aitems within &6{radius} &ablocks."
              clearitems-none: "&7No items to clear."
            """;

    static final String LANG_PL_PL = """
            # MKT Essentials - Polski (pl_pl)
            general:
              prefix: "&8[&6MKT&8] &r"
              no-permission: "&cNie masz uprawnien do wykonania tej czynnosci."
              player-not-found: "&cGracz '{player}' nie zostal znaleziony."
              player-only: "&cTa komenda moze byc uzyta tylko przez graczy."
              reload-success: "&aKonfiguracja zostala pomyslnie przeladowana."
              reload-failed: "&cNie udalo sie przeladowac konfiguracji. Sprawdz konsole."

            teleportation:
              delay: "&7Teleportacja za &6{seconds}&7 sekund... Nie ruszaj sie!"
              cancelled: "&cTeleportacja anulowana."
              cooldown: "&cMusisz poczekac &6{seconds}&c sekund przed kolejna teleportacja."
              teleported: "&aTeleportacja udana!"

            homes:
              set: "&aDom '&6{name}&a' zostal ustawiony."
              deleted: "&aDom '&6{name}&a' zostal usuniety."
              not-found: "&cDom '&6{name}&c' nie zostal znaleziony."
              limit-reached: "&cOsiagnieto limit domow ({max})."
              list: "&7Twoje domy: &6{homes}"
              none: "&7Nie masz ustawionych domow."

            warps:
              set: "&aWarp '&6{name}&a' zostal ustawiony."
              deleted: "&aWarp '&6{name}&a' zostal usuniety."
              not-found: "&cWarp '&6{name}&c' nie zostal znaleziony."
              list: "&7Dostepne warpy: &6{warps}"
              none: "&7Brak dostepnych warpow."

            tpa:
              sent: "&aProba teleportacji wyslana do &6{player}&a."
              received: "&6{player} &7chce sie teleportowac {direction}."
              accepted: "&aProba teleportacji zaakceptowana."
              denied: "&cProba teleportacji odrzucona."
              expired: "&cProba teleportacji wygasla."
              no-request: "&cBrak oczekujacych prob teleportacji."
              no-request-from: "&cBrak oczekujacej prosby od tego gracza."
              to-you: "do ciebie"
              you-to-them: "ciebie do niego"
              self: "&cNie mozesz teleportowac sie do siebie!"

            admin:
              healed: "&aUleczono &6{player}&a."
              fed: "&aNakarmiono &6{player}&a."
              fly-enabled: "&7Latanie &awlaczone &7dla &6{player}&7."
              fly-disabled: "&7Latanie &cwylaczone &7dla &6{player}&7."
              god-enabled: "&7Tryb boga &awlaczony &7dla &6{player}&7."
              god-disabled: "&7Tryb boga &cwylaczony &7dla &6{player}&7."
              vanish-enabled: "&7Vanish &awlaczony&7. Jestes teraz niewidzialny."
              vanish-disabled: "&7Vanish &cwylaczony&7. Jestes teraz widoczny."
              speed-fly: "&7Predkosc latania ustawiona na &6{value} &7dla &6{player}&7."
              speed-walk: "&7Predkosc chodzenia ustawiona na &6{value} &7dla &6{player}&7."
              clearinv: "&7Wyczyszczono ekwipunek &6{player}&7."
              tpall: "&aTeleportowano &6{count} &agraczy do twojej lokalizacji."

            moderation:
              kicked: "&7Wyrzucono &6{player}&7. Powod: &f{reason}"
              banned: "&7Zbanowano &6{player}&7. Powod: &f{reason}"
              temp-banned: "&7Tymczasowo zbanowano &6{player} &7na &e{duration}&7. Powod: &f{reason}"
              unbanned: "&aOdbanowano &6{player}&a."
              not-banned: "&cGracz nie jest zbanowany."
              muted: "&7Wyciszono &6{player} &7{duration}."
              unmuted: "&aOdciszono &6{player}&a."
              muted-notify: "&cZostales wyciszony {duration}."
              already-muted: "&cJestes wyciszony jeszcze przez {remaining}."
              permanently-muted: "&cJestes permanentnie wyciszony."
              shadowbanned: "&7Shadowban nalozony na &6{player}&7. Metoda: &e{method}&7. Powod: &f{reason}"
              unshadowbanned: "&aUsunieto shadowban z &6{player}&a."
              not-shadowbanned: "&cGracz nie jest shadowbanowany."
              shadowban-list-header: "&7--- Shadowbanowani gracze ---"
              shadowban-list-entry: "&7- &6{player} &7({method}) &8[{reason}]"
              shadowban-list-empty: "&7Brak shadowbanowanych graczy."

            utility:
              repair-success: "&aPrzedmiot naprawiony pomyslnie."
              repair-empty-hand: "&cMusisz trzymac przedmiot, aby go naprawic."
              repair-not-damageable: "&cTen przedmiot nie moze zostac naprawiony."
              enchant-success: "&aDodano &6{enchantment} &apoziom &6{level} &ana twoj przedmiot."
              enchant-empty-hand: "&cMusisz trzymac przedmiot, aby go zaczarowac."
              enchant-invalid: "&cNieprawidlowy czar."

            afk:
              now-afk: "&7{player} jest teraz AFK."
              no-longer-afk: "&7{player} nie jest juz AFK."

            rtp:
              searching: "&7Szukanie bezpiecznej lokalizacji..."
              failed: "&cNie znaleziono bezpiecznej lokalizacji po 50 probach. Sprobuj ponownie."

            back:
              no-location: "&cNie znaleziono poprzedniej lokalizacji!"

            auth:
              welcome-link-required: "&7Musisz polaczyc swoje konto Discord, aby grac na tym serwerze."
              welcome-register: "&7Zarejestruj sie komenda &6/register <haslo> <haslo>"
              welcome-login: "&7Zaloguj sie komenda &6/login <haslo>"
              link-code: "&7Twoj kod polaczenia: &b&l{code}"
              link-instruction: "&7Uzyj &b/{command} {code} &7na naszym Discordzie, aby polaczyc konto."
              link-success: "&aTwoje konto Discord zostalo pomyslnie polaczone!"
              register-success: "&aRejestracja udana! Jestes teraz zalogowany."
              login-success: "&aLogowanie udane! Witaj ponownie."
              login-failed: "&cBledne haslo! Proba {attempts}/{max}."
              already-registered: "&cJestes juz zarejestrowany."
              already-linked: "&cTwoje konto jest juz polaczone z kontem Discord."
              passwords-dont-match: "&cHasla nie sa identyczne."
              frozen-reminder: "&7Prosze sie uwierzytelnic, aby kontynuowac gre."
              kicked-timeout: "&cZostales wyrzucony za brak uwierzytelnienia w ciagu {seconds} sekund."
              newbie-protection: "&aMasz ochrone dla nowych graczy przez &6{minutes} &aminut."
              password-changed: "&aHaslo zostalo zmienione pomyslnie."
              wrong-old-password: "&cStare haslo jest nieprawidlowe."
              must-link-first: "&cMusisz najpierw polaczyc konto Discord przed rejestracja."
              not-registered: "&cNie jestes zarejestrowany. Uzyj &6/register &caby utworzyc konto."
              discord-info: "&7Polaczone Discord ID: &b{discord_id}"
              no-discord-linked: "&7Brak polaczonego konta Discord."
              unlink-success: "&aKonto Discord zostalo odlaczone pomyslnie."
              code-invalid: "&cNieprawidlowy lub wygasly kod polaczenia."
              code-expired: "&cTen kod polaczenia wygasl. Wygeneruj nowy komenda /link."
              discord-already-linked: "&cTo konto Discord jest juz polaczone z innym graczem."
              password-too-short: "&cHaslo musi miec co najmniej 4 znaki."
              password-too-long: "&cHaslo moze miec maksymalnie 64 znaki."
              admin-reset: "&aKonto gracza &6{player} &azostalo zresetowane."
              admin-unlink: "&aDiscord odlaczony dla &6{player}&a."
              admin-info-header: "&7--- Informacje o koncie &6{player} &7---"
              admin-info-uuid: "&7UUID: &f{uuid}"
              admin-info-discord: "&7Discord: &f{discord}"
              admin-info-registered: "&7Zarejestrowano: &f{date}"
              admin-info-last-login: "&7Ostatnie logowanie: &f{date}"
              admin-info-last-ip: "&7Ostatnie IP: &f{ip}"
              admin-no-account: "&cNie znaleziono konta dla tego gracza."

            shortcuts:
              gamemode-self: "&7Tryb gry ustawiony na &6{mode}&7."
              gamemode-other: "&7Ustawiono tryb gry &6{player}&7 na &6{mode}&7."
              tp-to: "&7Teleportowano do &6{player}&7."
              tp-here: "&7Teleportowano &6{player} &7do ciebie."
              tp-pos: "&7Teleportowano na &6{x}, {y}, {z}&7."
              invalid-item: "&cNieprawidlowe ID przedmiotu: &6{item}"
              item-not-found: "&cNie znaleziono przedmiotu: &6{item}"
              gave-item: "&7Dano &6{amount}x {item}&7."
              must-hold-item: "&cMusisz trzymac przedmiot!"
              more-success: "&7Rozmiar stosu ustawiony na &6{amount}&7."
              skull-given: "&7Dano glowe &6{player}&7."
              near-none: "&7Brak graczy w zasiegu &6{radius} &7blokow."
              near-found: "&7W poblizu (&6{count}&7): {list}"
              seen-online: "&6{player} &7jest aktualnie &aonline&7."
              seen-never: "&cGracz '&6{player}&c' nigdy nie dolaczyl do tego serwera."
              seen-ago: "&6{player} &7byl widziany &e{time} temu&7."
              seen-unknown: "&6{player} &7gral wczesniej ale czas ostatniej wizyty jest nieznany."
              sudo-executed: "&7Wymuszono na &6{player} &7wykonanie: &f/{command}"

            items:
              sweep-warning: "&e\u26A0 &7Przedmioty na ziemi zostana usuniete za &6{seconds} &7sekund!"
              sweep-cleared: "&7Usunieto &6{count} &7przedmiotow z ziemi."
              clearitems-success: "&aUsunieto &6{count} &aprzedmiotow z ziemi."
              clearitems-radius: "&aUsunieto &6{count} &aprzedmiotow w zasiegu &6{radius} &ablokow."
              clearitems-none: "&7Brak przedmiotow do usuniecia."
            """;
}
