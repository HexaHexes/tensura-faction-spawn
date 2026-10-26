package org.bitwisemadness.tensurafactionspawn.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Comparator;
import java.util.Optional;

/**
 * The raw, Minecraft-free fields of a single spawn rule, as written in
 * {@code data/<namespace>/tensura_faction_spawn/spawn_rules/<id>.json}.
 *
 * <p>Kept free of Minecraft types so parsing and ordering are unit-testable on a
 * plain JVM. {@link SpawnRule} wraps this with the resolved {@code TagKey}s and
 * dimension key.</p>
 *
 * <p>A rule maps one <em>race-group tag</em> to one <em>structure tag</em>:
 * {@code race} names which races the rule applies to, and {@code structures}
 * names the capitals valid for them. Both are datapack tags.</p>
 */
public record SpawnRuleFields(
        String race,
        String structures,
        String dimension,
        int searchRadius,
        boolean expandOnMissing,
        boolean setRespawn,
        int priority) {

    public static final String DEFAULT_DIMENSION = "minecraft:overworld";

    /**
     * Default search radius in chunks (~2048 blocks). {@code findNearestMapStructure}
     * searches ring by ring, so its cost grows with the square of this value - keep
     * it modest. 128 chunks matches a comfortable "near the world spawn" band.
     */
    public static final int DEFAULT_SEARCH_RADIUS = 128;

    /**
     * Resolution order: highest priority first, then race tag name ascending, so
     * two rules matching the same race always resolve deterministically.
     */
    public static final Comparator<SpawnRuleFields> RESOLUTION_ORDER =
            Comparator.comparingInt(SpawnRuleFields::priority).reversed()
                    .thenComparing(SpawnRuleFields::race);

    /** Parse and validate one rule object. Throws {@link IllegalArgumentException} on bad input. */
    public static SpawnRuleFields fromJson(JsonObject json) {
        String race = requiredTag(json, "race");
        String structures = requiredTag(json, "structures");
        String dimension = optionalString(json, "dimension").orElse(DEFAULT_DIMENSION);
        int searchRadius = optionalInt(json, "search_radius").orElse(DEFAULT_SEARCH_RADIUS);
        if (searchRadius < 1) {
            throw new IllegalArgumentException("search_radius must be >= 1, got " + searchRadius);
        }
        boolean expandOnMissing = optionalBoolean(json, "expand_on_missing").orElse(true);
        boolean setRespawn = optionalBoolean(json, "set_respawn").orElse(false);
        int priority = optionalInt(json, "priority").orElse(0);
        return new SpawnRuleFields(race, structures, dimension, searchRadius, expandOnMissing, setRespawn, priority);
    }

    /** The tag's resource-location string with any leading {@code #} removed. */
    public static String stripHash(String value) {
        return value.startsWith("#") ? value.substring(1) : value;
    }

    private static String requiredTag(JsonObject json, String key) {
        String value = optionalString(json, key)
                .orElseThrow(() -> new IllegalArgumentException("missing required field '" + key + "'"));
        if (value.isBlank()) {
            throw new IllegalArgumentException("field '" + key + "' must not be blank");
        }
        return stripHash(value);
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        return Optional.of(element.getAsString());
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        return Optional.of(element.getAsInt());
    }

    private static Optional<Boolean> optionalBoolean(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        return Optional.of(element.getAsBoolean());
    }
}
