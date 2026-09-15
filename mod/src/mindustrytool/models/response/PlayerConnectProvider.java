package mindustrytool.models.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerConnectProvider {
	private String id;
	private String name;
	private String address;
}
