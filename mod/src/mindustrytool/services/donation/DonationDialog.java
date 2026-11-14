package mindustrytool.services.donation;

import static solim.UI.*;

import arc.Core;
import mindustry.graphics.Pal;
import mindustrytool.Config;
import mindustrytool.components.FileIcon;
import solim.overlay.SolimDialog;
import solim.reactive.Effect;
import solim.reactive.Signal;

public class DonationDialog extends SolimDialog {

    public DonationDialog() {
        super(Core.bundle.get("donation.dialog.title"));
        name("donationDialog");

        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        actionButton(
                Core.bundle.get("donation.button.donate"),
                FileIcon.of("heart.png"),
                200f,
                64f,
                this::onDonate);

        Signal<Boolean> dontShowAgain = Signal.of(DonationPrompt.isDontShowAgain());

        children(() -> {
            Effect.of(dontShowAgain, DonationPrompt::setDontShowAgain);

            column().props(growX(), padding(unit(3)), prefWidth(500f), gap(unit(3)), center()).children(() -> {
                icon(FileIcon.of("heart.png"))
                        .props(size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)), color(Pal.accent));

                text(Core.bundle.get("donation.dialog.description"))
                        .props(growX(), textWrap(), center());

                checkbox(Core.bundle.get("donation.dialog.dont-show-again"), dontShowAgain)
                        .props(growX(), left());
            });
        });
    }

    private void onDonate() {
        Core.app.openURI(Config.DONATE_URL);
        hide();
    }
}
