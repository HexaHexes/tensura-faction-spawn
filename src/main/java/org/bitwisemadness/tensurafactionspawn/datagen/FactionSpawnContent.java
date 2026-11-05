package org.bitwisemadness.tensurafactionspawn.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The datapack tables: one spawn rule per faction and the capital structure
 * tags, ported byte-for-byte from the hand-authored originals (issue #34).
 *
 * <p>The interim capitals (Tensura's own villages) keep their bundled wiring so
 * factions work out of the box: dwarves, goblins (and orcs) and lizardmen
 * resolve to real structures, every other faction ships an empty additive tag
 * anchor that capital datapacks append to with {@code "replace": false}. The
 * giants anchor is owned by the capital-giants addon and is deliberately not
 * shipped here - the validation pins that asymmetry.</p>
 */
public final class FactionSpawnContent {

    /** The factions in id order - one spawn rule each. */
    private static final List<String> RACES = List.of(
            "aquatic", "beastfolk", "celestials", "dwarves", "elves", "giants",
            "goblins", "hell", "humans", "insect", "lizardman", "monsters");

    /** The one faction whose tag anchor lives in its capital addon, not here. */
    private static final String ADDON_OWNED_TAG = "giants";

    private static final int SEARCH_RADIUS = 128;
    private static final int PRIORITY = 0;

    /** Interim capitals: faction -> Tensura village structure ids. */
    private static final Map<String, List<String>> INTERIM_CAPITALS = Map.of(
            "dwarves", List.of("tensura:villages/dwarf_village"),
            "goblins", List.of(
                    "tensura:villages/goblin_village/acacia",
                    "tensura:villages/goblin_village/birch",
                    "tensura:villages/goblin_village/jungle",
                    "tensura:villages/goblin_village/oak",
                    "tensura:villages/goblin_village/palm",
                    "tensura:villages/goblin_village/spruce",
                    "tensura:villages/orc_village"),
            "lizardman", List.of(
                    "tensura:villages/lizardmen_village/tower_tall",
                    "tensura:villages/lizardmen_village/tower_short"));

    private FactionSpawnContent() {
    }

    /** Every datapack file, keyed by its path inside the jar. */
    public static Map<String, JsonObject> files() {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        for (String race : RACES) {
            files.put(spawnRulePath(race), spawnRule(race));
            if (!race.equals(ADDON_OWNED_TAG)) {
                files.put(tagPath(race), capitalTag(race));
            }
        }
        validate(files);
        return files;
    }

    private static String spawnRulePath(String race) {
        return "data/tensura_faction_spawn/tensura_faction_spawn/spawn_rules/" + race + ".json";
    }

    private static String tagPath(String race) {
        return "data/tensura_faction_spawn/tags/worldgen/structure/capital/" + race + ".json";
    }

    private static JsonObject spawnRule(String race) {
        JsonObject json = new JsonObject();
        json.addProperty("race", "#tensura_faction_spawn:" + race);
        json.addProperty("structures", "#tensura_faction_spawn:capital/" + race);
        json.addProperty("search_radius", SEARCH_RADIUS);
        json.addProperty("expand_on_missing", true);
        json.addProperty("priority", PRIORITY);
        return json;
    }

    private static JsonObject capitalTag(String race) {
        JsonObject json = new JsonObject();
        json.addProperty("replace", false);
        JsonArray values = new JsonArray();
        List<String> interim = INTERIM_CAPITALS.get(race);
        if (interim != null) {
            interim.forEach(values::add);
        }
        json.add("values", values);
        return json;
    }

    /**
     * Pins the table against silent drift: exact file counts, the spawn-rule
     * shape, the interim capital wiring and the giants asymmetry.
     */
    private static void validate(Map<String, JsonObject> files) {
        List<String> errors = new java.util.ArrayList<>();
        long rules = files.keySet().stream().filter(p -> p.contains("/spawn_rules/")).count();
        long tags = files.keySet().stream().filter(p -> p.contains("/capital/")).count();
        if (rules != RACES.size()) {
            errors.add("spawn rules: " + rules + " expected " + RACES.size());
        }
        if (tags != RACES.size() - 1) {
            errors.add("capital tags: " + tags + " expected " + (RACES.size() - 1)
                    + " (every faction except " + ADDON_OWNED_TAG + ")");
        }
        if (files.containsKey(tagPath(ADDON_OWNED_TAG))) {
            errors.add("the " + ADDON_OWNED_TAG + " tag anchor must not ship here - its capital addon owns it");
        }
        for (String race : RACES) {
            JsonObject rule = files.get(spawnRulePath(race));
            if (!rule.get("race").getAsString().equals("#tensura_faction_spawn:" + race)) {
                errors.add(race + " rule: wrong race tag");
            }
            if (!rule.get("structures").getAsString().equals("#tensura_faction_spawn:capital/" + race)) {
                errors.add(race + " rule: wrong structure tag");
            }
            if (rule.get("search_radius").getAsInt() != SEARCH_RADIUS
                    || !rule.get("expand_on_missing").getAsBoolean()
                    || rule.get("priority").getAsInt() != PRIORITY) {
                errors.add(race + " rule: unexpected radius/expand/priority");
            }
            JsonObject tag = files.get(tagPath(race));
            if (tag == null) {
                continue;
            }
            if (tag.get("replace").getAsBoolean()) {
                errors.add(race + " tag: replace must stay false (addons append)");
            }
            List<String> expected = INTERIM_CAPITALS.getOrDefault(race, List.of());
            List<String> actual = new java.util.ArrayList<>();
            tag.getAsJsonArray("values").forEach(v -> actual.add(v.getAsString()));
            if (!actual.equals(expected)) {
                errors.add(race + " tag: interim capital wiring drifted, expected " + expected);
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("faction spawn datapack drift: " + errors);
        }
    }
}
