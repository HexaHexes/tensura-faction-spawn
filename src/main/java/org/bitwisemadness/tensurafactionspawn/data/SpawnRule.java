package org.bitwisemadness.tensurafactionspawn.data;

import com.google.gson.JsonObject;
import java.util.Optional;
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
 * <p>{@code race} names the faction's race-group tag (matched by the race
 * integration through {@code capital-api}) and {@code structures} names the
 * valid capitals. {@code dimension} is empty when the rule defers to the
 * race's own respawn dimension - dimension is per-race in Tensura (only the
 * daemon chain overrides it, to {@code tensura:hell}), so a hardcoded
 * faction-wide dimension would misplace addon races that join a faction tag
 * later.</p>
 */
public record SpawnRule(
        ResourceLocation id,
        SpawnRuleFields fields,
        ResourceLocation race,
        TagKey<Structure> structures,
        Optional<ResourceKey<Level>> dimension) {

    public int searchRadius() {
        return fields.searchRadius();
    }

    public boolean expandOnMissing() {
        return fields.expandOnMissing();
    }

    public RespawnMode respawnMode() {
        return fields.respawnMode();
    }

    /** Parse and resolve one rule, throwing {@link IllegalArgumentException} on bad input. */
    public static SpawnRule from(ResourceLocation id, JsonObject json) {
        SpawnRuleFields fields = SpawnRuleFields.fromJson(json);
        ResourceLocation raceId = requireLocation(fields.race(), "race");
        ResourceLocation structureId = requireLocation(fields.structures(), "structures");
        Optional<ResourceKey<Level>> dimension = fields.dimension() == null
                ? Optional.empty()
                : Optional.of(ResourceKey.create(Registries.DIMENSION,
                        requireLocation(fields.dimension(), "dimension")));
        return new SpawnRule(id, fields, raceId,
                TagKey.create(Registries.STRUCTURE, structureId), dimension);
    }

    private static ResourceLocation requireLocation(String value, String field) {
        ResourceLocation location = ResourceLocation.tryParse(value);
        if (location == null) {
            throw new IllegalArgumentException("field '" + field + "' is not a valid resource location: '" + value + "'");
        }
        return location;
    }
}
