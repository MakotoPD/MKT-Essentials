package pl.makoto.essentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;

import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.commands.*;

import net.minecraft.server.MinecraftServer;
import pl.makoto.essentials.util.MKTPlaceholders;
import pl.makoto.essentials.util.TABHook;

@Mod(MKTEssentials.MODID)
public class MKTEssentials {
    public static final String MODID = "mktessentials";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer server;

    public MKTEssentials(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        // Register only once to the server event bus
        NeoForge.EVENT_BUS.register(this);
    }

    public static MinecraftServer getServer() {
        return server;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        LOGGER.info("Registering MKT Essentials commands...");
        HomeCommands.register(dispatcher);
        WarpCommands.register(dispatcher);
        MiscTeleportCommands.register(dispatcher);
        TpaCommands.register(dispatcher);
        MessagingCommands.register(dispatcher);
        AdminCommands.register(dispatcher);
        KitCommands.register(dispatcher);
        MKTCommand.register(dispatcher);
        MuteCommands.register(dispatcher);
        MiscCommands.register(dispatcher);
        TimeWeatherCommands.register(dispatcher);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("MKT Essentials starting...");
        server = event.getServer();
        DataManager.init(server);
        MKTPlaceholders.register();
        TABHook.init();
    }
}
