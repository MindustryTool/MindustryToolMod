package mindustrytool.features.modmanager;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.util.Nullable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;
import mindustrytool.Main;
import mindustrytool.features.modmanager.PackModel.FileEntry;
import mindustrytool.features.modmanager.PackModel.ModEntry;
import mindustrytool.features.modmanager.PackModel.ModMetaEntry;
import mindustrytool.features.modmanager.PackModel.PackMetadata;
import mindustrytool.utils.JsonUtils;

/**
 * Service providing modpack validation, ZIP packaging, archive inspection,
 * direct installation ({@code Distribute}, {@code Shadow}, {@code Override}),
 * and backup/restore operations.
 */
public final class ModpackService {

    private static final int BUFFER_SIZE = 8192;

    private ModpackService() {
    }

    /**
     * Evaluates validation bitmask for a {@link PackModel}.
     */
    public static int evaluateStat(PackModel model) {
        if (model == null) {
            return PackModel.STAT_MISSING_INFO;
        }
        int stat = 0;
        if (model.getMods() == null || model.getMods().isEmpty()) {
            stat |= PackModel.STAT_NON_MOD_SELECT;
        }
        if (isBlank(model.getName())
                || isBlank(model.getDisplayName())
                || isBlank(model.getVersion())
                || isBlank(model.getAuthor())) {
            stat |= PackModel.STAT_MISSING_INFO;
        }

        if (model.getIconPath() != null && !model.getIconPath().trim().isEmpty()) {
            Fi iconFi = new Fi(new File(model.getIconPath().trim()));
            if (iconFi.exists() && !iconFi.isDirectory()) {
                try {
                    Pixmap pixmap = new Pixmap(iconFi);
                    try {
                        if (pixmap.width != pixmap.height) {
                            stat |= PackModel.STAT_ICON_NON_SQUARE;
                        }
                    } finally {
                        pixmap.dispose();
                    }
                } catch (Throwable ignored) {
                    // In headless environment or unreadable image, skip pixmap dimension check
                }
            }
        }

        Set<String> selectedNames = new HashSet<>();
        if (model.getMods() != null) {
            for (ModEntry mod : model.getMods()) {
                if (mod != null && mod.getName() != null) {
                    selectedNames.add(mod.getName().trim().toLowerCase(Locale.ROOT));
                }
            }
            for (ModEntry mod : model.getMods()) {
                if (mod == null) {
                    continue;
                }
                if (!ModStat.isValid(mod.getStat())) {
                    stat |= PackModel.STAT_MOD_INVALID;
                }
                if (mod.getDependencies() != null) {
                    for (String dep : mod.getDependencies()) {
                        if (dep != null && !dep.trim().isEmpty()
                                && !selectedNames.contains(dep.trim().toLowerCase(Locale.ROOT))) {
                            stat |= PackModel.STAT_DEPEND_MISSING;
                        }
                    }
                }
            }
        }

        if (model.getFiles() != null && model.getFiles().size() > 1) {
            List<String> normTargets = new ArrayList<>();
            for (FileEntry entry : model.getFiles()) {
                if (entry == null || isBlank(entry.getTargetPath())) {
                    continue;
                }
                String norm = normalizeRelPath(entry.getTargetPath());
                for (String existing : normTargets) {
                    if (existing.equalsIgnoreCase(norm)
                            || existing.startsWith(norm + "/")
                            || norm.startsWith(existing + "/")) {
                        stat |= PackModel.STAT_FILE_CONFLICT;
                        break;
                    }
                }
                normTargets.add(norm);
            }
        }

        return stat;
    }

