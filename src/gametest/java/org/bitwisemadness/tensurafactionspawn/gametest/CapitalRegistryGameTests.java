package org.bitwisemadness.tensurafactionspawn.gametest;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.tensurafactionspawn.TensuraFactionSpawn;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalEntry;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalLocator;
import org.bitwisemadness.tensurafactionspawn.capital.CapitalRegistry;


/**
 * Deterministic coverage for the capital registry: the locator's job
 * enumeration covers every loaded capital tag, and the registry's in-memory
 * and NBT round-trips are lossless. Worldgen placement itself is
 * biome-environmental and is verified in game.
 */
@GameTestHolder(TensuraFactionSpawn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CapitalRegistryGameTests {

    private CapitalRegistryGameTests() {
    }

    /** The locator enumerates one job per distinct loaded capital tag. */
    @GameTest(template = "empty")
    public static void locator_enumerates_all_capital_tags(GameTestHelper helper) {
        List<CapitalLocator.LocateJob> jobs = CapitalLocator.buildJobs(helper.getLevel().getServer());
        long distinct = jobs.stream().map(j -> j.tagLocation().toString()).distinct().count();
        helper.assertTrue(jobs.size() >= 12, "expected at least 12 capital tags, got " + jobs.size());
        helper.assertTrue(distinct == jobs.size(), "capital tag jobs contain duplicates");
        helper.succeed();
    }

    /** In-memory put/get round-trip. */
    @GameTest(template = "empty")
    public static void registry_put_get_round_trip(GameTestHelper helper) {
        var registry = CapitalRegistry.get(helper.getLevel().getServer().overworld().getDataStorage());
        ResourceLocation tag = ResourceLocation.fromNamespaceAndPath(
                TensuraFactionSpawn.MOD_ID, "capital/registry_test");
        ResourceKey<Level> dimension = Level.OVERWORLD;
        BlockPos pos = new BlockPos(123, 64, -456);

        registry.put(tag, dimension, pos);
        Optional<CapitalEntry> entry = registry.get(tag);
        helper.assertTrue(entry.isPresent(), "entry missing after put");
        helper.assertTrue(entry.orElseThrow().dimension().equals(dimension), "dimension round-trip failed");
        helper.assertTrue(entry.orElseThrow().position().equals(pos), "position round-trip failed");
        registry.all(); // must not throw
        helper.succeed();
    }

    /** NBT save/load round-trip is lossless. */
    @GameTest(template = "empty")
    public static void registry_nbt_round_trip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var registry = CapitalRegistry.get(helper.getLevel().getServer().overworld().getDataStorage());
        ResourceLocation tag = ResourceLocation.fromNamespaceAndPath(
                TensuraFactionSpawn.MOD_ID, "capital/nbt_test");
        BlockPos pos = new BlockPos(-1000, 32, 2000);

        registry.put(tag, Level.OVERWORLD, pos);
        var saved = registry.save(new net.minecraft.nbt.CompoundTag(), provider);
        var loaded = CapitalRegistry.load(saved, provider);

        Optional<CapitalEntry> entry = loaded.get(tag);
        helper.assertTrue(entry.isPresent(), "entry missing after NBT round-trip");
        helper.assertTrue(entry.orElseThrow().position().equals(pos), "position lost in NBT round-trip");
        helper.assertTrue(loaded.size() >= registry.size(), "entries lost in NBT round-trip");
        helper.succeed();
    }
}
