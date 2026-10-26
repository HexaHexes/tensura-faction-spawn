package org.bitwisemadness.tensurafactionspawn.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * When a capital becomes the player's respawn point.
 *
 * <p>{@code IF_UNSET} is the default: a first pick gets a home (the player has
 * no bed yet), while a later reincarnation keeps the base they already built.
 * In a dimension where beds do not work - Tensura's hell, for one -
 * {@code IF_UNSET} skips entirely rather than store a point vanilla will
 * ignore; {@code ALWAYS} still stores, ineffective there but explicitly
 * requested by the pack.</p>
 */
public enum RespawnMode {
    NEVER,
    IF_UNSET,
    ALWAYS;

    /**
     * Parses a datapack value. Legacy booleans are accepted for back-compat:
     * {@code true} means {@code ALWAYS}, {@code false} means {@code NEVER}.
     */
    public static RespawnMode fromJson(JsonElement element) {
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean() ? ALWAYS : NEVER;
        }
        if (primitive.isString()) {
            switch (primitive.getAsString()) {
                case "never": return NEVER;
                case "if_unset": return IF_UNSET;
                case "always": return ALWAYS;
                default:
                    throw new IllegalArgumentException("set_respawn must be never|if_unset|always or a boolean, got '"
                            + primitive.getAsString() + "'");
            }
        }
        throw new IllegalArgumentException("set_respawn must be never|if_unset|always or a boolean");
    }
}
