/*
 * Decompiled with CFR 0.152.
 */
package com.kordonlabs.plugin.common.api.dto;

public record ConnStats(Integer a, Integer b, Integer c, Integer d, Integer e, Boolean f) {
    public boolean any() {
        return this.a != null || this.b != null || this.c != null || this.d != null || Boolean.TRUE.equals(this.f);
    }

    public ConnStats forLogin() {
        ConnStats s = new ConnStats(this.a, this.b, this.c, null, null, this.f);
        return s.any() ? s : null;
    }
}

