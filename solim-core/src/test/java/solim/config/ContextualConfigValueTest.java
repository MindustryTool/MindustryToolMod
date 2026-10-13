package solim.config;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Settings;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class ContextualConfigValueTest extends SolimEnv {

    @BeforeAll
    static void initSettings() {
        Core.settings = new Settings();
    }

    @BeforeEach
    void clearSettings() {
        Core.settings.clear();
    }

    @Test
    void testKeyDerivationAndContextSwitching() {
        ConfigGroup group = ConfigGroup.of("hud");
        Signal<String> mode = Signal.of("desktop");

        ContextualConfigValue<Integer, String> widthConfig = new ContextualConfigValue<>(
                group,
                "width",
                mode,
                m -> m,
                800,
                ConfigPersister.INTEGER);

        assertEquals("hud.width.desktop", widthConfig.getCurrentKey());
        assertEquals("hud.width.desktop", widthConfig.getKey());
        assertEquals(800, widthConfig.get());

        widthConfig.set(1024);
        assertEquals(1024, Core.settings.getInt("hud.width.desktop"));
        assertEquals(1024, widthConfig.get());

        mode.set("mobile");
        assertEquals("hud.width.mobile", widthConfig.getCurrentKey());

        assertEquals(800, widthConfig.get());

        widthConfig.set(400);
        assertEquals(400, Core.settings.getInt("hud.width.mobile"));
        assertEquals(1024, Core.settings.getInt("hud.width.desktop"));

        mode.set("desktop");
        assertEquals(1024, widthConfig.get());
    }

    @Test
    void testDiscriminantChangeEmitsToSignalListeners() {
        ConfigGroup group = ConfigGroup.of("app");
        Signal<Boolean> portrait = Signal.of(false);

        ContextualConfigValue<Float, Boolean> xConfig = group.floatValueKeyed(
                "posX",
                portrait,
                p -> p ? "portrait" : "landscape",
                100f);

        Core.settings.put("app.posX.portrait", 250f);

        AtomicReference<Float> signalValue = new AtomicReference<>(xConfig.get());
        xConfig.signal().subscribe(signalValue::set);

        portrait.set(true);
        assertEquals(250f, signalValue.get());
        assertEquals(250f, xConfig.get());
    }

    @Test
    void testDisposeStopsDiscriminantUpdates() {
        ConfigGroup group = ConfigGroup.of("test");
        Signal<String> env = Signal.of("dev");

        ContextualConfigValue<String, String> endpoint = group.stringValueKeyed(
                "endpoint",
                env,
                e -> e,
                "http://localhost");

        endpoint.dispose();

        env.set("prod");

        assertEquals("test.endpoint.dev", endpoint.getCurrentKey());
    }

    @Test
    void testDefaultFactoryPerContext() {
        ConfigGroup group = ConfigGroup.of("hud");
        Signal<String> context = Signal.of("collapsed.landscape");

        ContextualConfigValue<Float, String> xConfig = group.floatValueKeyed(
                "x",
                context,
                k -> k,
                k -> k.startsWith("collapsed") ? 100f : 500f);

        assertEquals(100f, xConfig.get());
        assertEquals("hud.x.collapsed.landscape", xConfig.getCurrentKey());

        context.set("expanded.landscape");
        assertEquals(500f, xConfig.get());
        assertEquals("hud.x.expanded.landscape", xConfig.getCurrentKey());

        xConfig.set(600f);
        assertEquals(600f, xConfig.get());
        assertEquals(600f, Core.settings.getFloat("hud.x.expanded.landscape"));

        context.set("collapsed.landscape");
        assertEquals(100f, xConfig.get());

        context.set("expanded.landscape");
        assertEquals(600f, xConfig.get());

        xConfig.reset();
        assertEquals(500f, xConfig.get());
    }
}
