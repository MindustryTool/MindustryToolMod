package mindustrytool.features.savesync;

import arc.Core;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustrytool.Main;
import mindustrytool.Utils;
import mindustrytool.features.savesync.dto.ClientFileDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileService {
    public List<ClientFileDto> listFiles() {
        Seq<Fi> files = new Seq<>();
        files.add(Core.settings.getSettingsFile());
        files.add(Core.settings.getBackupSettingsFile());
        files.addAll(Vars.customMapDirectory.list());
        files.addAll(Vars.saveDirectory.list());
        files.addAll(Vars.schematicDirectory.list());

        String basePath = Vars.dataDirectory.absolutePath();
        List<ClientFileDto> result = new ArrayList<>();
        for (Fi file : files) {
            ClientFileDto clientFile = toClientFile(file, basePath);
            if (clientFile != null) {
                result.add(clientFile);
            }
        }
        return result;
    }

    public Fi getFile(String path) {
        return Vars.dataDirectory.child(path);
    }

    public SaveSyncFileChanges compareFiles(List<ClientFileDto> previousFiles, List<ClientFileDto> currentFiles) {
        Map<String, ClientFileDto> previousByPath = indexByPath(previousFiles);
        Map<String, ClientFileDto> currentByPath = indexByPath(currentFiles);
        List<ClientFileDto> createdFiles = new ArrayList<>();
        List<ClientFileDto> updatedFiles = new ArrayList<>();
        List<ClientFileDto> deletedFiles = new ArrayList<>();

        for (ClientFileDto currentFile : currentFiles) {
            ClientFileDto previousFile = previousByPath.get(currentFile.getPath());
            if (previousFile == null) {
                createdFiles.add(currentFile);
                continue;
            }

            if (!currentFile.getHash().equals(previousFile.getHash())) {
                updatedFiles.add(currentFile);
            }
        }

        for (ClientFileDto previousFile : previousFiles) {
            if (!currentByPath.containsKey(previousFile.getPath())) {
                deletedFiles.add(previousFile);
            }
        }

        return new SaveSyncFileChanges(createdFiles, updatedFiles, deletedFiles);
    }

    public List<Fi> resolveFilesForHashes(List<ClientFileDto> files, List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Fi> filesByHash = new HashMap<>();
        for (ClientFileDto file : files) {
            Fi localFile = getFile(file.getPath());
            if (localFile.exists() && !filesByHash.containsKey(file.getHash())) {
                filesByHash.put(file.getHash(), localFile);
            }
        }

        List<Fi> resolvedFiles = new ArrayList<>();
        for (String hash : hashes) {
            Fi localFile = filesByHash.get(hash);
            if (localFile != null) {
                resolvedFiles.add(localFile);
            }
        }
        return resolvedFiles;
    }

    public ClientFileDto findFileByHash(List<ClientFileDto> files, String hash) {
        for (ClientFileDto file : files) {
            if (file.getHash().equals(hash)) {
                return file;
            }
        }
        return null;
    }

    public void writeFile(String path, byte[] bytes) {
        getFile(path).writeBytes(bytes);
    }

    public void deleteFile(String path) {
        getFile(path).delete();
    }

    public boolean shouldSkipDownload(String path) {
        return Main.self.file.path().endsWith(path);
    }

    private ClientFileDto toClientFile(Fi file, String basePath) {
        if (file.isDirectory()) {
            return null;
        }

        try {
            String relativePath = normalizeRelativePath(file, basePath);
            String hash = Utils.sha256(file.file());
            return new ClientFileDto(relativePath, hash, Instant.ofEpochMilli(file.lastModified()));
        } catch (Exception e) {
            Log.err(e);
            return null;
        }
    }

    private String normalizeRelativePath(Fi file, String basePath) {
        String relativePath = file.absolutePath().substring(basePath.length());
        relativePath = relativePath.replace('\\', '/');
        if (relativePath.startsWith("/")) {
            return relativePath.substring(1);
        }
        return relativePath;
    }

    private Map<String, ClientFileDto> indexByPath(List<ClientFileDto> files) {
        Map<String, ClientFileDto> indexedFiles = new HashMap<>();
        for (ClientFileDto file : files) {
            indexedFiles.put(file.getPath(), file);
        }
        return indexedFiles;
    }
}
