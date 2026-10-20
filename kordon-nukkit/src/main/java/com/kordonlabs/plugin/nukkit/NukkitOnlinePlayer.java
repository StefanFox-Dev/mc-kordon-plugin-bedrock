package com.kordonlabs.plugin.nukkit;

import cn.nukkit.Player;
import cn.nukkit.utils.LoginChainData;
import com.kordonlabs.plugin.common.api.dto.ClientSettings;
import com.kordonlabs.plugin.common.platform.OnlinePlayer;
import com.kordonlabs.plugin.common.platform.PlayerFacts;
import com.kordonlabs.plugin.common.text.Legacy;
import java.util.Locale;
import java.util.UUID;

/**
 * A connected Nukkit / Lumi player presented as Java Velocity. Thread-safe.
 */
public final class NukkitOnlinePlayer implements OnlinePlayer {

    private final Player player;
    private final String ip;

    private NukkitOnlinePlayer(Player player, String ip) {
        this.player = player;
        this.ip = ip;
    }

    public static NukkitOnlinePlayer of(Player player) {
        if (player == null) return null;
        String ip = null;
        LoginChainData chain = player.getLoginChainData();
        if (isWaterdogEnabled() && chain != null && chain.getWaterdogIP() != null && !chain.getWaterdogIP().isEmpty()) {
            ip = chain.getWaterdogIP();
        } else if (player.getSocketAddress() != null) {
            ip = PlayerFacts.ipOf(player.getSocketAddress());
        } else {
            ip = player.getAddress();
        }
        return ip == null ? null : new NukkitOnlinePlayer(player, ip);
    }

    private static boolean isWaterdogEnabled() {
        try {
            cn.nukkit.Server server = cn.nukkit.Server.getInstance();
            return server != null && server.getSettings().general().useWaterdog();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public Player handle() {
        return player;
    }

    public static UUID javaOfflineUuid(String name) {
        if (name == null || name.isEmpty()) return UUID.randomUUID();
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public UUID uuid() {
        return javaOfflineUuid(player.getName());
    }

    @Override
    public String name() {
        return player.getName();
    }

    @Override
    public String ip() {
        return ip;
    }

    @Override
    public int protocol() {
        return 767;
    }

    @Override
    public String backend() {
        return "survival";
    }

    @Override
    public String clientBrand() {
        return "vanilla";
    }

    @Override
    public String locale() {
        LoginChainData chain = player.getLoginChainData();
        if (chain == null) return "ru_ru";
        String lang = chain.getLanguageCode();
        if (lang == null || lang.isEmpty()) return "ru_ru";
        return lang.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    @Override
    public boolean bedrock() {
        return false;
    }

    @Override
    public ClientSettings clientSettings() {
        int viewDistance = player.getViewDistance();
        if (viewDistance <= 0) {
            viewDistance = 12;
        }
        return new ClientSettings(
                viewDistance,
                "shown",
                Boolean.TRUE,
                "right",
                ClientSettings.skinMask(true, true, true, true, true, true, true),
                Boolean.TRUE,
                Boolean.FALSE,
                "all"
        );
    }

    @Override
    public Object connectionKey() {
        LoginChainData chain = player.getLoginChainData();
        return chain != null ? chain : player;
    }

    @Override
    public boolean connected() {
        return player.isConnected();
    }

    @Override
    public boolean hasPermission(String node) {
        return player.hasPermission(node);
    }

    @Override
    public void sendMessage(String legacySection) {
        player.sendMessage(legacySection);
    }

    @Override
    public void disconnect(String legacySection) {
        String msg = legacySection != null ? Legacy.strip(legacySection) : "";
        player.close(msg, msg);
    }

    @Override
    public String toString() {
        return "VelocityOnlinePlayer{" + player.getName() + "}";
    }
}
