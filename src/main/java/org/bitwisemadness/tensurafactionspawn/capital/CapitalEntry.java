package org.bitwisemadness.tensurafactionspawn.capital;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * One registered capital: the faction's capital tag, the dimension it lives
 * in, and its located position (nearest to the world spawn at locate time).
 *
 * <p>Positions are cached per world in {@link CapitalRegistry} so the
 * O(radius^2) structure search runs once per capital, not once per player.
 * The specific structure inside the tag is not recorded - the ServerLevel
 * locate overload returns a position only.</p>
 */
public record CapitalEntry(
        ResourceLocation tagLocation,
        ResourceKey<Level> dimension,
        BlockPos position) {

    public CapitalEntry {
        Objects.requireNonNull(tagLocation, "tagLocation");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
    }
}
