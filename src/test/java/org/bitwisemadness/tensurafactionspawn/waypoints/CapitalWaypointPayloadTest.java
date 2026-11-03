package org.bitwisemadness.tensurafactionspawn.waypoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The waypoint payload's wire format roundtrips. */
class CapitalWaypointPayloadTest {

    @Test
    void payloadSurvivesTheWire() {
        var entries = List.of(
                new CapitalWaypointPayload.Entry("minecraft:overworld", 120, 70, -354, "Goblins Capital"),
                new CapitalWaypointPayload.Entry("tensura:hell", -12, 40, 88, "Hell Capital"));
        var payload = new CapitalWaypointPayload(entries);

        var buf = Unpooled.buffer();
        CapitalWaypointPayload.STREAM_CODEC.encode(buf, payload);
        CapitalWaypointPayload decoded = CapitalWaypointPayload.STREAM_CODEC.decode(buf);
        assertEquals(0, buf.readableBytes());

        assertEquals(2, decoded.entries().size());
        assertEquals("minecraft:overworld", decoded.entries().get(0).dimension());
        assertEquals("Goblins Capital", decoded.entries().get(0).name());
        assertEquals(-354, decoded.entries().get(0).z());
        assertEquals("tensura:hell", decoded.entries().get(1).dimension());
        assertTrue(decoded.entries().get(1).x() == -12);
    }

    @Test
    void emptyPayloadSurvivesTheWire() {
        var payload = new CapitalWaypointPayload(List.of());
        var buf = Unpooled.buffer();
        CapitalWaypointPayload.STREAM_CODEC.encode(buf, payload);
        CapitalWaypointPayload decoded = CapitalWaypointPayload.STREAM_CODEC.decode(buf);
        assertTrue(decoded.entries().isEmpty());
    }
}
