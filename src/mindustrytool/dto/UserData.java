package mindustrytool.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.Data;
import mindustrytool.features.chat.global.dto.ChatUser.SimpleRole;

@Data
public class UserData {
    private String id;
    private String name;
    private String imageUrl;
    private List<SimpleRole> roles;

    public Optional<SimpleRole> getHighestRole() {
        return getRoles().stream().max(Comparator.comparingInt(SimpleRole::getLevel));
    }

}
