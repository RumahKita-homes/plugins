/*
 * Decompiled with CFR 0.152.
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.RumahKitaGuildsPlugin;

public enum GuildRole {
    LEADER(3),
    ADMIN(2),
    MEMBER(1);

    private final int power;

    private GuildRole(int power) {
        this.power = power;
    }

    public boolean atLeast(GuildRole other) {
        return this.power >= other.power;
    }

    public static GuildRole fromString(String value) {
        if (value == null) {
            return MEMBER;
        }
        try {
            return GuildRole.valueOf(value.toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            return MEMBER;
        }
    }

    public String displayName(RumahKitaGuildsPlugin plugin) {
        switch (this.ordinal()) {
            case 0: return plugin.getConfig().getString("placeholder.role-leader", "Leader");
            case 1: return plugin.getConfig().getString("placeholder.role-admin", "Admin");
            case 2: return plugin.getConfig().getString("placeholder.role-member", "Member");
            default: return "Member";
        }
    }
}

