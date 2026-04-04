package mindustrytool.features.savesync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mindustrytool.features.savesync.dto.ClientFileDto;

public class SaveSyncFileChanges {
    private final List<ClientFileDto> createdFiles;
    private final List<ClientFileDto> updatedFiles;
    private final List<ClientFileDto> deletedFiles;

    public SaveSyncFileChanges(List<ClientFileDto> createdFiles, List<ClientFileDto> updatedFiles,
            List<ClientFileDto> deletedFiles) {
        this.createdFiles = Collections.unmodifiableList(new ArrayList<>(createdFiles));
        this.updatedFiles = Collections.unmodifiableList(new ArrayList<>(updatedFiles));
        this.deletedFiles = Collections.unmodifiableList(new ArrayList<>(deletedFiles));
    }

    public List<ClientFileDto> getCreatedFiles() {
        return createdFiles;
    }

    public List<ClientFileDto> getUpdatedFiles() {
        return updatedFiles;
    }

    public List<ClientFileDto> getDeletedFiles() {
        return deletedFiles;
    }

    public boolean hasChanges() {
        return !createdFiles.isEmpty() || !updatedFiles.isEmpty() || !deletedFiles.isEmpty();
    }
}
