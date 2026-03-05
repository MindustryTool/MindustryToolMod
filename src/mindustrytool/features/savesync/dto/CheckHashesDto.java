package mindustrytool.features.savesync.dto;

import java.util.List;

public class CheckHashesDto {
    public List<String> hashes;

    public CheckHashesDto() {}

    public CheckHashesDto(List<String> hashes) {
        this.hashes = hashes;
    }
}
