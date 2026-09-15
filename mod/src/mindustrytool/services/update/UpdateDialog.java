package mindustrytool.services.update;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Timer;
import mindustry.Vars;
import mindustrytool.Config;
import solim.overlay.SolimDialog;

/**
 * Refactored to extend SolimDialog. All user-visible text via {@code Core.bundle}.
 */
public class UpdateDialog extends SolimDialog {

	private final @Nullable String releaseTag;

	public UpdateDialog(String currentVer, String latestVer, String changelog, Runnable done) {
		this(currentVer, latestVer, changelog, null, done);
	}

	/**
	 * @param releaseTag exact GitHub release tag to install (e.g. {@code v5.0.3-v8-beta});
	 * null uses the default-branch import.
	 */
	public UpdateDialog(String currentVer, String latestVer, String changelog, @Nullable String releaseTag, Runnable done) {
		super(Core.bundle.get("update.dialog.title"));
		this.releaseTag = releaseTag;
		name("updateAvailableDialog");
		closeOnBack();

		String newVersionText = Core.bundle.format(
				"update.message.new-version",
				"[#" + Color.crimson.toString() + "]" + currentVer,
				"[#" + Color.green.toString() + "]" + latestVer);

		content(() -> {
			column().growX().gap(unit(2)).left().width(500f).children(() -> {
				text(newVersionText)
						.wrap();

				button("Discord: " + Config.DISCORD_INVITE_URL, () -> Core.app.openURI(Config.DISCORD_INVITE_URL))
						.color(Color.royal)
						.left();

				divider();

				scroll().size(500f, 400f).scrollX(false).children(() -> {
					column().growX().left().children(() -> {
						text(changelog != null ? changelog : "")
								.wrap()
								.left();
					});
				});
			});
		});

		String cancelLabel = Core.bundle.get("update.button.cancel");
		String updateLabel = Core.bundle.get("update.button.update");

		actionButton(cancelLabel, () -> {
			hide();
			done.run();
		});

		actionButton(updateLabel, () -> {
			try {
				hide();
				Vars.ui.mods.show();
				String repoUrl = Config.ORG_NAME + "/" + Config.REPO_NAME;
				if (releaseTag != null && !releaseTag.trim().isEmpty()) {
					String tagParam = releaseTag.startsWith("tags/") || releaseTag.matches("\\d+") ? releaseTag : "tags/" + releaseTag;
					Vars.ui.mods.githubImportMod(repoUrl, true, tagParam, true);
				} else {
					Vars.ui.mods.githubImportMod(repoUrl, true, true);
				}
				Vars.ui.mods.toFront();
				Timer.schedule(() -> Vars.ui.loadfrag.toFront(), 0.2f);
			} catch (Throwable e) {
				Log.err(e);
				Vars.ui.showException(e);
			}
		});
	}
}
