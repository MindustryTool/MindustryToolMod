package mindustrytool.features.playerconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.mock.MockSettings;
import arc.scene.style.TextureRegionDrawable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import mindustry.gen.Icon;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.models.response.PlayerConnectProvider;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerConnectPingAndCustomProviderTest extends MindustryTestEnv {

    private PlayerConnectFeature feature;

    @BeforeEach
    void setUp() {
        newScene();
        Core.settings = new MockSettings();
        Icon.planet = new TextureRegionDrawable();
        feature = new PlayerConnectFeature(() -> CompletableFuture.completedFuture(Collections.emptyList()), false);
    }

    @AfterEach
    void tearDown() {
        PlayerConnectClient.disposePinger();
    }

    @Test
    void testPingHostSuccess() throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread serverThread = new Thread(() -> {
            try {
                serverSocket.accept().close();
            } catch (IOException ignored) {
            }
        });
        serverThread.start();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong elapsed = new AtomicLong(-1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        PlayerConnectClient.pingHost("127.0.0.1", port, ms -> {
            elapsed.set(ms);
            latch.countDown();
        }, err -> {
            failure.set(err);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(failure.get());
        assertTrue(elapsed.get() >= 0);

        serverSocket.close();
        serverThread.join(1000);
    }

    @Test
    void testPingHostFailureOnUnreachablePort() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        PlayerConnectClient.pingHost("127.0.0.1", 65432, ms -> {
            latch.countDown();
        }, err -> {
            failure.set(err);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(failure.get());
    }

    @Test
    void testCustomProviderAddAndRemove() {
        feature.addCustomProvider("Custom Alpha", "127.0.0.1:11010");
        List<PlayerConnectProvider> customProviders = feature.loadCustomProviders();
        assertEquals(1, customProviders.size());
        assertEquals("Custom Alpha", customProviders.get(0).getName());
        assertEquals("127.0.0.1:11010", customProviders.get(0).getAddress());

        feature.removeCustomProvider("Custom Alpha");
        List<PlayerConnectProvider> afterRemove = feature.loadCustomProviders();
        assertTrue(afterRemove.isEmpty());
    }
}
