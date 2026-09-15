package mindustrytool.models.response;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerConnectRoomsResponse {
    private List<PlayerConnectRoom> rooms;
}
