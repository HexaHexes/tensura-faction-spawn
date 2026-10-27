package org.bitwisemadness.tensurafactionspawn.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.tensurafactionspawn.TensuraFactionSpawn;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRule;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRuleManager;
import org.bitwisemadness.tensurafactionspawn.spawn.FactionSpawnService;

import java.util.List;
import java.util.Optional;

/**
 * Server-side coverage for the datapack wiring.
 *
 * <p>These run on a headless GameTest server that loads this mod's bundled
 * datapack, so they prove the rules parse, resolve their tags, and behave
 * gracefully when a capital tag is still empty - all on the real server, not a
 * mock. Worldgen-dependent placement (finding a generated village in a matching
 * biome) is verified in-game under issue #4, because structure placement is
 * biome-constrained and the headless world's biomes make it environmental.</p>
 */
@GameTestHolder(TensuraFactionSpawn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FactionSpawnGameTests {

    private FactionSpawnGameTests() {
    }

    /** The bundled datapack loads and the shipped rules resolve their tags. */
    @GameTest(template = "empty")
    public static void rules_load_from_datapack(GameTestHelper helper) {
        List<SpawnRule> rules = SpawnRuleManager.rules();
        helper.assertTrue(!rules.isEmpty(), "expected at least one faction spawn rule to load");

        SpawnRule humans = rules.stream()
                .filter(rule -> rule.id().getPath().equals("humans"))
                .findFirst()
                .orElse(null);
        helper.assertTrue(humans != null, "expected the shipped 'humans' rule");
        helper.assertTrue(humans.race().location().toString().equals("tensura_faction_spawn:humans"),
                "race tag mismatch: " + humans.race().location());
        helper.assertTrue(humans.structures().location().toString().equals("tensura_faction_spawn:capital/humans"),
                "structure tag mismatch: " + humans.structures().location());
        helper.succeed();
    }

    /** With no capitals registered yet, resolving a target is a clean no-op. */
    @GameTest(template = "empty")
    public static void empty_capital_tag_is_a_graceful_noop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SpawnRule humans = SpawnRuleManager.rules().stream()
                .filter(rule -> rule.id().getPath().equals("humans"))
                .findFirst()
                .orElseThrow();

        Optional<BlockPos> target = FactionSpawnService.findNearest(level, humans, level.getSharedSpawnPos());
        helper.assertTrue(target.isEmpty(), "no capitals are registered yet, so no target should be found");
        helper.succeed();
    }

    /**
     * The bundled interim capitals (Tensura's own villages) are wired to real,
     * registered structures. Vanilla silently drops unresolvable tag values, so
     * the resolved set is pinned to the exact bundled wiring - a typo'd value or
     * a packaging miss fails here instead of disappearing at runtime.
     */
    @GameTest(template = "empty")
    public static void bundled_capital_tags_resolve_to_registered_structures(GameTestHelper helper) {
        assertBundledTag(helper, "dwarves", 1, "tensura:villages/dwarf_village");
        assertBundledTag(helper, "lizardman", 2,
                "tensura:villages/lizardmen_village/tower_tall",
                "tensura:villages/lizardmen_village/tower_short");
        assertBundledTag(helper, "goblins", 7,
                "tensura:villages/goblin_village/acacia",
                "tensura:villages/goblin_village/birch",
                "tensura:villages/goblin_village/jungle",
                "tensura:villages/goblin_village/oak",
                "tensura:villages/goblin_village/palm",
                "tensura:villages/goblin_village/spruce",
                "tensura:villages/orc_village");
        helper.succeed();
    }

    private static void assertBundledTag(GameTestHelper helper, String faction, int expectedSize, String... expectedIds) {
        ServerLevel level = helper.getLevel();
        SpawnRule rule = SpawnRuleManager.rules().stream()
                .filter(r -> r.id().getPath().equals(faction))
                .findFirst()
                .orElseThrow();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<String> resolved = registry.getTag(rule.structures()).orElseThrow().stream()
                .map(h -> h.unwrapKey().map(k -> k.location().toString()).orElse("<direct>"))
                .sorted()
                .toList();
        helper.assertTrue(resolved.size() == expectedSize,
                faction + " tag should resolve to " + expectedSize + " structure(s); got "
                        + resolved.size() + ": " + resolved);
        for (String id : expectedIds) {
            helper.assertTrue(resolved.contains(id), faction + " tag is missing bundled structure " + id);
        }
    }
}
