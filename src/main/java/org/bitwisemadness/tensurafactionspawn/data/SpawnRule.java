package org.bitwisemadness.tensurafactionspawn.data;

import com.google.gson.JsonObject;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * A validated spawn rule: the Minecraft-resolved view of a
 * {@link SpawnRuleFields} entry.
 *
 * <p>{@code race} matches the player's race, {@code structures} names the valid
 * capitals, and {@code dimension} is the level the capitals live in.</p>
 */
public record SpawnRule(
        ResourceLocation id,
        SpawnRuleFields fields,
        TagKey<ManasRace> race,
        TagKey<Structure> structures,
        ResourceKey<Level> dimension) {

    public int searchRadius() {
        return fields.searchRadius();
    }

    public boolean expandOnMissing() {
        return fields.expandOnMissing();
    }

    public boolean setRespawn() {
        return fields.setRespawn();
    }

    /** Parse and resolve one rule, throwing {@link IllegalArgumentException} on bad input. */
    public static SpawnRule from(ResourceLocation id, JsonObject json) {
        SpawnRuleFields fields = SpawnRuleFields.fromJson(json);
        ResourceLocation raceId = requireLocation(fields.race(), "race");
        ResourceLocation structureId = requireLocation(fields.structures(), "structures");
        ResourceLocation dimensionId = requireLocation(fields.dimension(), "dimension");
        return new SpawnRule(
                id,
                fields,
                TagKey.create(RaceAPI.getRaceRegistryKey(), raceId),
                TagKey.create(Registries.STRUCTURE, structureId),
                ResourceKey.create(Registries.DIMENSION, dimensionId));
    }

    private static ResourceLocation requireLocation(String value, String field) {
        ResourceLocation location = ResourceLocation.tryParse(value);
        if (location == null) {
            throw new IllegalArgumentException("field '" + field + "' is not a valid resource location: '" + value + "'");
        }
        return location;
    }
}
