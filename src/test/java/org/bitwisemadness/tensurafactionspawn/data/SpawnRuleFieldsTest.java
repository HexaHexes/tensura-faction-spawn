package org.bitwisemadness.tensurafactionspawn.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

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
                  "race": "#tensura_faction_spawn:celestials",
                  "structures": "#tensura_faction_spawn:capital/celestials",
                  "dimension": "tensura:hell",
                  "search_radius": 512,
                  "expand_on_missing": false,
                  "set_respawn": "always",
                  "priority": 3
                }
                """));

        assertEquals("tensura_faction_spawn:celestials", fields.race());
        assertEquals("tensura_faction_spawn:capital/celestials", fields.structures());
        assertEquals("tensura:hell", fields.dimension());
        assertEquals(512, fields.searchRadius());
        assertFalse(fields.expandOnMissing());
        assertEquals(RespawnMode.ALWAYS, fields.respawnMode());
        assertEquals(3, fields.priority());
    }

    @Test
    void appliesDefaultsForOptionalFields() {
        SpawnRuleFields fields = SpawnRuleFields.fromJson(json("""
                { "race": "humans", "structures": "capital/humans" }
                """));

        assertNull(fields.dimension(), "an absent dimension must defer to the race's own respawn dimension");
        assertEquals(SpawnRuleFields.DEFAULT_SEARCH_RADIUS, fields.searchRadius());
        assertTrue(fields.expandOnMissing());
        assertEquals(RespawnMode.IF_UNSET, fields.respawnMode());
        assertEquals(0, fields.priority());
    }

    @Test
    void mapsLegacyBooleansForSetRespawn() {
        SpawnRuleFields truthy = SpawnRuleFields.fromJson(json(
                "{ \"race\": \"a:b\", \"structures\": \"c:d\", \"set_respawn\": true }"));
        SpawnRuleFields falsy = SpawnRuleFields.fromJson(json(
                "{ \"race\": \"a:b\", \"structures\": \"c:d\", \"set_respawn\": false }"));

        assertEquals(RespawnMode.ALWAYS, truthy.respawnMode());
        assertEquals(RespawnMode.NEVER, falsy.respawnMode());
    }

    @Test
    void rejectsAnUnknownSetRespawnValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpawnRuleFields.fromJson(json(
                        "{ \"race\": \"a:b\", \"structures\": \"c:d\", \"set_respawn\": \"sometimes\" }")));
        assertTrue(ex.getMessage().contains("set_respawn"));
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
                () -> SpawnRuleFields.fromJson(json(
                        "{ \"race\": \"a:b\", \"structures\": \"c:d\", \"search_radius\": 0 }")));
    }

    @Test
    void resolutionOrderPrefersHigherPriorityThenNameAscending() {
        SpawnRuleFields low = new SpawnRuleFields("zzz:race", "s", null, 1, true, RespawnMode.IF_UNSET, 0);
        SpawnRuleFields highB = new SpawnRuleFields("bbb:race", "s", null, 1, true, RespawnMode.IF_UNSET, 5);
        SpawnRuleFields highA = new SpawnRuleFields("aaa:race", "s", null, 1, true, RespawnMode.IF_UNSET, 5);

        List<SpawnRuleFields> rules = new ArrayList<>(List.of(low, highB, highA));
        rules.sort(SpawnRuleFields.RESOLUTION_ORDER);

        assertEquals(List.of(highA, highB, low), rules);
    }
}
