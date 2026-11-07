package org.bitwisemadness.tensurafactionspawn.gametest;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.capitalapi.CapitalApi;
import org.bitwisemadness.tensurafactionspawn.TensuraFactionSpawn;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRule;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRuleManager;
import org.bitwisemadness.tensurafactionspawn.datagen.FactionSpawnContent;
import org.bitwisemadness.tensurafactionspawn.spawn.FactionSpawnService;

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
        helper.assertTrue(humans.race().toString().equals("tensura_faction_spawn:humans"),
                "race tag mismatch: " + humans.race());
        helper.assertTrue(humans.structures().location().toString().equals("tensura_faction_spawn:capital/humans"),
                "structure tag mismatch: " + humans.structures().location());

        // Every datagen-produced file must ship and resolve on the live
        // classpath (issue #34); the resource id drops the data/<ns>/
        // prefix from the jar-relative path and re-runs table validation.
        FactionSpawnContent.files().keySet().forEach(path -> {
            String rest = path.substring(("data/" + TensuraFactionSpawn.MOD_ID + "/").length());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TensuraFactionSpawn.MOD_ID, rest);
            helper.assertTrue(
                    helper.getLevel().getServer().getResourceManager().getResource(id).isPresent(),
                    "generated datapack file missing at runtime: " + path);
        });
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
     * The boundary contract is wired across the three-jar split: the race
     * integration (a separate jar, staged into this runtime) registered its
     * resolver at construct time, and this engine registered its relocation
     * side. If either registration is missing, the split's runtime wiring is
     * broken and the sweep would discover it the hard way.
     */
    @GameTest(template = "empty")
    public static void boundary_contract_is_wired(GameTestHelper helper) {
        helper.assertTrue(CapitalApi.integration().isPresent(),
                "the race integration did not register its resolver");
        helper.assertTrue(CapitalApi.engine().isPresent(),
                "the spawn engine did not register its relocation side");
        helper.succeed();
    }

    /**
     * The full staged ecosystem boots in this runtime (the engine, the api,
     * the integration, the generation engine and every capital jar), and all
     * twelve faction capital tags resolve to at least one structure. A jar
     * that fails to load, a duplicate-id conflict, or a typo'd tag shows up
     * here instead of in the instance.
     */
    @GameTest(template = "empty")
    public static void all_capital_tags_resolve(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        String[] factions = {"humans", "elves", "dwarves", "celestials", "hell", "beastfolk",
                "aquatic", "insect", "monsters", "goblins", "lizardman", "giants"};
        for (String faction : factions) {
            var tag = TagKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath("tensura_faction_spawn", "capital/" + faction));
            var holders = registry.getTag(tag);
            int count = holders.map(h -> h.size()).orElse(0);
            helper.assertTrue(count > 0,
                    "the '" + faction + "' capital tag resolved to " + count + " structures");
        }
        helper.succeed();
    }
    /**
     * The bundled interim capitals (Tensura's own villages) are wired to real,
     * registered structures. Vanilla silently drops unresolvable tag values, so
     * every bundled wiring is asserted present. The real-capital jars merge
     * their structures into these same tags (the relocation rules point at
     * them), so the resolved set is bundled + jar contributions - the size is
     * pinned from below, and the contains-assertions still catch a typo'd
     * value or a packaging miss.
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
        helper.assertTrue(resolved.size() >= expectedSize,
                faction + " tag should resolve to at least " + expectedSize + " structure(s); got "
                        + resolved.size() + ": " + resolved);
        for (String id : expectedIds) {
            helper.assertTrue(resolved.contains(id), faction + " tag is missing bundled structure " + id);
        }
    }
}
