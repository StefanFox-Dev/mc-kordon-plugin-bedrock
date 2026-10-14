package com.kordonlabs.plugin.nukkit;

import cn.nukkit.Player;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.LoginChainData;
import com.kordonlabs.plugin.common.api.dto.ClientSettings;
import com.kordonlabs.plugin.common.platform.OnlinePlayer;
import com.kordonlabs.plugin.common.platform.PlayerFacts;
import com.kordonlabs.plugin.common.text.Legacy;
import java.util.Locale;
import java.util.UUID;

/**
 * A connected Nukkit / Lumi player. Thread-safe.
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

    @Override
    public UUID uuid() {
        return player.getUniqueId();
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
        return player.protocol > 0 ? player.protocol : ProtocolInfo.CURRENT_PROTOCOL;
    }

    @Override
    public String backend() {
        return null;
    }

    @Override
    public String clientBrand() {
        LoginChainData chain = player.getLoginChainData();
        return chain != null ? chain.getDeviceModel() : null;
    }

    @Override
    public String locale() {
        LoginChainData chain = player.getLoginChainData();
        if (chain == null) return null;
        String lang = chain.getLanguageCode();
        if (lang == null || lang.isEmpty()) return null;
        try {
            return PlayerFacts.localeTag(Locale.forLanguageTag(lang.replace('_', '-')));
        } catch (Throwable t) {
            return lang;
        }
    }

    @Override
    public boolean bedrock() {
        return true;
    }

    @Override
    public ClientSettings clientSettings() {
        int viewDistance = player.getViewDistance();
        LoginChainData chain = player.getLoginChainData();
        if (viewDistance <= 0 && chain != null) {
            try {
                com.google.gson.JsonObject raw = chain.getRawData();
                if (raw != null && raw.has("MaxViewDistance")) {
                    viewDistance = raw.get("MaxViewDistance").getAsInt();
                }
            } catch (Throwable ignored) {
            }
        }
        if (viewDistance <= 0) {
            viewDistance = 8;
        }

        boolean hasCape = chain != null && chain.getCapeData() != null && !chain.getCapeData().isEmpty();
        if (!hasCape) {
            try {
                Object skin = player.getSkin();
                if (skin != null) {
                    java.lang.reflect.Method getCapeData = skin.getClass().getMethod("getCapeData");
                    Object cape = getCapeData.invoke(skin);
                    hasCape = cape != null;
                }
            } catch (Throwable ignored) {
            }
        }

        int skinParts = ClientSettings.skinMask(hasCape, true, true, true, true, true, true);
        return new ClientSettings(
                viewDistance,
                "shown",
                Boolean.TRUE,
                "right",
                skinParts,
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
        return "NukkitOnlinePlayer{" + player.getName() + "}";
    }
}
