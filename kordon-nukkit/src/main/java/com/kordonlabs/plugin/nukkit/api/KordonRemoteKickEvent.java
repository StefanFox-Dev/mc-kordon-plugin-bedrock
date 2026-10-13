package com.kordonlabs.plugin.nukkit.api;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.Event;
import com.kordonlabs.plugin.common.text.Legacy;

/**
 * Fired when Kordon is about to disconnect an online player on the panel's order.
 *
 * <ul>
 *   <li>{@link #getCause()}: {@code PANEL} (the panel's "Kick from server" action), {@code BLACKLIST} (a blacklist
 *       entry matches the player), {@code GAME} ({@code /kordon blacklist add}).</li>
 *   <li>{@code setCancelled(true)} prevents the kick; Kordon does not try again for this session.</li>
 *   <li>{@link #setMessage(String)} changes the kick screen.</li>
 * </ul>
 *
 * <p>Handlers must be fast and thread-safe.
 */
public final class KordonRemoteKickEvent extends Event implements Cancellable {

    private final Player player;
    private final String cause;
    private volatile String message;
    private volatile boolean cancelled;

    public KordonRemoteKickEvent(Player player, String cause, String message) {
        this.player = player;
        this.cause = cause;
        this.message = message;
    }

    public Player getPlayer() {
        return player;
    }

    /** {@code PANEL}, {@code BLACKLIST} or {@code GAME}. */
    public String getCause() {
        return cause;
    }

    /** The kick screen ({@code §}-codes). */
    public String getMessage() {
        return message;
    }

    /** Replaces the kick screen ({@code §}- or {@code &}-codes); null keeps Kordon's text. */
    public void setMessage(String legacy) {
        this.message = legacy == null ? null : Legacy.translate(legacy);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /** {@code true} prevents the kick. */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public String toString() {
        return "KordonRemoteKickEvent{player=" + (player != null ? player.getName() : "null") + ", cause=" + cause
                + ", cancelled=" + cancelled + "}";
    }
}
