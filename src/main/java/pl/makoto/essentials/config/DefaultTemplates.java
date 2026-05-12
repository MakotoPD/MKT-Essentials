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

            # ============================================
            #  Moderation Commands
            # ============================================
            moderation:
              kick: true      # /kick <player> [reason]
              ban: true       # /ban, /tempban, /unban
              mute: true      # /mute, /unmute

            # ============================================
            #  Utility Commands
            # ============================================
            utility:
              repair: true    # /repair
              enchant: true   # /enchant <enchantment> <level>
              kit: true       # /kit, /createkit, /deletekit
              nick: true      # /nick [nickname]
              msg: true       # /msg, /reply
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
            """;
}
