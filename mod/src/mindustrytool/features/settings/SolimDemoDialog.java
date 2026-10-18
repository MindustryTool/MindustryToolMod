package mindustrytool.features.settings;

import solim.overlay.SolimDialog;

public class SolimDemoDialog extends SolimDialog {

    public SolimDemoDialog() {
        super("Solim UI Demo");

        name("solimDemoDialog");
        addCloseButton();
        closeOnBack();

        maxWidth(700f);

        children(SolimDemoView::new);
    }
}