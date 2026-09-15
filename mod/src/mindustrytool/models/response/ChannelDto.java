package mindustrytool.models.response;

import lombok.Data;

@Data
public class ChannelDto {
	public String id;
	public String name;
	public String lastMessageId;
}
