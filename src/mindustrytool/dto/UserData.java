package mindustrytool.dto;

import java.util.Optional;

import arc.struct.Seq;
import lombok.Data;
import lombok.experimental.Accessors;
import mindustrytool.features.chat.global.dto.ChatUser.SimpleRole;

@Data
@Accessors(chain = true, fluent = true)
public class UserData {
    private String id;
    private String name;
    private String imageUrl;
    private Seq<SimpleRole> roles;

    public Optional<SimpleRole> getHighestRole() {
        return Optional.ofNullable(roles().max(SimpleRole::level));
    }

}
