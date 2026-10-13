package com.kordonlabs.plugin.nukkit.listener;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import com.kordonlabs.plugin.common.KordonCore;
import com.kordonlabs.plugin.common.login.LoginGate;
import com.kordonlabs.plugin.common.session.SessionTracker;
import com.kordonlabs.plugin.common.util.ThrottledLog;
import com.kordonlabs.plugin.nukkit.NukkitOnlinePlayer;
import com.kordonlabs.plugin.nukkit.NukkitPlatform;

/**
 * Tracks Nukkit / Lumi sessions: join and quit.
 */
public final class NukkitPlayerListener implements Listener {

    private final KordonCore core;
    private final NukkitPlatform platform;
    private final ThrottledLog log;

    public NukkitPlayerListener(KordonCore core, NukkitPlatform platform, ThrottledLog log) {
        this.core = core;
        this.platform = platform;
        this.log = log;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        try {
            if (core.stopped() || !core.active()) return;
            SessionTracker sessions = core.sessions();
            LoginGate gate = core.loginGate();
            if (sessions == null || gate == null) return;

            Player player = event.getPlayer();
            Object key = player.getLoginChainData() != null ? player.getLoginChainData() : player.getUniqueId();
            Long loginId = gate.takeLoginId(key);
            NukkitOnlinePlayer online = platform.wrap(player);
            if (online != null) sessions.onJoin(online, loginId);
        } catch (Throwable t) {
            failed("PlayerJoinEvent", t);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            Object key = player.getLoginChainData() != null ? player.getLoginChainData() : player.getUniqueId();
            SessionTracker sessions = sessions();
            if (sessions != null) sessions.onQuit(key);
        } catch (Throwable t) {
            failed("PlayerQuitEvent", t);
        }
    }

    private SessionTracker sessions() {
        return core.stopped() ? null : core.sessions();
    }

    private void failed(String event, Throwable t) {
        log.warn("listener." + event, "Kordon: " + event + " not tracked after an error: " + t);
    }
}
