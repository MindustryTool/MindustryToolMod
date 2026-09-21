package solim.style;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.graphics.Color;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class StyleCacheTest extends SolimEnv {


    @BeforeEach
    void clearCache() {
        StyleCache.clear();
    }

    @Test
    void identicalStaticStylesReuseInstance() {
        SolimButtonStyle first = new SolimButtonStyleBuilder()
                .rounded(8)
                .border(1.5f, Color.white)
                .up(u -> u.background(Color.blue))
                .build();

        SolimButtonStyle second = new SolimButtonStyleBuilder()
                .rounded(8)
                .border(1.5f, Color.white)
                .up(u -> u.background(Color.blue))
                .build();

        assertSame(first, second);
        assertSame(first.style(), second.style());
    }

    @Test
    void differentStaticStylesCreateNewInstances() {
        SolimButtonStyle base = new SolimButtonStyleBuilder()
                .rounded(8)
                .up(u -> u.background(Color.blue))
                .build();

        SolimButtonStyle other = new SolimButtonStyleBuilder()
                .rounded(4)
                .up(u -> u.background(Color.blue))
                .build();

        assertNotSame(base, other);
        assertNotSame(base.style(), other.style());
    }

    @Test
    void differentBackgroundsAreNotShared() {
        SolimButtonStyle blue = new SolimButtonStyleBuilder()
                .rounded(8)
                .up(u -> u.background(Color.blue))
                .build();

        SolimButtonStyle red = new SolimButtonStyleBuilder()
                .rounded(8)
                .up(u -> u.background(Color.red))
                .build();

        assertNotSame(blue, red);
    }

    @Test
    void dynamicStylesBypassCache() {
        Signal<Color> signal = Signal.of(Color.red);

        SolimButtonStyle first = new SolimButtonStyleBuilder()
                .rounded(8)
                .up(u -> u.background(signal))
                .build();

        SolimButtonStyle second = new SolimButtonStyleBuilder()
                .rounded(8)
                .up(u -> u.background(signal))
                .build();

        assertNotSame(first, second);
        assertNotSame(first.style(), second.style());
    }

    @Test
    void layoutSignalsBypassCache() {
        Signal<Float> padding = Signal.of(8f);

        SolimButtonStyle first = new SolimButtonStyleBuilder()
                .rounded(8)
                .padding(padding)
                .up(u -> u.background(Color.blue))
                .build();

        SolimButtonStyle second = new SolimButtonStyleBuilder()
                .rounded(8)
                .padding(padding)
                .up(u -> u.background(Color.blue))
                .build();

        assertNotSame(first, second);
    }
}
