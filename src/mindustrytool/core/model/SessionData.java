package mindustrytool.core.model;

import arc.struct.Seq;

/** Session data from /api/v4/auth/session */
public class SessionData {
    public String id;
    public String name;
    public String imageUrl;
    public Seq<Role> roles;
    public Stats stats;

    public String id() { return id; }
    public String name() { return name; }
    public int credit() { return stats != null ? stats.credit : 0; }
    public Role topRole() { return roles != null && roles.size > 0 ? roles.first() : null; }

    public static class Role {
        public String id;
        public String description;
        public String color;
        public String icon;
        public int level;
    }

    public static class Stats { public int credit; }
}
