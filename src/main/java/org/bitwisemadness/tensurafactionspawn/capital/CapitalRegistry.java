package org.bitwisemadness.tensurafactionspawn.capital;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Per-world persisted registry of located capitals (spec §17
 * CapitalStateRepository seed; §5.3 "persist the plan before construction"
 * pattern in miniature).
 *
 * <p>Stored in the overworld's {@link DimensionDataStorage} via
 * {@code computeIfAbsent}, so entries survive restarts. The
 * {@link CapitalLocator} fills it once per world (the O(radius^2) structure
 * search is the expensive part); consumers - arrival, commands, future map
 * integrations - query positions from here instead of searching.</p>
 */
public final class CapitalRegistry extends SavedData {

    private static final String DATA_NAME = "tensura_faction_spawn_capitals";
    private static final String ENTRIES_KEY = "Entries";

    private final Map<String, CapitalEntry> entries = new HashMap<>();

    public static CapitalRegistry get(DimensionDataStorage storage) {
        return storage.computeIfAbsent(
                new SavedData.Factory<CapitalRegistry>(() -> new CapitalRegistry(), CapitalRegistry::load, null),
                DATA_NAME);
    }

    /** Deserializes from NBT; public so the gametest round-trip can exercise it. */
    public static CapitalRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        CapitalRegistry registry = new CapitalRegistry();
        ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                registry.put(
                        ResourceLocation.parse(entry.getString("Tag")),
                        ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                                ResourceLocation.parse(entry.getString("Dimension"))),
                        new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
            } catch (Exception e) {
                // A malformed entry is skipped, not fatal - the locator can rebuild it.
            }
        }
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (CapitalEntry entry : entries.values()) {
            CompoundTag compound = new CompoundTag();
            compound.putString("Tag", entry.tagLocation().toString());
            compound.putString("Dimension", entry.dimension().location().toString());
            compound.putInt("X", entry.position().getX());
            compound.putInt("Y", entry.position().getY());
            compound.putInt("Z", entry.position().getZ());
            list.add(compound);
        }
        tag.put(ENTRIES_KEY, list);
        return tag;
    }

    /** Registers (or overwrites) a located capital. */
    public void put(ResourceLocation tagLocation, ResourceKey<Level> dimension, BlockPos position) {
        entries.put(tagLocation.toString(), new CapitalEntry(tagLocation, dimension, position));
        setDirty();
    }

    public Optional<CapitalEntry> get(ResourceLocation tagLocation) {
        return Optional.ofNullable(entries.get(tagLocation.toString()));
    }

    public Optional<CapitalEntry> get(String tagLocation) {
        return Optional.ofNullable(entries.get(tagLocation));
    }

    public boolean has(ResourceLocation tagLocation) {
        return entries.containsKey(tagLocation.toString());
    }

    public Map<String, CapitalEntry> all() {
        return Map.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }
}
