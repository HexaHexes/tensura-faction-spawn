package org.bitwisemadness.tensurafactionspawn.capital;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.bitwisemadness.tensurafactionspawn.TensuraFactionSpawn;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRule;
import org.bitwisemadness.tensurafactionspawn.data.SpawnRuleManager;

/**
 * Locates every registered capital once per world and records it in the
 * {@link CapitalRegistry}.
 *
 * <p>The structure search is O(radius^2) (it walks outward ring by ring), so
 * the work is spread across server ticks - one search per tick - and runs
 * once per capital per world: tags already in the registry are skipped. The
 * search tries the rule's explicit dimension first, then the overworld, then
 * every other loaded level, because a capital tag's structures can live in
 * any dimension (the hell gate lives in Tensura's hell).</p>
 *
 * <p>Started via {@code /tensurafactionspawn locate-all} or lazily by
 * consumers; results land in the {@link CapitalRegistry}.</p>
 */
public final class CapitalLocator {

    /** Search radius in chunks (with the registry's expand handled inside the search call). */
    public static final int SEARCH_RADIUS = 512;

    private static final ArrayDeque<LocateJob> QUEUE = new ArrayDeque<>();
    private static final Set<String> QUEUED = new HashSet<>();

    /** One pending search: a capital tag, the dimensions to try in order, and the search origin. */
    public static final class LocateJob {
        private final ResourceLocation tagLocation;
        private final TagKey<Structure> tag;
        private final List<ResourceKey<Level>> dimensions;
        private final BlockPos origin;
        private int levelIndex;

        LocateJob(ResourceLocation tagLocation, TagKey<Structure> tag,
                  List<ResourceKey<Level>> dimensions, BlockPos origin) {
            this.tagLocation = tagLocation;
            this.tag = tag;
            this.dimensions = dimensions;
            this.origin = origin;
        }

        public ResourceLocation tagLocation() {
            return tagLocation;
        }

        public TagKey<Structure> tag() {
            return tag;
        }

        public BlockPos origin() {
            return origin;
        }

        public List<ResourceKey<Level>> dimensions() {
            return dimensions;
        }

        public int levelIndex() {
            return levelIndex;
        }

        void advance() {
            levelIndex++;
        }
    }

    private CapitalLocator() {
    }

    /**
     * Builds the locate jobs from the loaded spawn rules: one per distinct
     * capital tag, skipping tags the registry already holds.
     */
    public static List<LocateJob> buildJobs(MinecraftServer server) {
        BlockPos origin = server.overworld().getSharedSpawnPos();
        CapitalRegistry registry = CapitalRegistry.get(server.overworld().getDataStorage());
        List<LocateJob> jobs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SpawnRule rule : SpawnRuleManager.rules()) {
            ResourceLocation tagLocation = rule.structures().location();
            if (!seen.add(tagLocation.toString()) || registry.has(tagLocation)) {
                continue;
            }
            List<ResourceKey<Level>> dimensions = new ArrayList<>();
            dimensions.add(rule.dimension().orElse(Level.OVERWORLD));
            if (rule.dimension().isEmpty()) {
                for (ServerLevel level : server.getAllLevels()) {
                    if (!level.dimension().equals(Level.OVERWORLD)) {
                        dimensions.add(level.dimension());
                    }
                }
            }
            jobs.add(new LocateJob(tagLocation, rule.structures(), dimensions, origin));
        }
        return jobs;
    }

    /** Queues a locate pass for every unlocated capital tag. Returns the number of jobs queued. */
    public static int startLocating(MinecraftServer server) {
        List<LocateJob> jobs = buildJobs(server);
        for (LocateJob job : jobs) {
            if (QUEUED.add(job.tagLocation().toString())) {
                QUEUE.add(job);
            }
        }
        if (!jobs.isEmpty()) {
            TensuraFactionSpawn.LOGGER.info("Capital locator queued {} capital tag(s) for location", QUEUE.size());
        }
        return QUEUE.size();
    }

    /** Drains one search per server tick (spec §5.9-lite: spread, don't stall). */
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        LocateJob job = QUEUE.poll();
        if (job == null) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(job.dimensions().get(job.levelIndex()));
        BlockPos found = level == null
                ? null
                : level.findNearestMapStructure(job.tag(), job.origin(), SEARCH_RADIUS, false);

        if (found != null) {
            CapitalRegistry.get(server.overworld().getDataStorage())
                    .put(job.tagLocation(), job.dimensions().get(job.levelIndex()), found);
            TensuraFactionSpawn.LOGGER.info("Capital located: {} -> {} in {}",
                    job.tagLocation(), found.toShortString(),
                    job.dimensions().get(job.levelIndex()).location());
            QUEUED.remove(job.tagLocation().toString());
        } else if (job.levelIndex + 1 < job.dimensions().size()) {
            // advance to the next dimension and retry on a later tick
            job.advance();
            QUEUE.add(job);
        } else {
            TensuraFactionSpawn.LOGGER.info("Capital locator: no structure found for {} in any loaded dimension",
                    job.tagLocation());
            QUEUED.remove(job.tagLocation().toString());
        }
    }

    /** True while locate jobs remain queued. */
    public static boolean isRunning() {
        return !QUEUE.isEmpty();
    }
}
