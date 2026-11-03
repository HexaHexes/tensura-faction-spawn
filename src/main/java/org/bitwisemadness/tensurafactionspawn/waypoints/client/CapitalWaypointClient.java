package org.bitwisemadness.tensurafactionspawn.waypoints.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.bitwisemadness.tensurafactionspawn.TensuraFactionSpawn;
import org.bitwisemadness.tensurafactionspawn.waypoints.CapitalWaypointConfig;
import org.bitwisemadness.tensurafactionspawn.waypoints.CapitalWaypointPayload;

/**
 * Client half of the waypoint feature: turns the login-synced capital list
 * into Xaero waypoints, once per capital per world.
 *
 * <p>Deletion respect: an offered capital id is recorded in a per-world offers
 * file under {@code config/tensura_faction_spawn/waypoint_offers/} at offer
 * time; Xaero's own persistence keeps the visible waypoint, and a player
 * deletion plus the recorded offer means never re-offering. Capitals in other
 * dimensions stay unmarked until the client visits their dimension, so
 * cross-dimension capitals offer where they are actually reachable.</p>
 *
 * <p>Xaero is optional: without it this class never touches anything beyond
 * the offers file, and every Xaero call is wrapped so an upstream change
 * degrades to a logged no-op.</p>
 */
public final class CapitalWaypointClient {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile List<CapitalWaypointPayload.Entry> pending;
    private static ResourceLocation lastDimension;

    private CapitalWaypointClient() {
    }

    /** Client tick: apply the pending login sync, and re-apply on dimension change. */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!CapitalWaypointConfig.enabled()) {
            pending = null;
            lastDimension = null;
            return;
        }
        ResourceLocation dimension = mc.level.dimension().location();
        boolean dimensionChanged = !dimension.equals(lastDimension);
        List<CapitalWaypointPayload.Entry> entries = pending;
        if (entries != null) {
            pending = null;
            apply(mc, dimension, entries);
        } else if (dimensionChanged) {
            apply(mc, dimension, List.of());
        }
        lastDimension = dimension;
    }

    /** Queues a login-time capital list for the next tick. */
    public static void accept(List<CapitalWaypointPayload.Entry> entries) {
        pending = entries;
    }

    private static void apply(Minecraft mc, ResourceLocation dimension,
                              List<CapitalWaypointPayload.Entry> synced) {
        String worldKey = worldKey(mc);
        Set<String> offered = loadOffers(worldKey);
        List<CapitalWaypointPayload.Entry> relevant = synced.stream()
                .filter(e -> ResourceLocation.parse(e.dimension()).equals(dimension))
                .toList();
        boolean changed = false;
        for (CapitalWaypointPayload.Entry entry : relevant) {
            String capitalId = entry.dimension() + ":" + entry.x() + "," + entry.z();
            if (offered.contains(capitalId)) {
                continue;
            }
            if (xaeroPresent() && offerToXaero(entry)) {
                offered.add(capitalId);
                changed = true;
            }
        }
        if (changed) {
            saveOffers(worldKey, offered);
        }
    }

    private static boolean offerToXaero(CapitalWaypointPayload.Entry entry) {
        try {
            xaero.common.XaeroMinimapSession session = xaero.common.XaeroMinimapSession.getCurrentSession();
            if (session == null) {
                return false;
            }
            xaero.common.minimap.waypoints.WaypointsManager manager = session.getWaypointsManager();
            xaero.common.minimap.waypoints.WaypointWorld world = manager.getCurrentWorld();
            if (world == null) {
                return false;
            }
            xaero.common.minimap.waypoints.WaypointSet set = world.getCurrentSet();
            for (xaero.common.minimap.waypoints.Waypoint existing : set.getList()) {
                if (existing.getName().equals(entry.name())
                        && existing.getX() == entry.x() && existing.getZ() == entry.z()) {
                    return true;
                }
            }
            set.add(new xaero.common.minimap.waypoints.Waypoint(
                    entry.x(), entry.y(), entry.z(), entry.name(), "O", 0), true);
            TensuraFactionSpawn.LOGGER.info("Offered Xaero waypoint '{}' at {} ({})",
                    entry.name(), entry.x() + "," + entry.y() + "," + entry.z(), entry.dimension());
            return true;
        } catch (Throwable t) {
            TensuraFactionSpawn.LOGGER.warn("Xaero waypoint offering failed (degrading to no-op): {}", t.toString());
            return false;
        }
    }

    private static boolean xaeroPresent() {
        return ModList.get().isLoaded("xaerominimap");
    }

    private static String worldKey(Minecraft mc) {
        if (mc.getCurrentServer() != null) {
            return "mp-" + mc.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9.-]", "_");
        }
        if (mc.getSingleplayerServer() != null) {
            return "sp-" + mc.getSingleplayerServer().getWorldData().getLevelName()
                    .replaceAll("[^a-zA-Z0-9.-]", "_");
        }
        return "unknown";
    }

    private static Path offersFile(String worldKey) {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()
                .resolve("tensura_faction_spawn/waypoint_offers/" + worldKey + ".json");
    }

    private static Set<String> loadOffers(String worldKey) {
        Path file = offersFile(worldKey);
        if (!Files.exists(file)) {
            return new HashSet<>();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Set<String> offers = GSON.fromJson(reader, new TypeToken<Set<String>>() {
            }.getType());
            return offers != null ? new HashSet<>(offers) : new HashSet<>();
        } catch (IOException | RuntimeException ex) {
            TensuraFactionSpawn.LOGGER.warn("Could not read the waypoint offers file: {}", ex.toString());
            return new HashSet<>();
        }
    }

    private static void saveOffers(String worldKey, Set<String> offers) {
        Path file = offersFile(worldKey);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(offers, writer);
            }
        } catch (IOException | RuntimeException ex) {
            TensuraFactionSpawn.LOGGER.warn("Could not save the waypoint offers file: {}", ex.toString());
        }
    }
}