    /**
     * Packages the given {@link PackModel} into a {@code .zip} modpack archive.
     */
    public static void exportModpack(PackModel model, Fi outputZip) throws IOException {
        if (outputZip.parent() != null) {
            outputZip.parent().mkdirs();
        }
        PackMetadata metadata = toMetadata(model);
        String packJson = JsonUtils.toJsonPretty(metadata);

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputZip.file())))) {
            writeTextEntry(zos, "pack.json", packJson);

            if (model.getIconPath() != null && !model.getIconPath().trim().isEmpty()) {
                Fi iconFile = new Fi(new File(model.getIconPath().trim()));
                if (iconFile.exists() && !iconFile.isDirectory()) {
                    writeFileEntry(zos, "icon.png", iconFile.file());
                }
            }

            Set<String> writtenEntries = new HashSet<>();
            writtenEntries.add("pack.json");
            writtenEntries.add("icon.png");

            if (model.getMods() != null) {
                for (ModEntry mod : model.getMods()) {
                    if (mod == null || isBlank(mod.getSourcePath())) {
                        continue;
                    }
                    File src = new File(mod.getSourcePath());
                    if (!src.exists()) {
                        continue;
                    }
                    String fileName = !isBlank(mod.getFileName()) ? mod.getFileName() : src.getName();
                    String entryName = "mods/" + sanitizeFileName(fileName);
                    if (src.isFile() && writtenEntries.add(entryName)) {
                        writeFileEntry(zos, entryName, src);
                    }
                }
            }

            if (model.getFiles() != null) {
                for (FileEntry fileEntry : model.getFiles()) {
                    if (fileEntry == null || isBlank(fileEntry.getSourcePath())
                            || isBlank(fileEntry.getTargetPath())) {
                        continue;
                    }
                    File src = new File(fileEntry.getSourcePath());
                    if (!src.exists()) {
                        continue;
                    }
                    String relTarget = normalizeRelPath(fileEntry.getTargetPath());
                    writeRecursiveEntries(zos, "files/" + relTarget, src, writtenEntries);
                }
            }
        }
    }

    public static PackMetadata toMetadata(PackModel model) {
        List<ModMetaEntry> modMetas = new ArrayList<>();
        if (model.getMods() != null) {
            for (ModEntry mod : model.getMods()) {
                if (mod == null) {
                    continue;
                }
                String fileName = !isBlank(mod.getFileName())
                        ? mod.getFileName()
                        : new File(mod.getSourcePath()).getName();
                modMetas.add(new ModMetaEntry(
                        mod.getName(),
                        mod.getVersion(),
                        mod.getAuthor(),
                        "mods/" + sanitizeFileName(fileName)));
            }
        }
        List<String> filePaths = new ArrayList<>();
        if (model.getFiles() != null) {
            for (FileEntry entry : model.getFiles()) {
                if (entry != null && !isBlank(entry.getTargetPath())) {
                    filePaths.add(normalizeRelPath(entry.getTargetPath()));
                }
            }
        }
        return new PackMetadata(
                model.getDisplayName(),
                model.getName(),
                model.getDescription(),
                model.getVersion(),
                model.getAuthor(),
                model.getMode(),
                model.isSkipRepeat(),
                model.isRawBackup(),
                model.isForce(),
                model.isUncheck(),
                modMetas,
                filePaths);
    }

    /**
     * Reads a {@code .zip} or {@code .jar} modpack archive into an extracted temporary folder
     * and reconstructs its {@link PackModel}.
     */
    public static PackModel readModpack(Fi archiveFile, Fi extractDir) throws IOException {
        extractDir.deleteDirectory();
        extractDir.mkdirs();
        unzipArchive(archiveFile.file(), extractDir.file());

        Fi packJsonFi = extractDir.child("pack.json");
        if (!packJsonFi.exists()) {
            throw new IOException("Archive does not contain pack.json");
        }
        String jsonText = new String(packJsonFi.readBytes(), StandardCharsets.UTF_8);
        PackMetadata meta = JsonUtils.fromJson(PackMetadata.class, jsonText);

        PackModel model = new PackModel();
        model.setDisplayName(meta.getDisplayName() != null ? meta.getDisplayName() : "");
        model.setName(meta.getName() != null ? meta.getName() : "");
        model.setDescription(meta.getDescription() != null ? meta.getDescription() : "");
        model.setVersion(meta.getVersion() != null ? meta.getVersion() : "1.0");
        model.setAuthor(meta.getAuthor() != null ? meta.getAuthor() : "");
        model.setMode(meta.getMode() != null ? meta.getMode() : PackModel.MODE_DISTRIBUTE);
        model.setSkipRepeat(meta.isSkipRepeat());
        model.setRawBackup(meta.isRawBackup());
        model.setForce(meta.isForce());
        model.setUncheck(meta.isUncheck());

        Fi iconFi = extractDir.child("icon.png");
        if (iconFi.exists()) {
            model.setIconPath(iconFi.file().getAbsolutePath());
        }

        List<ModEntry> mods = new ArrayList<>();
        if (meta.getMods() != null) {
            for (ModMetaEntry m : meta.getMods()) {
                if (m == null) {
                    continue;
                }
                String relFile = !isBlank(m.getFile()) ? normalizeRelPath(m.getFile()) : ("mods/" + m.getName() + ".zip");
                Fi modFi = extractDir.child(relFile);
                mods.add(new ModEntry(
                        m.getName() != null ? m.getName() : "",
                        m.getName() != null ? m.getName() : "",
                        m.getVersion() != null ? m.getVersion() : "",
                        m.getAuthor() != null ? m.getAuthor() : "",
                        modFi.name(),
                        modFi.file().getAbsolutePath(),
                        "",
                        new ArrayList<>(),
                        0));
            }
        }
        model.setMods(mods);

        List<FileEntry> files = new ArrayList<>();
        if (meta.getFiles() != null) {
            for (String relTarget : meta.getFiles()) {
                if (isBlank(relTarget)) {
                    continue;
                }
                String cleanRel = normalizeRelPath(relTarget);
                Fi extractedFile = extractDir.child("files").child(cleanRel);
                if (extractedFile.exists()) {
                    files.add(new FileEntry(cleanRel, extractedFile.file().getAbsolutePath(), inferCategory(cleanRel)));
                }
            }
        }
        model.setFiles(files);
        return model;
    }

    /**
     * Installs a modpack archive into the given data directory (typically {@code Vars.dataDirectory}).
     */
    public static void installModpack(
            Fi archiveFile,
            Fi dataDir,
            @Nullable String modeOverride,
            boolean createBackup) throws IOException {
        Fi tmpExtract = dataDir.child("tmp").child("modpack_install_" + System.currentTimeMillis());
        try {
            PackModel model = readModpack(archiveFile, tmpExtract);
            String mode = modeOverride != null && !modeOverride.trim().isEmpty()
                    ? modeOverride.trim()
                    : model.getMode();

            if (createBackup || PackModel.MODE_SHADOW.equalsIgnoreCase(mode)
                    || PackModel.MODE_OVERRIDE.equalsIgnoreCase(mode)) {
                createBackupArchive(dataDir, model.getName());
            }

            Fi modsDir = dataDir.child("mods");
            modsDir.mkdirs();

            if (PackModel.MODE_OVERRIDE.equalsIgnoreCase(mode)) {
                Fi[] existingMods = modsDir.list();
                if (existingMods != null) {
                    for (Fi f : existingMods) {
                        if (f != null && !f.isDirectory() && !isMindustryToolArchive(f)) {
                            f.delete();
                        }
                    }
                }
            } else if (PackModel.MODE_SHADOW.equalsIgnoreCase(mode) && Core.settings != null && Vars.mods != null) {
                for (LoadedMod loadedMod : Vars.mods.list()) {
                    if (loadedMod != null && loadedMod != Main.self) {
                        Core.settings.put("mod-" + loadedMod.name + "-enabled", false);
                    }
                }
            }

            for (ModEntry mod : model.getMods()) {
                Fi srcMod = new Fi(new File(mod.getSourcePath()));
                if (!srcMod.exists()) {
                    continue;
                }
                Fi targetMod = modsDir.child(srcMod.name());
                if (model.isSkipRepeat() && targetMod.exists()
                        && PackModel.MODE_DISTRIBUTE.equalsIgnoreCase(mode)) {
                    // Keep existing mod archive when skipRepeat is set in Distribute mode
                } else {
                    srcMod.copyTo(targetMod);
                }
                if (Core.settings != null && !isBlank(mod.getName())) {
                    Core.settings.put("mod-" + mod.getName().toLowerCase(Locale.ROOT) + "-enabled", true);
                }
            }

            for (FileEntry fileEntry : model.getFiles()) {
                Fi src = new Fi(new File(fileEntry.getSourcePath()));
                if (!src.exists()) {
                    continue;
                }
                Fi dest = dataDir.child(normalizeRelPath(fileEntry.getTargetPath()));
                if (dest.parent() != null) {
                    dest.parent().mkdirs();
                }
                if (src.isDirectory()) {
                    copyDirectoryContents(src, dest);
                } else {
                    src.copyTo(dest);
                }
            }

            if (Core.settings != null) {
                Core.settings.forceSave();
            }
        } finally {
            tmpExtract.deleteDirectory();
        }
    }

    /**
     * Creates a backup ZIP of current mods, saves, schematics, and settings in {@code modpack-backups/}.
     */
    public static Fi createBackupArchive(Fi dataDir, @Nullable String label) throws IOException {
        Fi backupDir = dataDir.child("modpack-backups");
        backupDir.mkdirs();
        String cleanLabel = label != null && !label.trim().isEmpty()
                ? sanitizeFileName(label.trim())
                : "backup";
        Fi backupZip = backupDir.child(cleanLabel + "_" + System.currentTimeMillis() + ".zip");

        Set<String> written = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(backupZip.file())))) {
            Fi modsDir = dataDir.child("mods");
            if (modsDir.exists()) {
                writeRecursiveEntries(zos, "mods", modsDir.file(), written);
            }
            Fi savesDir = dataDir.child("saves");
            if (savesDir.exists()) {
                writeRecursiveEntries(zos, "saves", savesDir.file(), written);
            }
            Fi schematicsDir = dataDir.child("schematics");
            if (schematicsDir.exists()) {
                writeRecursiveEntries(zos, "schematics", schematicsDir.file(), written);
            }
            Fi settingsBin = dataDir.child("settings.bin");
            if (settingsBin.exists() && !settingsBin.isDirectory()) {
                writeFileEntry(zos, "settings.bin", settingsBin.file());
            }
        }
        return backupZip;
    }

    public static List<Fi> listBackups(Fi dataDir) {
        Fi backupDir = dataDir.child("modpack-backups");
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return Collections.emptyList();
        }
        Fi[] files = backupDir.list();
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<Fi> list = new ArrayList<>();
        for (Fi f : files) {
            if (f != null && !f.isDirectory() && f.name().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                list.add(f);
            }
        }
        list.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return list;
    }

    public static void restoreBackupArchive(Fi backupZip, Fi dataDir) throws IOException {
        if (!backupZip.exists()) {
            throw new IOException("Backup file does not exist: " + backupZip.path());
        }
        unzipArchive(backupZip.file(), dataDir.file());
    }

    private static boolean isMindustryToolArchive(Fi file) {
        if (Main.self != null && Main.self.file != null) {
            return file.file().equals(Main.self.file.file());
        }
        return file.name().toLowerCase(Locale.ROOT).contains("mindustrytool");
    }

    private static void copyDirectoryContents(Fi srcDir, Fi destDir) {
        destDir.mkdirs();
        Fi[] children = srcDir.list();
        if (children == null) {
            return;
        }
        for (Fi child : children) {
            Fi target = destDir.child(child.name());
            if (child.isDirectory()) {
                copyDirectoryContents(child, target);
            } else {
                child.copyTo(target);
            }
        }
    }

    private static void writeRecursiveEntries(
            ZipOutputStream zos,
            String baseEntryPath,
            File file,
            Set<String> writtenEntries) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return;
            }
            Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File child : children) {
                if ("tmp".equalsIgnoreCase(child.getName())) {
                    continue;
                }
                writeRecursiveEntries(zos, baseEntryPath + "/" + child.getName(), child, writtenEntries);
            }
        } else if (file.isFile()) {
            String normalized = normalizeRelPath(baseEntryPath);
            if (writtenEntries.add(normalized)) {
                writeFileEntry(zos, normalized, file);
            }
        }
    }

    private static void writeTextEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(bytes);
        zos.closeEntry();
    }

    private static void writeFileEntry(ZipOutputStream zos, String entryName, File file) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                zos.write(buffer, 0, read);
            }
        }
        zos.closeEntry();
    }

    public static void unzipArchive(File zipFile, File destDir) throws IOException {
        String canonicalDest = destDir.getCanonicalPath();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String cleanName = normalizeRelPath(entry.getName());
                if (cleanName.isEmpty()) {
                    zis.closeEntry();
                    continue;
                }
                if (cleanName.startsWith("..") || cleanName.contains("/../")) {
                    throw new IOException("Zip Slip prevented for entry: " + entry.getName());
                }
                File outFile = new File(destDir, cleanName);
                String canonicalOut = outFile.getCanonicalPath();
                if (!canonicalOut.startsWith(canonicalDest + File.separator)
                        && !canonicalOut.equals(canonicalDest)) {
                    throw new IOException("Zip Slip prevented for entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    if (outFile.getParentFile() != null) {
                        outFile.getParentFile().mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public static String normalizeRelPath(@Nullable String path) {
        if (path == null) {
            return "";
        }
        String norm = path.replace('\\', '/').trim();
        while (norm.startsWith("/")) {
            norm = norm.substring(1);
        }
        while (norm.endsWith("/")) {
            norm = norm.substring(0, norm.length() - 1);
        }
        return norm;
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String inferCategory(String relPath) {
        String lower = relPath.toLowerCase(Locale.ROOT);
        if (lower.startsWith("saves/")) {
            return "saves";
        }
        if (lower.startsWith("schematics/")) {
            return "schematics";
        }
        if (lower.equals("settings.bin")) {
            return "settings";
        }
        return "custom";
    }

    private static boolean isBlank(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }
}
