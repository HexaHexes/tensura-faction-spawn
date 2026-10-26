package org.bitwisemadness.tensurafactionspawn.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.bitwisemadness.tensurafactionspawn.TensuraFactionSpawn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server datapack reload listener for spawn rules.
 *
 * <p>Reads {@code tensura_faction_spawn/spawn_rules/*.json}, validates each
 * entry, and keeps them in resolution order (priority descending, then race tag
 * name). {@link #select(ManasRaceInstance)} returns the first rule whose
 * race-group tag the given race is a member of.</p>
 */
public final class SpawnRuleManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIRECTORY = "tensura_faction_spawn/spawn_rules";

    private static final Comparator<SpawnRule> ORDER =
            Comparator.<SpawnRule, SpawnRuleFields>comparing(SpawnRule::fields, SpawnRuleFields.RESOLUTION_ORDER)
                    .thenComparing(rule -> rule.id().toString());

    private static volatile List<SpawnRule> rules = List.of();

    public SpawnRuleManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<SpawnRule> parsed = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            JsonElement element = entry.getValue();
            if (element == null || !element.isJsonObject()) {
                TensuraFactionSpawn.LOGGER.error("Skipping faction spawn rule {}: not a JSON object", entry.getKey());
                continue;
            }
            JsonObject json = element.getAsJsonObject();
            try {
                parsed.add(SpawnRule.from(entry.getKey(), json));
            } catch (RuntimeException ex) {
                TensuraFactionSpawn.LOGGER.error("Skipping invalid faction spawn rule {}: {}", entry.getKey(), ex.getMessage());
            }
        }
        parsed.sort(ORDER);
        rules = List.copyOf(parsed);
        TensuraFactionSpawn.LOGGER.info("Loaded {} faction spawn rule(s)", rules.size());
    }

    /** The loaded rules, in resolution order. */
    public static List<SpawnRule> rules() {
        return rules;
    }

    /** The first rule whose race-group tag contains the given race, if any. */
    public static Optional<SpawnRule> select(ManasRaceInstance race) {
        for (SpawnRule rule : rules) {
            if (race.is(rule.race())) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }
}
