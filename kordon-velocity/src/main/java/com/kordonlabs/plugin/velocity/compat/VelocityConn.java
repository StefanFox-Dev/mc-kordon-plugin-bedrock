/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.velocitypowered.api.proxy.Player
 */
package com.kordonlabs.plugin.velocity.compat;

import com.kordonlabs.plugin.common.api.dto.ConnStats;
import com.kordonlabs.plugin.common.compat.ConnStatsReader;
import com.velocitypowered.api.proxy.Player;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class VelocityConn {
    private static final MethodType GETTER = MethodType.methodType(Object.class, Object.class);
    private static final ClassValue<ConcurrentHashMap<String, Optional<MethodHandle>>> GETTERS = new ClassValue<ConcurrentHashMap<String, Optional<MethodHandle>>>(){

        @Override
        protected ConcurrentHashMap<String, Optional<MethodHandle>> computeValue(Class<?> type) {
            return new ConcurrentHashMap<String, Optional<MethodHandle>>();
        }
    };

    private VelocityConn() {
    }

    public static Object channel(Player player) {
        if (player == null) {
            return null;
        }
        try {
            Object connection = VelocityConn.call(player, "getConnection");
            return connection == null ? null : VelocityConn.call(connection, "getChannel");
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static ConnStats stats(Player player, String ip) {
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
        return ConnStatsReader.stats(VelocityConn.channel(player), ip, ping, 1);
    }

    private static Object call(Object target, String name) throws Throwable {
        MethodHandle handle = GETTERS.get(target.getClass()).computeIfAbsent(name, n -> Optional.ofNullable(VelocityConn.resolve(target.getClass(), n))).orElse(null);
        if (handle == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "." + name + "()");
        }
        return handle.invokeExact(target);
    }

    private static MethodHandle resolve(Class<?> type, String name) {
        try {
            Method m = type.getMethod(name);
            if (Modifier.isStatic(m.getModifiers()) || m.getReturnType() == Void.TYPE) {
                return null;
            }
            return MethodHandles.publicLookup().unreflect(m).asType(GETTER);
        }
        catch (Throwable absent) {
            return null;
        }
    }
}

