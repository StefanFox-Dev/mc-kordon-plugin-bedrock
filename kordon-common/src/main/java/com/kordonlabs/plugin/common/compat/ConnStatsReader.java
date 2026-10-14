/*
 * Decompiled with CFR 0.152.
 */
package com.kordonlabs.plugin.common.compat;

import com.kordonlabs.plugin.common.api.dto.ConnStats;
import com.kordonlabs.plugin.common.platform.PlayerFacts;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnStatsReader {
    private static final MethodType GETTER = MethodType.methodType(Object.class, Object.class);
    private static final ClassValue<ConcurrentHashMap<String, Optional<MethodHandle>>> GETTERS = new ClassValue<ConcurrentHashMap<String, Optional<MethodHandle>>>(){

        @Override
        protected ConcurrentHashMap<String, Optional<MethodHandle>> computeValue(Class<?> type) {
            return new ConcurrentHashMap<String, Optional<MethodHandle>>();
        }
    };

    private ConnStatsReader() {
    }

    public static TcpInfo read(Object channel) {
        if (channel == null) {
            return null;
        }
        try {
            Object info = ConnStatsReader.call(channel, "tcpInfo");
            if (info == null) {
                return null;
            }
            Long rtt = ConnStatsReader.number(ConnStatsReader.call(info, "rtt"));
            Long rttVar = ConnStatsReader.number(ConnStatsReader.call(info, "rttvar"));
            Long mss = ConnStatsReader.number(ConnStatsReader.call(info, "sndMss"));
            if (rtt == null || mss == null) {
                return null;
            }
            return new TcpInfo(rtt, rttVar == null ? 0L : rttVar, mss);
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static String remoteIp(Object channel) {
        if (channel == null) {
            return null;
        }
        try {
            String string;
            Object address = ConnStatsReader.call(channel, "remoteAddress");
            if (address instanceof SocketAddress) {
                SocketAddress sa = (SocketAddress)address;
                string = PlayerFacts.ipOf(sa);
            } else {
                string = null;
            }
            return string;
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static ConnStats stats(Object channel, String playerIp, long pingMs, int pings) {
        TcpInfo tcp = ConnStatsReader.read(channel);
        String peer = ConnStatsReader.remoteIp(channel);
        Boolean relay = peer != null && playerIp != null && !peer.equals(playerIp) ? Boolean.TRUE : null;
        Integer ping = pingMs > 0L ? Integer.valueOf(ConnStatsReader.clamp(pingMs)) : null;
        ConnStats s = new ConnStats(tcp == null ? null : Integer.valueOf(ConnStatsReader.clamp(ConnStatsReader.toMillis(tcp.rttMicros()))), tcp == null ? null : Integer.valueOf(ConnStatsReader.clamp(ConnStatsReader.toMillis(tcp.rttVarMicros()))), tcp == null || tcp.sndMss() <= 0L ? null : Integer.valueOf(ConnStatsReader.clamp(tcp.sndMss())), ping, ping == null ? null : Integer.valueOf(Math.max(1, pings)), relay);
        return s.any() ? s : null;
    }

    static long toMillis(long micros) {
        return (Math.max(0L, micros) + 500L) / 1000L;
    }

    private static int clamp(long value) {
        return (int)Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }

    private static Long number(Object value) {
        Long l;
        if (value instanceof Number) {
            Number n = (Number)value;
            l = n.longValue();
        } else {
            l = null;
        }
        return l;
    }

    static Object call(Object target, String name) throws Throwable {
        MethodHandle handle = GETTERS.get(target.getClass()).computeIfAbsent(name, n -> Optional.ofNullable(ConnStatsReader.resolve(target.getClass(), n))).orElse(null);
        if (handle == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "." + name + "()");
        }
        return handle.invokeExact(target);
    }

    private static MethodHandle resolve(Class<?> type, String name) {
        Method found;
        try {
            found = type.getMethod(name);
        } catch (Throwable absent) {
            return null;
        }
        if (Modifier.isStatic(found.getModifiers()) || found.getReturnType() == Void.TYPE) {
            return null;
        }
        MethodHandle handle = ConnStatsReader.unreflect(found);
        if (handle != null) {
            return handle;
        }
        ArrayDeque<Class<?>> pending = new ArrayDeque<>();
        pending.add(type);
        while (!pending.isEmpty()) {
            Class<?> c = pending.poll();
            if (Modifier.isPublic(c.getModifiers())) {
                try {
                    Method m = c.getDeclaredMethod(name);
                    if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && (handle = ConnStatsReader.unreflect(m)) != null) {
                        return handle;
                    }
                } catch (Throwable throwable) {
                    // empty catch block
                }
            }
            if (c.getSuperclass() != null) {
                pending.add(c.getSuperclass());
            }
            pending.addAll(Arrays.asList(c.getInterfaces()));
        }
        return null;
    }

    private static MethodHandle unreflect(Method m) {
        try {
            return MethodHandles.publicLookup().unreflect(m).asType(GETTER);
        }
        catch (Throwable inaccessible) {
            return null;
        }
    }

    public record TcpInfo(long rttMicros, long rttVarMicros, long sndMss) {
    }
}

