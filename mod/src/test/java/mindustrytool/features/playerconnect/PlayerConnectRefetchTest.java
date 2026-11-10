package mindustrytool.features.playerconnect;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.net.NetworkProxy;
import mindustrytool.features.playerconnect.ui.RoomBrowserView;
import mindustrytool.models.response.PlayerConnectRoom;
import mindustrytool.models.response.PlayerConnectRoom.PlayerConnectRoomData;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.QueryCache;

class PlayerConnectRefetchTest extends MindustryTestEnv {

    private final List<PlayerConnectFeature> createdFeatures = new ArrayList<>();
    private Drawable savedBlack8;
    private LabelStyle savedOutlineLabel;

    @BeforeEach
    void setUp() {
        newScene();
        if (Fonts.def == null) {
            Fonts.def = Core.scene.getStyle(LabelStyle.class).font;
        }
        savedBlack8 = Styles.black8;
        savedOutlineLabel = Styles.outlineLabel;
        if (Styles.black8 == null) {
            Styles.black8 = new TextureRegionDrawable(new TextureRegion());
        }
        if (Styles.outlineLabel == null) {
            Styles.outlineLabel = Core.scene.getStyle(LabelStyle.class);
        }
        Icon.planet = new TextureRegionDrawable();
        Icon.refresh = new TextureRegionDrawable();
        Icon.rightOpen = new TextureRegionDrawable();
        Icon.downOpen = new TextureRegionDrawable();
        Icon.copy = new TextureRegionDrawable();
        QueryCache.getInstance().clear();
    }

    @AfterEach
    void tearDown() {
        for (PlayerConnectFeature feature : createdFeatures) {
            feature.disable();
            feature.getRoomsQuery().dispose();
        }
        createdFeatures.clear();
        Styles.black8 = savedBlack8;
        Styles.outlineLabel = savedOutlineLabel;
        QueryCache.getInstance().clear();
    }

    private PlayerConnectFeature createFeature(AtomicReference<CompletableFuture<List<PlayerConnectRoom>>> futureRef) {
        PlayerConnectFeature feature = new PlayerConnectFeature(futureRef::get, false);
        createdFeatures.add(feature);
        return feature;
    }

    private static PlayerConnectRoom makeRoom(String id, String roomName) {
        PlayerConnectRoomData data = new PlayerConnectRoomData();
        data.setName(roomName);
        data.setMapName("Ground Zero");
        data.setGamemode("Survival");
        data.setPlayers(Collections.emptyList());
        data.setMods(Collections.emptyList());
        data.setProtocolVersion(NetworkProxy.PROTOCOL_VERSION);

        PlayerConnectRoom room = new PlayerConnectRoom();
        room.setRoomId(id);
        room.setName("Relay-A");
        room.setLink("player-connect://localhost:11010/" + id);
        room.setData(data);
        return room;
    }

    private static String collectText(Element element) {
        StringBuilder sb = new StringBuilder();
        collectTextInto(element, sb);
        return sb.toString();
    }

    private static void collectTextInto(Element element, StringBuilder sb) {
        if (element instanceof Label) {
            CharSequence text = ((Label) element).getText();
            if (text != null && text.length() > 0) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(text);
            }
        }
        if (element instanceof Group) {
            for (Element child : ((Group) element).getChildren()) {
                collectTextInto(child, sb);
            }
        }
    }

    @Test
    void getRoomsReturnsSingleFeatureOwnedReadable() {
        AtomicReference<CompletableFuture<List<PlayerConnectRoom>>> futureRef = new AtomicReference<>(
                CompletableFuture.completedFuture(Collections.emptyList()));
        PlayerConnectFeature feature = createFeature(futureRef);

        assertSame(feature.getRooms(), feature.getRooms());
    }

    @Test
    void refetchShowsLoadingThenRendersUpdatedRooms() {
        AtomicReference<CompletableFuture<List<PlayerConnectRoom>>> futureRef = new AtomicReference<>(
                CompletableFuture.completedFuture(Collections.singletonList(makeRoom("r1", "Alpha Room"))));
        PlayerConnectFeature feature = createFeature(futureRef);

        RoomBrowserView view = new RoomBrowserView(feature);
        Element root = view.element();
        flushEffects();

        String initialText = collectText(root);
        assertTrue(initialText.contains("Alpha Room"), "Initial rooms should be rendered: " + initialText);

        CompletableFuture<List<PlayerConnectRoom>> secondFetch = new CompletableFuture<>();
        futureRef.set(secondFetch);
        feature.getRoomsQuery().refetch();
        flushEffects();

        String loadingText = collectText(root);
        assertFalse(loadingText.contains("Alpha Room"), "Old rooms should be hidden while loading: " + loadingText);
        assertTrue(loadingText.contains("Loading"), "Loading indicator should be visible while fetching: " + loadingText);

        secondFetch.complete(Collections.singletonList(makeRoom("r2", "Beta Room")));
        flushEffects();

        String refreshedText = collectText(root);
        assertTrue(refreshedText.contains("Beta Room"), "Refetched rooms should replace stale data: " + refreshedText);
        assertFalse(refreshedText.contains("Alpha Room"), "Stale rooms must not remain after refetch: " + refreshedText);

        CompletableFuture<List<PlayerConnectRoom>> thirdFetch = new CompletableFuture<>();
        futureRef.set(thirdFetch);
        feature.getRoomsQuery().refetch();
        flushEffects();
        thirdFetch.complete(Collections.singletonList(makeRoom("r3", "Gamma Room")));
        flushEffects();

        String secondRefreshedText = collectText(root);
        assertTrue(secondRefreshedText.contains("Gamma Room"), "Subsequent refetch should update rooms: " + secondRefreshedText);
        assertFalse(secondRefreshedText.contains("Beta Room"));

        view.dispose();
    }

    @Test
    void disableAndReEnablePreservesRoomsQueryAndRefetch() {
        AtomicReference<CompletableFuture<List<PlayerConnectRoom>>> futureRef = new AtomicReference<>(
                CompletableFuture.completedFuture(Collections.singletonList(makeRoom("r1", "Initial Room"))));
        PlayerConnectFeature feature = createFeature(futureRef);
        flushEffects();
        assertEquals("Initial Room", feature.getRooms().get().get(0).getData().getName());

        feature.disable();
        assertFalse(feature.isEnabled());
        assertFalse(feature.getRoomsQuery().isDisposed(), "Disabling feature must not permanently dispose roomsQuery");

        CompletableFuture<List<PlayerConnectRoom>> nextFetch = CompletableFuture.completedFuture(
                Collections.singletonList(makeRoom("r4", "Delta Room")));
        futureRef.set(nextFetch);
        feature.enable();
        feature.getRoomsQuery().refetch();
        flushEffects();

        assertTrue(feature.isEnabled());
        assertEquals(1, feature.getRooms().get().size());
        assertEquals("Delta Room", feature.getRooms().get().get(0).getData().getName());
    }
}
