package org.bitwisemadness.tensurafactionspawn.waypoints;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-client login sync: every located capital as a waypoint candidate.
 * The client registers each into Xaero's waypoint set once per player per
 * world (the offers file tracks what was already offered; deletions stick).
 */
public record CapitalWaypointPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.parse("tensura_faction_spawn:capital_waypoints");
    public static final Type<CapitalWaypointPayload> TYPE = new Type<>(ID);

    /** One capital: its dimension, anchor position and display name. */
    public record Entry(String dimension, int x, int y, int z, String name) {

        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::dimension,
                ByteBufCodecs.VAR_INT, Entry::x,
                ByteBufCodecs.VAR_INT, Entry::y,
                ByteBufCodecs.VAR_INT, Entry::z,
                ByteBufCodecs.STRING_UTF8, Entry::name,
                Entry::new);
    }

    private static final StreamCodec<ByteBuf, List<Entry>> LIST_CODEC =
            ByteBufCodecs.collection(ArrayList::new, Entry.STREAM_CODEC);

    public static final StreamCodec<ByteBuf, CapitalWaypointPayload> STREAM_CODEC =
            LIST_CODEC.map(CapitalWaypointPayload::new, p -> new ArrayList<>(p.entries()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
