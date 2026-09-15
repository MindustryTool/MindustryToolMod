package mindustrytool.models.response;

import lombok.Data;

@Data
public class ServerData {
	private String id;
	private String name;
	private String address;
	private int port;
	private int status;
}
