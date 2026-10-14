package mindustrytool.features.modmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import mindustrytool.utils.JsonUtils;
import org.junit.jupiter.api.Test;

class ModServiceTest {

    @Test
    void parsesGithubRepoFromUrlsAndShorthands() {
        assertEquals("EB-Wilson/Helium", ModService.parseGithubRepo("EB-Wilson/Helium"));
        assertEquals("Anuken/Mindustry", ModService.parseGithubRepo("https://github.com/Anuken/Mindustry"));
        assertEquals("Anuken/Mindustry", ModService.parseGithubRepo("https://github.com/Anuken/Mindustry.git"));
        assertEquals("Anuken/Mindustry", ModService.parseGithubRepo("github.com/Anuken/Mindustry/releases/tag/v146"));
        assertNull(ModService.parseGithubRepo(null));
        assertNull(ModService.parseGithubRepo("   "));
        assertNull(ModService.parseGithubRepo("invalid-single-segment"));
    }

    @Test
    void extractsReleaseVersionAndResolvesPreferredAssetUrl() throws Exception {
        JsonNode releaseWithDexedJar = JsonUtils.readTree("{"
                + "\"tag_name\": \"v2.1.0\","
                + "\"zipball_url\": \"https://api.github.com/repos/u/r/zipball/v2.1.0\","
                + "\"assets\": ["
                + "  {\"name\": \"mod.zip\", \"browser_download_url\": \"https://dl/mod.zip\"},"
                + "  {\"name\": \"mod.jar\", \"browser_download_url\": \"https://dl/mod.jar\"},"
                + "  {\"name\": \"dexed-mod.jar\", \"browser_download_url\": \"https://dl/dexed-mod.jar\"}"
                + "]"
                + "}");
        assertEquals("v2.1.0", ModService.extractReleaseVersion(releaseWithDexedJar));
        assertEquals("https://dl/dexed-mod.jar", ModService.resolveReleaseAssetUrl(releaseWithDexedJar, true));

        JsonNode releaseWithRegularJar = JsonUtils.readTree("{"
                + "\"name\": \"1.5.0\","
                + "\"zipball_url\": \"https://api.github.com/repos/u/r/zipball/1.5.0\","
                + "\"assets\": ["
                + "  {\"name\": \"mod.zip\", \"browser_download_url\": \"https://dl/mod.zip\"},"
                + "  {\"name\": \"mod.jar\", \"browser_download_url\": \"https://dl/mod.jar\"}"
                + "]"
                + "}");
        assertEquals("1.5.0", ModService.extractReleaseVersion(releaseWithRegularJar));
        assertEquals("https://dl/mod.jar", ModService.resolveReleaseAssetUrl(releaseWithRegularJar, true));

        JsonNode releaseSourceFallback = JsonUtils.readTree("{"
                + "\"tag_name\": \"v1.0\","
                + "\"zipball_url\": \"https://api.github.com/repos/u/r/zipball/v1.0\","
                + "\"assets\": []"
                + "}");
        assertEquals(
                "https://api.github.com/repos/u/r/zipball/v1.0",
                ModService.resolveReleaseAssetUrl(releaseSourceFallback, false));
    }

    @Test
    void parsesModListingsJsonAndComputesIconUrlAndRepoMatching() {
        String sampleModsJson = "["
                + "{"
                + "  \"repo\": \"EB-Wilson/Helium\","
                + "  \"name\": \"Helium\","
                + "  \"internalName\": \"helium\","
                + "  \"author\": \"EB_Wilson\","
                + "  \"version\": \"1.0\","
                + "  \"description\": \"QoL mod\","
                + "  \"minGameVersion\": \"146\","
                + "  \"stars\": 128,"
                + "  \"hasScripts\": false,"
                + "  \"hasJava\": true"
                + "}"
                + "]";

        List<ModListing> listings = ModService.parseListingsJson(sampleModsJson);
        assertEquals(1, listings.size());

        ModListing listing = listings.get(0);
        assertEquals("EB-Wilson/Helium", listing.getRepo());
        assertEquals("helium", listing.getInternalName());
        assertEquals(128, listing.getStars());
        assertTrue(listing.isHasJava());
        assertFalse(listing.isHasScripts());
        assertEquals(
                "https://raw.githubusercontent.com/Anuken/MindustryMods/master/icons/EB-Wilson_Helium",
                listing.getIconUrl());

        ModName modName = ModName.fromListing(listing);
        assertNotNull(modName);
        assertTrue(modName.matches(listing));
        assertTrue(modName.matches(new ModName("Helium", "EB_Wilson", "eb-wilson/helium")));
        assertTrue(modName.matches(new ModName("Helium", "EB_Wilson", null)));
        assertFalse(modName.matches(new ModName("other-mod", "other-author", "other/repo")));
    }
}
