package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TpaManager {
    private static final Map<UUID, List<TpaRequest>> requests = new HashMap<>();

    public static void addRequest(ServerPlayer sender, ServerPlayer target, boolean here) {
        requests.computeIfAbsent(target.getUUID(), k -> new ArrayList<>())
                .add(new TpaRequest(sender.getUUID(), here, System.currentTimeMillis()));
    }

    public static TpaRequest getLatestRequest(UUID targetUuid) {
        List<TpaRequest> list = getValidRequests(targetUuid);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    public static TpaRequest getRequestFrom(UUID targetUuid, UUID senderUuid) {
        List<TpaRequest> list = getValidRequests(targetUuid);
        return list.stream()
                .filter(r -> r.senderUuid().equals(senderUuid))
                .findFirst().orElse(null);
    }

    public static void removeRequest(UUID targetUuid, TpaRequest request) {
        List<TpaRequest> list = requests.get(targetUuid);
        if (list != null) {
            list.remove(request);
            if (list.isEmpty()) requests.remove(targetUuid);
        }
    }

    public static List<TpaRequest> getValidRequests(UUID targetUuid) {
        List<TpaRequest> list = requests.get(targetUuid);
        if (list == null) return List.of();
        long now = System.currentTimeMillis();
        long timeout = pl.makoto.essentials.config.Settings.getTpaTimeout() * 1000L;
        list.removeIf(r -> now - r.timestamp() > timeout);
        if (list.isEmpty()) requests.remove(targetUuid);
        return list;
    }

    public static void cleanupPlayer(UUID uuid) {
        requests.remove(uuid);
        requests.entrySet().removeIf(e -> {
            e.getValue().removeIf(r -> r.senderUuid().equals(uuid));
            return e.getValue().isEmpty();
        });
    }

    public static record TpaRequest(UUID senderUuid, boolean here, long timestamp) {}
}
