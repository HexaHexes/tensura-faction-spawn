package org.bitwisemadness.tensurafactionspawn.waypoints;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.bitwisemadness.tensurafactionspawn.TensuraFactionSpawn;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalEntry;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalRegistry;

/**
 * Server half of the waypoint feature: on login, send every located capital
 * to the joining player. Positions come from the per-world
 * {@link CapitalRegistry} cache (populated by {@code CapitalLocator}), so the
 * O(radius^2) structure search never runs per login (spike #38, Option B).
 */
public final class CapitalWaypointSync {

    private CapitalWaypointSync() {
    }

    /** Login hook: send the located capitals to the joining player. */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var all = CapitalRegistry.get(player.getServer().overworld().getDataStorage()).all();
        if (all.isEmpty()) {
            return;
        }
        List<CapitalWaypointPayload.Entry> entries = all.values().stream()
                .map(CapitalWaypointSync::toEntry)
                .toList();
        TensuraFactionSpawn.LOGGER.debug("Sending {} capital waypoint(s) to {}",
                entries.size(), player.getGameProfile().getName());
        PacketDistributor.sendToPlayer(player, new CapitalWaypointPayload(entries));
    }

    private static CapitalWaypointPayload.Entry toEntry(CapitalEntry entry) {
        ResourceLocation tag = entry.tagLocation();
        String faction = tag.getPath().startsWith("capital/")
                ? tag.getPath().substring("capital/".length())
                : tag.getPath();
        String name = Character.toUpperCase(faction.charAt(0)) + faction.substring(1) + " Capital";
        return new CapitalWaypointPayload.Entry(
                entry.dimension().location().toString(),
                entry.position().getX(),
                entry.position().getY(),
                entry.position().getZ(),
                name);
    }
}
