package pl.makoto.essentials.auth;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

public final class DiscordBot extends ListenerAdapter {

    private static JDA jda;
    private static volatile boolean ready = false;
    private static final DiscordBot INSTANCE = new DiscordBot();

    private DiscordBot() {}

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    public static void initAsync() {
        String token = Settings.getDiscordBotToken();
        if (token == null || token.isBlank()) {
            MKTEssentials.LOGGER.warn("Discord bot token is empty — bot will not start.");
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS)
                        .addEventListeners(INSTANCE)
                        .build();
                jda.awaitReady();
                ready = true;
                MKTEssentials.LOGGER.info("Discord bot connected successfully.");

                // Register slash command on the configured guild
                registerSlashCommand();

                // Set initial status
                if (Settings.isDiscordShowPlayerCount()) {
                    updatePlayerCount(0);
                }
            } catch (Exception e) {
                MKTEssentials.LOGGER.error("Failed to start Discord bot", e);
                ready = false;
            }
        }, "MKT-Discord-Bot");
        thread.setDaemon(true);
        thread.start();
    }

    public static void shutdown() {
        if (jda != null) {
            try {
                jda.shutdown();
                MKTEssentials.LOGGER.info("Discord bot shut down.");
            } catch (Exception e) {
                MKTEssentials.LOGGER.error("Error shutting down Discord bot", e);
            }
            jda = null;
            ready = false;
        }
    }

    public static boolean isReady() {
        return ready && jda != null;
    }

    // ─── Role Management ───────────────────────────────────────────────────────

    public static void assignLinkedRole(String discordId) {
        if (!isReady()) return;
        String roleId = Settings.getDiscordLinkedRoleId();
        if (roleId == null || roleId.isBlank()) return;

        String guildId = Settings.getDiscordGuildId();
        if (guildId == null || guildId.isBlank()) return;

        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return;

            Role role = guild.getRoleById(roleId);
            if (role == null) return;

            guild.retrieveMemberById(discordId).queue(member -> {
                if (member != null) {
                    guild.addRoleToMember(member, role).queue(
                            success -> MKTEssentials.LOGGER.debug("Assigned linked role to {}", discordId),
                            error -> MKTEssentials.LOGGER.warn("Failed to assign linked role to {}", discordId)
                    );
                }
            }, error -> MKTEssentials.LOGGER.warn("Could not find member {} for role assignment", discordId));
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("Error assigning linked role to {}", discordId, e);
        }
    }

    public static void removeLinkedRole(String discordId) {
        if (!isReady()) return;
        String roleId = Settings.getDiscordLinkedRoleId();
        if (roleId == null || roleId.isBlank()) return;

        String guildId = Settings.getDiscordGuildId();
        if (guildId == null || guildId.isBlank()) return;

        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return;

            Role role = guild.getRoleById(roleId);
            if (role == null) return;

            guild.retrieveMemberById(discordId).queue(member -> {
                if (member != null) {
                    guild.removeRoleFromMember(member, role).queue(
                            success -> MKTEssentials.LOGGER.debug("Removed linked role from {}", discordId),
                            error -> MKTEssentials.LOGGER.warn("Failed to remove linked role from {}", discordId)
                    );
                }
            }, error -> MKTEssentials.LOGGER.warn("Could not find member {} for role removal", discordId));
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("Error removing linked role from {}", discordId, e);
        }
    }

    // ─── Status ────────────────────────────────────────────────────────────────

    public static void updatePlayerCount(int count) {
        if (!isReady()) return;
        if (!Settings.isDiscordShowPlayerCount()) return;

        try {
            jda.getPresence().setActivity(Activity.playing(count + " player" + (count == 1 ? "" : "s") + " online"));
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("Failed to update Discord bot status", e);
        }
    }

    // ─── Slash Command Handling ────────────────────────────────────────────────

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = Settings.getDiscordLinkCommandName();
        if (!event.getName().equals(commandName)) return;

        OptionMapping codeOption = event.getOption("code");
        if (codeOption == null) {
            event.reply("Please provide a link code. Usage: `/" + commandName + " <code>`").setEphemeral(true).queue();
            return;
        }

        String code = codeOption.getAsString().trim();
        String discordId = event.getUser().getId();

        AuthManager.LinkResult result = AuthManager.completeLinking(code, discordId);

        switch (result) {
            case SUCCESS -> event.reply("\u2705 Your Minecraft account has been linked successfully!").setEphemeral(true).queue();
            case CODE_INVALID -> event.reply("\u274c Invalid or expired link code. Please generate a new one in-game with `/link`.").setEphemeral(true).queue();
            case CODE_EXPIRED -> event.reply("\u274c This code has expired. Please generate a new one in-game with `/link`.").setEphemeral(true).queue();
            case DISCORD_ALREADY_LINKED -> event.reply("\u274c Your Discord account is already linked to another Minecraft account.").setEphemeral(true).queue();
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private static void registerSlashCommand() {
        String guildId = Settings.getDiscordGuildId();
        if (guildId == null || guildId.isBlank()) {
            MKTEssentials.LOGGER.warn("Discord guild ID is empty — slash command not registered.");
            return;
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            MKTEssentials.LOGGER.warn("Discord guild {} not found — slash command not registered.", guildId);
            return;
        }

        String cmdName = Settings.getDiscordLinkCommandName();
        guild.upsertCommand(Commands.slash(cmdName, "Link your Minecraft account")
                .addOption(OptionType.STRING, "code", "The 6-digit link code from in-game", true)
        ).queue(
                success -> MKTEssentials.LOGGER.info("Discord slash command '{}' registered on guild {}.", cmdName, guildId),
                error -> MKTEssentials.LOGGER.error("Failed to register Discord slash command", error)
        );
    }
}
