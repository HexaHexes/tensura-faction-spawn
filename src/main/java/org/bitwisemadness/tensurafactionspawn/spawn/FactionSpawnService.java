package org.bitwisemadness.tensurafactionspawn.spawn;

import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.Races;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.bitwisemadness.tensurafactionspawn.TensuraFactionSpawn;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRule;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRuleManager;

import java.util.Optional;

/**
 * Chooses and applies a player's faction capital.
 *
 * <p>The capital is the nearest structure in the rule's structure tag to the
 * target level's shared spawn point - i.e. proximity to the original world spawn.
 * If none is found within the rule's {@code search_radius} and the rule allows
 * it, the search is widened across the whole dimension.</p>
 *
 * <p>Placement puts the player on the surface at the structure's anchor chunk
 * and, when the rule asks for it, sets that spot as their respawn point.</p>
 */
public final class FactionSpawnService {

    /**
     * Widened radius in chunks (~8192 blocks) used when nothing is found within the
     * rule's radius. The API searches ring by ring, so this is bounded rather than
     * truly whole-dimension - a much larger value would stall the server thread.
     * Still an order of magnitude wider than the default, which is what "expand the
     * search" needs in practice.
     */
    public static final int EXPANDED_SEARCH_RADIUS = 512;

    private FactionSpawnService() {
    }

    /**
     * Resolve the player's rule and move them to their faction capital.
     *
     * @return the anchor position they were moved to, or empty when there is no
     *         race, no matching rule, no such dimension, or no capital found
     */
    public static Optional<BlockPos> relocate(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return Optional.empty();
        }
        Optional<ManasRaceInstance> race = currentRace(player);
        if (race.isEmpty()) {
            return Optional.empty();
        }
        Optional<SpawnRule> maybeRule = SpawnRuleManager.select(race.get());
        if (maybeRule.isEmpty()) {
            return Optional.empty();
        }
        SpawnRule rule = maybeRule.get();

        ServerLevel level = server.getLevel(rule.dimension());
        if (level == null) {
            TensuraFactionSpawn.LOGGER.warn("Faction spawn rule {} targets unknown dimension {}",
                    rule.id(), rule.dimension().location());
            return Optional.empty();
        }

        BlockPos origin = level.getSharedSpawnPos();
        Optional<BlockPos> target = findNearest(level, rule, origin);
        if (target.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(place(player, level, rule, target.get()));
    }

    /** The player's current race, if any. */
    public static Optional<ManasRaceInstance> currentRace(ServerPlayer player) {
        Races races = RaceAPI.getRaceFrom(player);
        if (races == null) {
            return Optional.empty();
        }
        return races.getRace();
    }

    /** Nearest capital structure for the rule, widening the search when allowed. */
    public static Optional<BlockPos> findNearest(ServerLevel level, SpawnRule rule, BlockPos origin) {
        BlockPos found = nearestStructure(level, rule.structures(), origin, rule.searchRadius());
        if (found == null && rule.expandOnMissing()) {
            found = nearestStructure(level, rule.structures(), origin, EXPANDED_SEARCH_RADIUS);
        }
        return Optional.ofNullable(found);
    }

    private static BlockPos nearestStructure(ServerLevel level, TagKey<Structure> tag, BlockPos origin, int radius) {
        // ServerLevel's overload returns the nearest structure position directly
        // (the Pair<BlockPos, Holder<Structure>> form is ChunkGenerator's).
        return level.findNearestMapStructure(tag, origin, radius, false);
    }

    private static BlockPos place(ServerPlayer player, ServerLevel level, SpawnRule rule, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());

        BlockPos anchor = BlockPos.containing(x, y, z);
        if (rule.setRespawn()) {
            player.setRespawnPosition(level.dimension(), anchor, player.getYRot(), true, false);
        }
        return anchor;
    }
}
