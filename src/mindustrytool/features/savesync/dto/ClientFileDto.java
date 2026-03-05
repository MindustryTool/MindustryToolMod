package mindustrytool.features.savesync.dto;

import java.time.Instant;

public class ClientFileDto {
    public String path;
    public String hash;
    public Instant updatedAt;

    public ClientFileDto() {}

    public ClientFileDto(String path, String hash, Instant updatedAt) {
        this.path = path;
        this.hash = hash;
        this.updatedAt = updatedAt;
    }
}
