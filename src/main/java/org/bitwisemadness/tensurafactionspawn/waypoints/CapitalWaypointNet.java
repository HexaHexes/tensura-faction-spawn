package org.bitwisemadness.tensurafactionspawn.waypoints;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.bitwisemadness.tensurafactionspawn.waypoints.client.CapitalWaypointClient;

/**
 * Registers the capital-waypoint payload. The handler body is dist-gated: on
 * the dedicated server the received payload is impossible (server-to-client
 * only), and on the client the work is handed to the client-side class via a
 * branch that never executes server-side, so Xaero classes never load there.
 */
public final class CapitalWaypointNet {

    private CapitalWaypointNet() {
    }

    /** Registers the payload handlers on the mod bus. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("tensura_faction_spawn");
        registrar.playToClient(CapitalWaypointPayload.TYPE, CapitalWaypointPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        CapitalWaypointClient.accept(payload.entries());
                    }
                }));
    }
}
