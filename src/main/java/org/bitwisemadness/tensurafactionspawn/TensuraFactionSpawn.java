package org.bitwisemadness.tensurafactionspawn;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.bitwisemadness.capitalapi.CapitalApi;
import org.bitwisemadness.capitalapi.CapitalSpawnEngine;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalLocator;
import org.bitwisemadness.tensurafactionspawn.command.FactionSpawnCommand;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRuleManager;
import org.bitwisemadness.tensurafactionspawn.datagen.FactionSpawnContent;
import org.bitwisemadness.tensurafactionspawn.datagen.JsonFilesProvider;
import org.bitwisemadness.tensurafactionspawn.spawn.FactionSpawnService;
import org.bitwisemadness.tensurafactionspawn.waypoints.CapitalWaypointConfig;
import org.bitwisemadness.tensurafactionspawn.waypoints.CapitalWaypointNet;
import org.bitwisemadness.tensurafactionspawn.waypoints.CapitalWaypointSync;
import org.bitwisemadness.tensurafactionspawn.waypoints.client.CapitalWaypointClient;
import org.slf4j.Logger;

/**
 * Places a player at their faction's capital when they pick a race or
 * reincarnate.
 *
 * <p>The relocation trigger arrives from ManasCore's own {@code SET_RACE}
 * signal, read by the race integration ({@code capital-tensura-reincarnated})
 * and handed here through {@code capital-api}: the mutable
 * {@code Changeable<Boolean>} it carries is {@code true} only for a first pick
 * / reincarnation and {@code false} for an evolution, so evolutions never
 * re-teleport and reincarnations always do.</p>
 *
 * <p>Where the player lands is datapack-driven: the integration resolves the
 * player's faction from the race-group tags
 * ({@code tags/manascore_race/races/}), spawn rules
 * ({@code tensura_faction_spawn/spawn_rules/}) map a faction to a structure
 * tag, and the nearest structure in that tag to the world spawn is chosen.</p>
 */
@Mod(TensuraFactionSpawn.MOD_ID)
public final class TensuraFactionSpawn {

    public static final String MOD_ID = "tensura_faction_spawn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TensuraFactionSpawn(IEventBus modBus, ModContainer container) {
        // Server-side only: spawn rules come from the server datapack, and the
        // relocation itself is authoritative on the server.
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(CapitalLocator::onServerTick);
        NeoForge.EVENT_BUS.addListener(CapitalWaypointSync::onPlayerLoggedIn);
        modBus.addListener(this::gatherData);

        // The engine half of the boundary contract (spec §5.1): the race
        // integration triggers arrival through this registration.
        CapitalApi.registerEngine(new CapitalSpawnEngine() {
            @Override
            public java.util.Optional<BlockPos> relocate(net.minecraft.server.level.ServerPlayer player) {
                return FactionSpawnService.relocate(player);
            }
        });

        // The capital-waypoint feature (#38): the payload registration is
        // common-side; the Xaero registration is client-only and dist-gated.
        modBus.addListener(CapitalWaypointNet::register);
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.addListener(CapitalWaypointClient::onClientTick);
        }
        container.registerConfig(ModConfig.Type.CLIENT, CapitalWaypointConfig.SPEC);

        LOGGER.info("Tensura Faction Spawn initialized");
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SpawnRuleManager());
    }

    /**
     * Feeds the shipped datapack - the spawn rules and capital structure tags,
     * ported from the hand-authored originals into the
     * {@link FactionSpawnContent} tables - into datagen (issue #34).
     */
    private void gatherData(GatherDataEvent event) {
        if (!event.includeServer()) {
            return;
        }
        event.addProvider(new JsonFilesProvider("faction_spawn_datapack",
                event.getGenerator().getPackOutput(), FactionSpawnContent.files()));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        FactionSpawnCommand.register(event.getDispatcher());
    }
}
