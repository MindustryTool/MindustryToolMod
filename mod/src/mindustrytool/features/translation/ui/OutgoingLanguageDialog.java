package mindustrytool.features.translation.ui;

import static solim.UI.*;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;

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
                                .style(Styles.flatTogglet)
                                .checked(isSelected)
                                .props(size(400f, 50f), prefWidth(400f), prefHeight(50f));
                    }
                });
            });
        });
    }
}
