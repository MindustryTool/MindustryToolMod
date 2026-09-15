package solim.ui;

import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Scl;
import mindustry.game.EventType.ResizeEvent;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

/**
 * Viewport unit signals and helpers for Solim. Exposes reactive {@link #dvw} and {@link #dvh}
 * signals representing 1% of dynamic viewport width and height in scene coordinates. Automatically
 * synchronizes with window resize events via Mindustry's {@link ResizeEvent}.
 */
public final class Units {

	public static final Signal<Float> dvw = Signal.of(calcDvw());
	public static final Signal<Float> dvh = Signal.of(calcDvh());

	static {
		Events.on(ResizeEvent.class, e -> update());
	}

	private Units() {}

	/** Recalculates and updates both {@link #dvw} and {@link #dvh} signals. */
	public static void update() {
		dvw.set(calcDvw());
		dvh.set(calcDvh());
	}

	/** Returns 1% of current viewport width in Arc scene coordinates. */
	public static float calcDvw() {
		return screenWidth() / 100f;
	}

	/** Returns 1% of current viewport height in Arc scene coordinates. */
	public static float calcDvh() {
		return screenHeight() / 100f;
	}

	/** Returns current viewport width in Arc scene coordinates. */
	public static float screenWidth() {
		if (Core.graphics == null) return 0f;
		float scl = Scl.scl();
		return scl > 0f ? Core.graphics.getWidth() / scl : Core.graphics.getWidth();
	}

	/** Returns current viewport height in Arc scene coordinates. */
	public static float screenHeight() {
		if (Core.graphics == null) return 0f;
		float scl = Scl.scl();
		return scl > 0f ? Core.graphics.getHeight() / scl : Core.graphics.getHeight();
	}

	/** Returns a reactive computed value representing a percentage of viewport width. */
	public static Computed<Float> dvw(float percentage) {
		return new Computed<>(() -> dvw.get() * percentage);
	}

	/** Returns a reactive computed value representing a percentage of viewport height. */
	public static Computed<Float> dvh(float percentage) {
		return new Computed<>(() -> dvh.get() * percentage);
	}

	/** Returns a reactive computed value representing a dynamic percentage of viewport width. */
	public static Computed<Float> dvw(Readable<Float> percentage) {
		return new Computed<>(() -> dvw.get() * percentage.get());
	}

	/** Returns a reactive computed value representing a dynamic percentage of viewport height. */
	public static Computed<Float> dvh(Readable<Float> percentage) {
		return new Computed<>(() -> dvh.get() * percentage.get());
	}

	/** Returns a reactive computed value representing 100% of viewport width. */
	public static Computed<Float> width() {
		return new Computed<>(() -> dvw.get() * 100f);
	}

	/** Returns a reactive computed value representing 100% of viewport height. */
	public static Computed<Float> height() {
		return new Computed<>(() -> dvh.get() * 100f);
	}
}
