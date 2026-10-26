package org.bitwisemadness.tensurafactionspawn.spawn;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.race.api.RaceEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.bitwisemadness.tensurafactionspawn.TensuraFactionSpawn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Listens for ManasCore's {@code SET_RACE} and schedules a faction-capital
 * relocation when the change is a first pick or a reincarnation.
 *
 * <p><strong>Why the relocation flag, not the evolution lineage.</strong>
 * {@code RaceStorage.setRace(instance, flagA, relocate, message)} wraps
 * {@code relocate} in a {@code Changeable<Boolean>} and passes it to
 * {@code SET_RACE}; after the event returns, Tensura teleports the player iff
 * that value is {@code true} and the owner is a {@link ServerPlayer}. Callers
 * set it deliberately: {@code ReincarnationMenu} passes {@code true} (first pick
 * and reincarnation), while {@code Races.evolveRace(...)} - the path used by
 * vanilla evolution and by Cross Evolution's cross-evolutions - passes
 * {@code false}. Mirroring the flag therefore classifies evolution vs.
 * reincarnation exactly as Tensura does, whatever the datapack/Cross Evolution
 * evolution graph looks like.</p>
 *
 * <p><strong>Why the relocation is deferred by a tick.</strong> Tensura performs
 * its own default relocation synchronously, immediately after this event
 * returns. Running ours in the same call would be overwritten, so the player is
 * queued and processed on the next server tick, once the race is stored and
 * Tensura's own hop has settled. The destination is read from live race data at
 * that point, so the event's race arguments are never needed.</p>
 */
public final class RaceSetHandler {

    /** Players awaiting relocation, keyed by UUID so a repeat set in the same tick cannot queue twice. */
    private static final Set<UUID> PENDING = new HashSet<>();

    private RaceSetHandler() {
    }

    /** Register on ManasCore's race-set event. Called from the mod constructor. */
    public static void register() {
        RaceEvents.SET_RACE.register((previousRace, entity, newRace, flag, relocate, message) -> {
            if (Boolean.TRUE.equals(relocate.get()) && entity instanceof ServerPlayer player) {
                PENDING.add(player.getUUID());
            }
            // Never veto: other listeners and Tensura's own handling must proceed.
            return EventResult.pass();
        });
    }

    /** Flush the queue on the next server tick. Called from the NeoForge tick event. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        List<UUID> batch = new ArrayList<>(PENDING);
        PENDING.clear();
        for (UUID id : batch) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                continue;
            }
            FactionSpawnService.relocate(player).ifPresent(pos ->
                    TensuraFactionSpawn.LOGGER.info(
                            "Relocated {} to their faction capital at {}", player.getGameProfile().getName(), pos));
        }
    }
}
