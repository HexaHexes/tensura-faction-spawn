package org.bitwisemadness.tensurafactionspawn.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

/**
 * The mod's built-in capital structure tags - one per faction.
 *
 * <p>Each tag is a {@code TagKey<Structure>} loaded from
 * {@code data/<namespace>/tags/worldgen/structure/capital/<path>.json}. A capital
 * datapack registers its city structure and appends it to the matching tag with
 * {@code "replace": false}; the spawn mod never needs to know the structure id.</p>
 */
public final class StructureTags {

    public static final String NAMESPACE = "tensura_faction_spawn";
    public static final String CAPITAL_PREFIX = "capital/";

    public static final TagKey<Structure> CAPITAL_HUMANS = create("humans");
    public static final TagKey<Structure> CAPITAL_ELVES = create("elves");
    public static final TagKey<Structure> CAPITAL_DWARVES = create("dwarves");
    public static final TagKey<Structure> CAPITAL_SPIRITFOLK = create("spiritfolk");
    public static final TagKey<Structure> CAPITAL_BEASTFOLK = create("beastfolk");
    public static final TagKey<Structure> CAPITAL_AQUATIC = create("aquatic");
    public static final TagKey<Structure> CAPITAL_INSECT = create("insect");
    public static final TagKey<Structure> CAPITAL_MONSTERS = create("monsters");
    public static final TagKey<Structure> CAPITAL_GOBLINS = create("goblins");
    public static final TagKey<Structure> CAPITAL_LIZARDMAN = create("lizardman");

    public static final List<TagKey<Structure>> ALL = List.of(
            CAPITAL_HUMANS, CAPITAL_ELVES, CAPITAL_DWARVES, CAPITAL_SPIRITFOLK, CAPITAL_BEASTFOLK,
            CAPITAL_AQUATIC, CAPITAL_INSECT, CAPITAL_MONSTERS, CAPITAL_GOBLINS, CAPITAL_LIZARDMAN);

    private StructureTags() {
    }

    private static TagKey<Structure> create(String faction) {
        return TagKey.create(Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath(NAMESPACE, CAPITAL_PREFIX + faction));
    }
}
