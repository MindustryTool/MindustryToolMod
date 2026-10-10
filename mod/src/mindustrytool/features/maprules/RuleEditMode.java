package mindustrytool.features.maprules;

import mindustry.Vars;

/**
 * Editing authority for map rules based on network context.
 */
public enum RuleEditMode {
    SINGLE_PLAYER,
    HOST,
    CLIENT_ADMIN,
    CLIENT_READONLY;

    public static RuleEditMode resolve() {
        if (Vars.net == null || !Vars.net.active()) {
            return SINGLE_PLAYER;
        }
        if (Vars.net.server()) {
            return HOST;
        }
        if (Vars.player != null && Vars.player.admin) {
            return CLIENT_ADMIN;
        }
        return CLIENT_READONLY;
    }

    public boolean canEditGameplay() {
        return this == SINGLE_PLAYER || this == HOST || this == CLIENT_ADMIN;
    }

    public boolean isLocalVisualOnly() {
        return this == CLIENT_READONLY;
    }
}
