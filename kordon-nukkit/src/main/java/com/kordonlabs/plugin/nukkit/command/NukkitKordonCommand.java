package com.kordonlabs.plugin.nukkit.command;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginIdentifiableCommand;
import cn.nukkit.plugin.Plugin;
import com.kordonlabs.plugin.common.command.CommandService;
import com.kordonlabs.plugin.common.platform.Permissions;
import com.kordonlabs.plugin.common.text.MessageKey;
import com.kordonlabs.plugin.common.text.Messages;
import com.kordonlabs.plugin.common.util.ThrottledLog;
import com.kordonlabs.plugin.nukkit.KordonNukkitPlugin;
import com.kordonlabs.plugin.nukkit.NukkitSender;
import java.util.function.Supplier;

/**
 * The {@code /kordon} command for Lumi / Nukkit.
 */
public final class NukkitKordonCommand extends Command implements PluginIdentifiableCommand {

    private final KordonNukkitPlugin plugin;
    private final Supplier<CommandService> commands;
    private final Messages messages;
    private final ThrottledLog log;

    public NukkitKordonCommand(KordonNukkitPlugin plugin, Supplier<CommandService> commands, Messages messages, ThrottledLog log) {
        super("kordon", "Kordon moderation and security command", "/kordon", new String[0]);
        this.plugin = plugin;
        this.commands = commands;
        this.messages = messages;
        this.log = log;
        setPermission(Permissions.COMMAND);
        try {
            setPermissionMessage(messages.chat(MessageKey.CMD_NO_PERMISSION, "perm", Permissions.COMMAND));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        NukkitSender wrapped = new NukkitSender(sender);
        try {
            CommandService service = commands.get();
            if (service == null) {
                wrapped.send(messages.chat(MessageKey.CMD_ERR_NETWORK));
                return true;
            }
            service.execute(wrapped, args);
        } catch (Throwable t) {
            log.warn("command.kordon", "Kordon: /kordon failed: " + t);
            try {
                wrapped.send(messages.chat(MessageKey.CMD_ERR_NETWORK));
            } catch (Throwable ignored) {
            }
        }
        return true;
    }
}
