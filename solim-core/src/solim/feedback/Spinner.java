package solim.feedback;

import arc.scene.ui.Label;

/** Spinner placeholder - a rotating indicator label. */
public final class Spinner {
	private final Label label = new Label("Loading...");

	public Label label() {
		return label;
	}
}
