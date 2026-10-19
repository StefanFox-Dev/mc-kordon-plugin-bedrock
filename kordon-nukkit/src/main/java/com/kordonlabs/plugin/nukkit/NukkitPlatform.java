package com.kordonlabs.plugin.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import com.kordonlabs.plugin.common.login.LoginVerdict;
import com.kordonlabs.plugin.common.platform.KickCause;
import com.kordonlabs.plugin.common.platform.OnlinePlayer;
import com.kordonlabs.plugin.common.platform.Platform;
import com.kordonlabs.plugin.common.text.LocalizedLine;
import com.kordonlabs.plugin.nukkit.api.KordonLoginCheckedEvent;
import com.kordonlabs.plugin.nukkit.api.KordonRemoteKickEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The core's view of a Lumi / Nukkit server. Every method is thread-safe.
 */
public final class NukkitPlatform implements Platform {

    private final KordonNukkitPlugin plugin;
    private final Server server;
    private final Logger logger;

    public NukkitPlatform(KordonNukkitPlugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.logger = createLogger(plugin);
    }

    private static Logger createLogger(KordonNukkitPlugin plugin) {
        Logger log = Logger.getLogger("Kordon-" + System.identityHashCode(plugin));
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record == null) return;
                String msg = record.getMessage();
                if (msg == null) return;
                Throwable thrown = record.getThrown();
                Level lvl = record.getLevel();
                if (lvl == Level.SEVERE) {
                    if (thrown != null) plugin.getLogger().error(msg, thrown);
                    else plugin.getLogger().error(msg);
                } else if (lvl == Level.WARNING) {
                    if (thrown != null) plugin.getLogger().warning(msg, thrown);
                    else plugin.getLogger().warning(msg);
                } else if (lvl == Level.INFO) {
                    if (thrown != null) plugin.getLogger().info(msg, thrown);
                    else plugin.getLogger().info(msg);
                } else {
                    if (thrown != null) plugin.getLogger().debug(msg, thrown);
                    else plugin.getLogger().debug(msg);
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        return log;
    }

    @Override
    public String platformId() {
        return "velocity";
    }

    @Override
    public String serverVersion() {
        return "3.4.0-SNAPSHOT";
    }

    @Override
    public String pluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean onlineMode() {
        try {
            return server.getSettings().network().xboxAuth();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public int onlineCount() {
        return server.getOnlinePlayers().size();
    }

    @Override
    public Path dataFolder() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public Path pluginJar() {
        return plugin.getFile().toPath();
    }

    @Override
    public Path pluginsFolder() {
        return plugin.getFile().getParentFile().toPath();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    public NukkitOnlinePlayer wrap(Player player) {
        return player == null ? null : NukkitOnlinePlayer.of(player);
    }

    @Override
    public Collection<OnlinePlayer> onlinePlayers() {
        List<OnlinePlayer> list = new ArrayList<>();
        for (Player p : server.getOnlinePlayers().values()) {
            OnlinePlayer wrapped = wrap(p);
            if (wrapped != null) list.add(wrapped);
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public Optional<OnlinePlayer> player(UUID uuid) {
        if (uuid == null) return Optional.empty();
        for (Player p : server.getOnlinePlayers().values()) {
            if (uuid.equals(p.getUniqueId())) return Optional.ofNullable(wrap(p));
        }
        return Optional.empty();
    }

    @Override
    public Optional<OnlinePlayer> player(String name) {
        if (name == null || name.isEmpty()) return Optional.empty();
        Player p = server.getPlayerExact(name);
        if (p == null) p = server.getPlayer(name);
        return Optional.ofNullable(wrap(p));
    }

    @Override
    public void notifyStaff(String permission, String legacySection) {
        for (Player p : server.getOnlinePlayers().values()) {
            if (p.hasPermission(permission)) {
                p.sendMessage(legacySection);
            }
        }
    }

    @Override
    public void notifyStaff(String permission, LocalizedLine line) {
        if (!line.localized()) {
            notifyStaff(permission, line.fallback());
            return;
        }
        for (Player p : server.getOnlinePlayers().values()) {
            if (!p.hasPermission(permission)) continue;
            String locale = null;
            if (p.getLoginChainData() != null) {
                locale = p.getLoginChainData().getLanguageCode();
            }
            p.sendMessage(line.forLocale(locale));
        }
    }

    @Override
    public boolean fireRemoteKick(OnlinePlayer player, KickCause cause, String legacySection) {
        return fireRemoteKickMessage(player, cause, legacySection) != null;
    }

    @Override
    public String fireRemoteKickMessage(OnlinePlayer player, KickCause cause, String legacySection) {
        Player target = player instanceof NukkitOnlinePlayer wrapped ? wrapped.handle() : null;
        if (target == null) return legacySection;
        KordonRemoteKickEvent event = new KordonRemoteKickEvent(target, cause.name(), legacySection);
        try {
            server.getEventBus().call(event);
        } catch (Throwable t) {
            return legacySection;
        }
        if (event.isCancelled()) return null;
        String message = event.getMessage();
        return message == null ? legacySection : message;
    }

    @Override
    public LoginVerdict fireLoginChecked(Object connectionKey, LoginVerdict verdict) {
        if (verdict == null) return verdict;
        KordonLoginCheckedEvent event = new KordonLoginCheckedEvent(connectionKey, verdict);
        try {
            server.getEventBus().call(event);
        } catch (Throwable t) {
            return verdict;
        }
        return LoginVerdict.afterPublicEvent(verdict, event.isCancelled(), event.getKickMessage());
    }
}
