/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.connection.PendingConnection
 *  net.md_5.bungee.api.connection.ProxiedPlayer
 */
package com.kordonlabs.plugin.bungee.compat;

import com.kordonlabs.plugin.bungee.compat.Reflect;
import com.kordonlabs.plugin.common.api.dto.ConnStats;
import com.kordonlabs.plugin.common.compat.ConnStatsReader;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public final class BungeeConn {
    private static final ClassValue<Optional<Field>> CH_FIELD = new ClassValue<Optional<Field>>(){

        @Override
        protected Optional<Field> computeValue(Class<?> type) {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField("ch");
                    f.setAccessible(true);
                    return Optional.of(f);
                }
                catch (Throwable throwable) {
                    continue;
                }
            }
            return Optional.empty();
        }
    };
    private static final ConcurrentHashMap<Class<?>, Boolean> GETTER_ABSENT = new ConcurrentHashMap<>();

    private BungeeConn() {
    }

    public static Object channel(Object connection) {
        if (connection == null) {
            return null;
        }
        try {
            Field f;
            Object wrapper = null;
            if (!GETTER_ABSENT.containsKey(connection.getClass())) {
                try {
                    wrapper = Reflect.call(connection, "getCh");
                }
                catch (NoSuchMethodException absent) {
                    GETTER_ABSENT.put(connection.getClass(), Boolean.TRUE);
                }
            }
            if (wrapper == null && (f = CH_FIELD.get(connection.getClass()).orElse(null)) != null) {
                wrapper = f.get(connection);
            }
            return wrapper == null ? null : Reflect.call(wrapper, "getHandle");
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static ConnStats loginStats(PendingConnection connection, String ip) {
        return ConnStatsReader.stats(BungeeConn.channel(connection), ip, -1L, 0);
    }

    public static ConnStats stats(ProxiedPlayer player, String ip) {
        long ping;
        if (player == null) {
            return null;
        }
        try {
            ping = player.getPing();
        }
        catch (RuntimeException e) {
            ping = -1L;
        }
        return ConnStatsReader.stats(BungeeConn.channel(player), ip, ping, 1);
    }
}

