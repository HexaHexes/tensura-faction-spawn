package org.bitwisemadness.tensurafactionspawn;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.bitwisemadness.capitalapi.CapitalApi;
import org.bitwisemadness.capitalapi.CapitalSpawnEngine;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalLocator;
import org.bitwisemadness.tensurafactionspawn.command.FactionSpawnCommand;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRuleManager;
import org.bitwisemadness.tensurafactionspawn.spawn.FactionSpawnService;
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

    public TensuraFactionSpawn(IEventBus modBus) {
        // Server-side only: spawn rules come from the server datapack, and the
        // relocation itself is authoritative on the server.
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(CapitalLocator::onServerTick);

        // The engine half of the boundary contract (spec §5.1): the race
        // integration triggers arrival through this registration.
        CapitalApi.registerEngine(new CapitalSpawnEngine() {
            @Override
            public java.util.Optional<BlockPos> relocate(net.minecraft.server.level.ServerPlayer player) {
                return FactionSpawnService.relocate(player);
            }
        });

        LOGGER.info("Tensura Faction Spawn initialized");
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SpawnRuleManager());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        FactionSpawnCommand.register(event.getDispatcher());
    }
}
