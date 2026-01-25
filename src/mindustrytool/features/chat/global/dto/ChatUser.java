package mindustrytool.features.chat.global.dto;

import java.util.List;
import java.util.Optional;

import lombok.Data;

@Data
public class ChatUser {
    private String name;
    private String imageUrl;
    private List<SimpleRole> roles;

    public Optional<SimpleRole> getHighestRole() {
        if (roles == null || roles.isEmpty()) {
            return Optional.empty();
        }

        return getRoles().stream().max((a, b) -> Integer.compare(a.getLevel(), b.getLevel()));
    }

    @Data
    public static class SimpleRole {
        String id;
        String color;
        String icon;
        int level;
    }
}
