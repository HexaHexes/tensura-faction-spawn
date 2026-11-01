package org.bitwisemadness.tensurafactionspawn;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalLocator;
import org.bitwisemadness.tensurafactionspawn.command.FactionSpawnCommand;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRuleManager;
import org.bitwisemadness.tensurafactionspawn.spawn.RaceSetHandler;
import org.slf4j.Logger;

/**
 * Places a player at their faction's capital when they pick a race or
 * reincarnate.
 *
 * <p>The relocation decision is taken from ManasCore's own {@code SET_RACE}
 * signal: the mutable {@code Changeable<Boolean>} it carries is {@code true}
 * only for a first pick / reincarnation and {@code false} for an evolution
 * (including Cross Evolution cross-evolutions), so evolutions never re-teleport
 * and reincarnations always do.</p>
 *
 * <p>Where the player lands is datapack-driven: race-group tags
 * ({@code tags/manascore_race/races/}) match the player's race, spawn rules
 * ({@code tensura_faction_spawn/spawn_rules/}) map a race-group tag to a
 * structure tag, and the nearest structure in that tag to the world spawn is
 * chosen.</p>
 */
@Mod(TensuraFactionSpawn.MOD_ID)
public final class TensuraFactionSpawn {

    public static final String MOD_ID = "tensura_faction_spawn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TensuraFactionSpawn(IEventBus modBus) {
        // ManasCore fires SET_RACE (an Architectury event) on both a first pick
        // and a reincarnation; the relocation flag it exposes is our trigger.
        RaceSetHandler.register();

        // Server-side only: spawn rules come from the server datapack, and the
        // relocation itself is authoritative on the server.
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(RaceSetHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(CapitalLocator::onServerTick);

        LOGGER.info("Tensura Faction Spawn initialized");
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SpawnRuleManager());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        FactionSpawnCommand.register(event.getDispatcher());
    }
}
