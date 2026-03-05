package mindustrytool.features.savesync.dto;

import java.util.List;

public class SyncSlotResponseDto {
    public List<DownloadDto> downloads;
    public List<String> missingHashes;
    public List<String> extraHashes;
}
