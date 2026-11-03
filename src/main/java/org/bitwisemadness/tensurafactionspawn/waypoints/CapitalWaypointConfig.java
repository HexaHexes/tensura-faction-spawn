package org.bitwisemadness.tensurafactionspawn.waypoints;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config for the capital-waypoint feature. The server always sends the
 * capital list (harmless when the client ignores it); the toggle gates the
 * client-side Xaero registration.
 */
public final class CapitalWaypointConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Offer a map waypoint for every located faction capital on login.",
                     "Already-offered capitals are never re-offered, and deleting",
                     "the waypoint in Xaero keeps it deleted.")
            .define("capitalWaypoints.enabled", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CapitalWaypointConfig() {
    }

    public static boolean enabled() {
        return ENABLED.get();
    }
}
