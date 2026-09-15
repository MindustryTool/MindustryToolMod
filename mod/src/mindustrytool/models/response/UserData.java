package mindustrytool.models.response;

import java.util.List;
import java.util.Optional;
import lombok.Data;

@Data
public class UserData {
	private String id;
	private String name;
	private String imageUrl;
	private List<ChatUser.SimpleRole> roles;

	public Optional<ChatUser.SimpleRole> getHighestRole() {
		if (roles == null || roles.isEmpty()) {
			return Optional.empty();
		}
		return getRoles().stream().max((a, b) -> Integer.compare(a.getLevel(), b.getLevel()));
	}
}
