package com.kordonlabs.plugin.nukkit.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.player.PlayerAsyncPreLoginEvent;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.LoginChainData;
import com.kordonlabs.plugin.common.KordonCore;
import com.kordonlabs.plugin.common.login.HeldLogin;
import com.kordonlabs.plugin.common.login.LoginAttempt;
import com.kordonlabs.plugin.common.login.LoginHold;
import com.kordonlabs.plugin.common.platform.Platform;
import com.kordonlabs.plugin.common.platform.PlayerFacts;
import com.kordonlabs.plugin.common.text.Legacy;
import com.kordonlabs.plugin.common.text.MessageKey;
import com.kordonlabs.plugin.common.text.Messages;
import com.kordonlabs.plugin.common.util.ThrottledLog;
import com.kordonlabs.plugin.nukkit.KordonNukkitPlugin;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checks logins during {@link PlayerAsyncPreLoginEvent}.
 * Handled via Lumi EventBus without deprecated Listener interface.
 */
public final class NukkitLoginListener {

    private final KordonNukkitPlugin plugin;
    private final HeldLogin.Core core;
    private final ThrottledLog log;
    private final Messages messages;
    private final AtomicBoolean notInetLogged = new AtomicBoolean();

    public NukkitLoginListener(KordonNukkitPlugin plugin, KordonCore core, Platform platform) {
        this(plugin, HeldLogin.coreOf(core, (key, verdict, deadline) -> platform.fireLoginChecked(key, verdict)),
                core.log(), core.messages());
    }

    NukkitLoginListener(KordonNukkitPlugin plugin, HeldLogin.Core core, ThrottledLog log, Messages messages) {
        this.plugin = plugin;
        this.core = core;
        this.log = log;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(PlayerAsyncPreLoginEvent event) {
        try {
            check(event);
        } catch (Throwable t) {
            log.warn("listener.login", "Kordon: login check skipped after an error: " + t);
        }
    }

    private void check(PlayerAsyncPreLoginEvent event) {
        if (event.getLoginResult() == PlayerAsyncPreLoginEvent.LoginResult.KICK) return;
        HeldLogin.Gate gate = core.gate();
        if (gate == null) return;

        LoginChainData chain = event.getChainData();
        String name = event.getName();
        String ip = null;
        if (isWaterdogEnabled() && chain != null && chain.getWaterdogIP() != null && !chain.getWaterdogIP().isEmpty()) {
            ip = chain.getWaterdogIP();
        } else {
            ip = event.getAddress();
        }
        if (ip != null) {
            int scope = ip.indexOf('%');
            if (scope >= 0) ip = ip.substring(0, scope);
            ip = ip.strip();
        }

        if (ip == null || ip.isEmpty()) {
            if (notInetLogged.compareAndSet(false, true)) {
                log.warn(messages.console(MessageKey.LOG_NOT_INET, "name", Messages.literal(name)));
            }
            return;
        }

        UUID uuid = event.getUuid();
        Object key = chain != null ? chain : uuid;

        InetSocketAddress virtualHost = null;
        if (chain != null && chain.getServerAddress() != null && !chain.getServerAddress().isEmpty()) {
            String addr = chain.getServerAddress();
            int colon = addr.indexOf(':');
            if (colon > 0) {
                try {
                    virtualHost = new InetSocketAddress(addr.substring(0, colon), Integer.parseInt(addr.substring(colon + 1)));
                } catch (Throwable ignored) {
                    virtualHost = new InetSocketAddress(addr.substring(0, colon), 19132);
                }
            } else {
                virtualHost = new InetSocketAddress(addr, 19132);
            }
        }

        LoginAttempt attempt = new LoginAttempt(
                key,
                name,
                uuid,
                ip,
                ProtocolInfo.CURRENT_PROTOCOL,
                null,
                true,
                PlayerFacts.joinHost(virtualHost)
        );

        Target target = new Target(event, attempt, gate);
        try {
            gate.hold(attempt, target);
            target.await();
        } catch (Throwable t) {
            target.fail(t);
        }
    }

    private final class Target extends HeldLogin {

        private final PlayerAsyncPreLoginEvent event;
        private final CountDownLatch latch = new CountDownLatch(1);

        Target(PlayerAsyncPreLoginEvent event, LoginAttempt attempt, HeldLogin.Gate gate) {
            super(attempt, gate, NukkitLoginListener.this.core, NukkitLoginListener.this.log);
            this.event = event;
        }

        @Override
        protected void deny(String kickMessage) {
            String msg = kickMessage != null ? kickMessage : "Connection refused by Kordon";
            event.disAllow(Legacy.strip(msg));
        }

        @Override
        protected void resume() {
            latch.countDown();
        }

        void await() {
            try {
                latch.await(LoginHold.WATCHDOG.toMillis() + 500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean isWaterdogEnabled() {
        try {
            cn.nukkit.Server server = cn.nukkit.Server.getInstance();
            return server != null && server.getSettings().general().useWaterdog();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
