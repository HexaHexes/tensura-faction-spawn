package org.bitwisemadness.tensurafactionspawn.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsing and ordering of a spawn rule. Pure JVM: {@link SpawnRuleFields} holds
 * no Minecraft types.
 */
class SpawnRuleFieldsTest {

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void parsesACompleteRule() {
        SpawnRuleFields fields = SpawnRuleFields.fromJson(json("""
                {
                  "race": "#tensura_faction_spawn:spiritfolk",
                  "structures": "#tensura_faction_spawn:capital/spiritfolk",
                  "dimension": "tensura:hell",
                  "search_radius": 512,
                  "expand_on_missing": false,
                  "set_respawn": true,
                  "priority": 3
                }
                """));

        assertEquals("tensura_faction_spawn:spiritfolk", fields.race());
        assertEquals("tensura_faction_spawn:capital/spiritfolk", fields.structures());
        assertEquals("tensura:hell", fields.dimension());
        assertEquals(512, fields.searchRadius());
        assertFalse(fields.expandOnMissing());
        assertTrue(fields.setRespawn());
        assertEquals(3, fields.priority());
    }

    @Test
    void appliesDefaultsForOptionalFields() {
        SpawnRuleFields fields = SpawnRuleFields.fromJson(json("""
                { "race": "humans", "structures": "capital/humans" }
                """));

        assertEquals(SpawnRuleFields.DEFAULT_DIMENSION, fields.dimension());
        assertEquals(SpawnRuleFields.DEFAULT_SEARCH_RADIUS, fields.searchRadius());
        assertTrue(fields.expandOnMissing());
        assertFalse(fields.setRespawn());
        assertEquals(0, fields.priority());
    }

    @Test
    void stripsALeadingHashFromTagReferences() {
        SpawnRuleFields fields = SpawnRuleFields.fromJson(json("""
                { "race": "#a:b", "structures": "#c:d" }
                """));

        assertEquals("a:b", fields.race());
        assertEquals("c:d", fields.structures());
    }

    @Test
    void rejectsAMissingRace() {
        assertEquals("missing required field 'race'",
                assertThrows(IllegalArgumentException.class,
                        () -> SpawnRuleFields.fromJson(json("{ \"structures\": \"x:y\" }"))).getMessage());
    }

    @Test
    void rejectsABlankStructuresField() {
        assertThrows(IllegalArgumentException.class,
                () -> SpawnRuleFields.fromJson(json("{ \"race\": \"a:b\", \"structures\": \"  \" }")));
    }

    @Test
    void rejectsANonPositiveSearchRadius() {
        assertThrows(IllegalArgumentException.class,
                () -> SpawnRuleFields.fromJson(json("{ \"race\": \"a:b\", \"structures\": \"c:d\", \"search_radius\": 0 }")));
    }

    @Test
    void resolutionOrderPrefersHigherPriorityThenNameAscending() {
        SpawnRuleFields low = new SpawnRuleFields("zzz:race", "s", "minecraft:overworld", 1, true, false, 0);
        SpawnRuleFields highB = new SpawnRuleFields("bbb:race", "s", "minecraft:overworld", 1, true, false, 5);
        SpawnRuleFields highA = new SpawnRuleFields("aaa:race", "s", "minecraft:overworld", 1, true, false, 5);

        List<SpawnRuleFields> rules = new ArrayList<>(List.of(low, highB, highA));
        rules.sort(SpawnRuleFields.RESOLUTION_ORDER);

        assertEquals(List.of(highA, highB, low), rules);
    }
}
