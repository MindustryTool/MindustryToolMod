package mindustrytool.services.auth;

import static solim.UI.*;

import arc.Core;
import mindustry.Vars;
import mindustrytool.components.Loader;
import solim.UI;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class AuthLoginDialog extends SolimDialog {

	private final Signal<String> loginUrlSignal = signal();

	public AuthLoginDialog(MindustryAuthProvider authService) {
		super(Core.bundle.get("auth.login.dialog-title"));
		name("loginDialog");
		closeOnBack();

		Readable<Boolean> hasUrl = loginUrlSignal.map(url -> url != null && !url.trim().isEmpty());

		children(() -> {
			column().grow().padding(unit(4)).center().children(() -> {
				when(hasUrl)
						.thenDo(() -> UI.button(() -> {
							Core.app.setClipboardText(loginUrlSignal.peek());
							Vars.ui.showInfoFade(Core.bundle.get("auth.login.copied"));
						}).children(() -> {
							text(loginUrlSignal).fontScale(0.7f).wrap();
						}))
						.elseDo(() -> new Loader(unit(8)));
			});
		});

		actionButton(Core.bundle.get("auth.login.cancel"), () -> {
			authService.cancelLogin();
			hide();
		}).setWidth(230f);
	}

	public void showLoading() {
		loginUrlSignal.set(null);
	}

	public void showLoginUrl(String loginUrl) {
		loginUrlSignal.set(loginUrl);
	}
}
