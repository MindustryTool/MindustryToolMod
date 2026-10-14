package mindustrytool.features.modmanager;

import arc.util.Nullable;
import java.util.Locale;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mindustry.mod.Mods.LoadedMod;

/**
 * Value object identifying a mod by its internal name, display name, author, and repository.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModName {
    private String name = "";
    private String author = "";
    private @Nullable String repo;

    public ModName(String name, String author) {
        this(name, author, null);
    }

    public static ModName fromLoadedMod(LoadedMod mod) {
        String modName = mod.meta != null && mod.meta.displayName != null
                ? mod.meta.displayName
                : mod.name;
        String modAuthor = mod.meta != null && mod.meta.author != null
                ? mod.meta.author
                : "";
        return new ModName(modName != null ? modName : "", modAuthor, mod.getRepo());
    }

    public static ModName fromListing(ModListing listing) {
        String name = listing.getName() != null && !listing.getName().isEmpty()
                ? listing.getName()
                : listing.getInternalName();
        String author = listing.getAuthor() != null ? listing.getAuthor() : "";
        return new ModName(name != null ? name : "", author, listing.getRepo());
    }

    public String key() {
        if (repo != null && !repo.trim().isEmpty()) {
            return repo.trim().toLowerCase(Locale.ROOT);
        }
        return (normalize(name) + ":" + normalize(author));
    }

    public boolean matches(@Nullable ModListing listing) {
        if (listing == null) {
            return false;
        }
        if (repo != null && !repo.trim().isEmpty() && listing.getRepo() != null) {
            if (repo.trim().equalsIgnoreCase(listing.getRepo().trim())) {
                return true;
            }
        }
        boolean sameAuthor = normalize(author).equals(normalize(listing.getAuthor()));
        String normName = normalize(name);
        return sameAuthor && (normName.equals(normalize(listing.getName()))
                || normName.equals(normalize(listing.getInternalName())));
    }

    public boolean matches(@Nullable ModName other) {
        if (other == null) {
            return false;
        }
        if (repo != null && !repo.trim().isEmpty()
                && other.repo != null && !other.repo.trim().isEmpty()) {
            return repo.trim().equalsIgnoreCase(other.repo.trim());
        }
        return normalize(name).equals(normalize(other.name))
                && normalize(author).equals(normalize(other.author));
    }

    private static String normalize(@Nullable String value) {
        return value != null ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModName)) {
            return false;
        }
        ModName that = (ModName) o;
        return matches(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalize(name), normalize(author));
    }
}
