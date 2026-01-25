package mindustrytool.features.chat.global.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.Data;

@Data
public class ChatUser {
    private String name;
    private String imageUrl;
    private List<SimpleRole> roles;

    public Optional<SimpleRole> getHighestRole() {
        return getRoles().stream().max(Comparator.comparingInt(SimpleRole::getLevel));
    }

    @Data
    public static class SimpleRole {
        String id;
        String color;
        String icon;
        int level;
    }
}
