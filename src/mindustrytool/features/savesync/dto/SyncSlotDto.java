package mindustrytool.features.savesync.dto;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class SyncSlotDto {
    public Instant lastSync;
    public List<ClientFileDto> clientFiles;
}
