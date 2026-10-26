package org.bitwisemadness.tensurafactionspawn.gametest;

import net.minecraft.core.BlockPos;
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
 * mock.</p>
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
}
