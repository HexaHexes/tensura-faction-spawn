package org.bitwisemadness.tensurafactionspawn.data;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * The mod's built-in race-group tags - one per faction.
 *
 * <p>Each tag is a {@code TagKey<ManasRace>} loaded from
 * {@code data/<namespace>/tags/manascore_race/races/<path>.json}. The shipped
 * tags live in this mod's own datapack and are additive
 * ({@code "replace": false}), so a pack or an addon can extend a faction without
 * editing this mod.</p>
 */
public final class RaceTags {

    public static final String NAMESPACE = "tensura_faction_spawn";

    public static final TagKey<ManasRace> HUMANS = create("humans");
    public static final TagKey<ManasRace> ELVES = create("elves");
    public static final TagKey<ManasRace> DWARVES = create("dwarves");
    public static final TagKey<ManasRace> CELESTIALS = create("celestials");
    public static final TagKey<ManasRace> HELL = create("hell");
    public static final TagKey<ManasRace> BEASTFOLK = create("beastfolk");
    public static final TagKey<ManasRace> AQUATIC = create("aquatic");
    public static final TagKey<ManasRace> INSECT = create("insect");
    public static final TagKey<ManasRace> MONSTERS = create("monsters");
    public static final TagKey<ManasRace> GOBLINS = create("goblins");
    public static final TagKey<ManasRace> LIZARDMAN = create("lizardman");
    public static final TagKey<ManasRace> GIANTS = create("giants");

    public static final List<TagKey<ManasRace>> ALL =
            List.of(HUMANS, ELVES, DWARVES, CELESTIALS, HELL, BEASTFOLK, AQUATIC, INSECT, MONSTERS, GOBLINS,
                    LIZARDMAN, GIANTS);

    private RaceTags() {
    }

    private static TagKey<ManasRace> create(String path) {
        return TagKey.create(RaceAPI.getRaceRegistryKey(),
                ResourceLocation.fromNamespaceAndPath(NAMESPACE, path));
    }
}
