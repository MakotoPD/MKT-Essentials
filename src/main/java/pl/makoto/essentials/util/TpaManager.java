package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaManager {
    private static final Map<UUID, TpaRequest> requests = new HashMap<>();

    public static void addRequest(ServerPlayer sender, ServerPlayer target, boolean here) {
        requests.put(target.getUUID(), new TpaRequest(sender.getUUID(), here, System.currentTimeMillis()));
    }

    public static TpaRequest getRequest(UUID targetUuid) {
        TpaRequest req = requests.get(targetUuid);
        if (req != null && System.currentTimeMillis() - req.timestamp > pl.makoto.essentials.Config.TPA_TIMEOUT.get() * 1000L) {
            requests.remove(targetUuid);
            return null;
        }
        return req;
    }

    public static void removeRequest(UUID targetUuid) {
        requests.remove(targetUuid);
    }

    public static record TpaRequest(UUID senderUuid, boolean here, long timestamp) {}
}
