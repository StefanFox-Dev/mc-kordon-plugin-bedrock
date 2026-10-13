package com.kordonlabs.plugin.nukkit;

import cn.nukkit.plugin.PluginBase;
import com.kordonlabs.plugin.common.KordonCore;
import com.kordonlabs.plugin.common.install.DuplicateJarCheck;
import com.kordonlabs.plugin.common.text.MessageKey;
import com.kordonlabs.plugin.common.text.Messages;
import com.kordonlabs.plugin.nukkit.command.NukkitKordonCommand;
import com.kordonlabs.plugin.nukkit.listener.NukkitLoginListener;
import com.kordonlabs.plugin.nukkit.listener.NukkitPlayerListener;
import java.time.Duration;

/**
 * Kordon's entry point on Lumi / Nukkit.
 */
public final class KordonNukkitPlugin extends PluginBase {

    private static final Duration FLUSH_BUDGET = Duration.ofSeconds(2);

    private volatile KordonCore core;
    private volatile NukkitKordonCommand command;
    private volatile NukkitLoginListener loginListener;
    private volatile NukkitPlayerListener playerListener;

    @Override
    public void onEnable() {
        KordonCore started = null;
        Messages texts = null;
        try {
            texts = Messages.load();
            NukkitPlatform platform = new NukkitPlatform(this);
            DuplicateJarCheck.run(getFile().getParentFile().toPath(), getFile().toPath(),
                    platform.logger(), texts);

            KordonCore c = new KordonCore(platform);
            core = c;
            c.start();
            if (!c.started()) return;
            started = c;
            register(c, platform);
            c.listenersReady();
        } catch (Throwable t) {
            try {
                String line = texts == null ? "Kordon failed to enable (" + t + ")"
                        : texts.console(MessageKey.LOG_START_FAILED, "error", String.valueOf(t));
                getLogger().error(line, t);
            } catch (Throwable ignored) {
            }
            unregisterAll();
            if (started != null) stopQuietly(started, Duration.ZERO);
            core = null;
        }
    }

    @Override
    public void onDisable() {
        KordonCore c = core;
        core = null;
        if (c != null) stopQuietly(c, FLUSH_BUDGET);
        unregisterAll();
    }

    private void register(KordonCore c, NukkitPlatform platform) {
        this.loginListener = new NukkitLoginListener(this, c, platform);
        this.playerListener = new NukkitPlayerListener(c, platform, c.log());
        getServer().getEventBus().subscribe(this.loginListener);
        getServer().getEventBus().subscribe(this.playerListener);

        this.command = new NukkitKordonCommand(this, c::commands, c.messages(), c.log());
        getServer().getCommandMap().register("kordon", this.command);
    }

    private void unregisterAll() {
        try {
            if (this.loginListener != null) {
                getServer().getEventBus().unsubscribe(this.loginListener);
                this.loginListener = null;
            }
            if (this.playerListener != null) {
                getServer().getEventBus().unsubscribe(this.playerListener);
                this.playerListener = null;
            }
            if (this.command != null) {
                this.command.unregister(getServer().getCommandMap());
                this.command = null;
            }
        } catch (Throwable ignored) {
        }
    }

    private void stopQuietly(KordonCore c, Duration budget) {
        try {
            c.stop(budget);
        } catch (Throwable t) {
            getLogger().warning("Kordon stop failed: " + t);
        }
    }
}
