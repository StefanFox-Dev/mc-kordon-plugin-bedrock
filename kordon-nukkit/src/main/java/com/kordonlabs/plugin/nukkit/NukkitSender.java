package com.kordonlabs.plugin.nukkit;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import com.kordonlabs.plugin.common.platform.Sender;
import com.kordonlabs.plugin.common.text.Legacy;
import java.util.UUID;

/**
 * Whoever typed {@code /kordon}: a player, or the console.
 */
public final class NukkitSender implements Sender {

    private final CommandSender sender;
    private final Player player;

    public NukkitSender(CommandSender sender) {
        this.sender = sender;
        this.player = sender instanceof Player p ? p : null;
    }

    @Override
    public String name() {
        return player != null ? player.getName() : "console";
    }

    @Override
    public UUID uuid() {
        return player != null ? player.getUniqueId() : null;
    }

    @Override
    public boolean console() {
        return player == null;
    }

    @Override
    public boolean hasPermission(String node) {
        return sender.hasPermission(node);
    }

    @Override
    public void send(String legacySection) {
        if (player != null) {
            player.sendMessage(legacySection);
        } else {
            sender.sendMessage(Legacy.strip(legacySection));
        }
    }

    @Override
    public void sendLink(String legacySection, String url, String hoverLegacySection) {
        if (player != null) {
            player.sendMessage(legacySection + " §7(" + url + "§7)");
        } else {
            sender.sendMessage(Legacy.strip(legacySection) + " " + url);
        }
    }
}
