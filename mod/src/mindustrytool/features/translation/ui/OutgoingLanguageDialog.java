package mindustrytool.features.translation.ui;

import static solim.UI.*;

import arc.Core;
import java.util.Locale;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.LanguageDialog;
import mindustrytool.features.translation.TranslationFeature;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;

public class OutgoingLanguageDialog extends SolimDialog {

    public OutgoingLanguageDialog(TranslationFeature feature) {
        super(Core.bundle.get("feature.translation.outgoing.select-lang",
                Core.bundle.get("setting.language.name", "Language")));

        name("outgoingLanguageDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> {
            scroll().props(center()).children(() -> {
                column().props(center()).children(() -> {
                    for (Locale loc : Vars.locales) {
                        String displayName = LanguageDialog.getDisplayName(loc);
                        Readable<Boolean> isSelected = feature.outgoingTargetLangConfig.signal()
                                .map(current -> feature.isSameLanguage(current, loc));

                        button(displayName, () -> {
                            feature.setOutgoingTargetLocale(loc);
                            hide();
                        })
                                .props(style(Styles.flatTogglet), checked(isSelected), size(400f, 50f), prefWidth(400f), prefHeight(50f));
                    }
                });
            });
        });
    }
}
