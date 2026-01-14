package mindustrytool.features.chat.dto;

import java.util.Optional;

import arc.struct.Seq;

public class ChatUser {
    String name;
    String imageUrl;
    Seq<SimpleRole> roles;

    public ChatUser() {
    }

    public String name() {
        return name;
    }

    public String imageUrl() {
        return imageUrl;
    }

    public Seq<SimpleRole> roles() {
        return roles;
    }

    public Optional<SimpleRole> getHighestRole() {
        return Optional.ofNullable(roles().max(SimpleRole::level));
    }

    public static class SimpleRole {
        String id;
        String color;
        String icon;
        int level;

        public SimpleRole() {
        }

        public String id() {
            return id;
        }

        public String color() {
            return color;
        }

        public String icon() {
            return icon;
        }

        public int level() {
            return level;
        }
    }
}
