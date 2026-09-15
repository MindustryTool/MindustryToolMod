package solim.feedback;

import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;

/** Avatar - user image with fallback. */
public final class Avatar {
	private final Table table = new Table();
	private final Image image = new Image();

	public Avatar() {
		table.add(image).size(48f);
	}

	public Avatar(Drawable d) {
		image.setDrawable(d);
		table.add(image).size(48f);
	}

	public Table table() {
		return table;
	}
}
