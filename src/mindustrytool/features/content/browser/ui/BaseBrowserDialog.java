package mindustrytool.features.content.browser.ui;

import arc.Core;
import mindustry.ui.dialogs.BaseDialog;

public class BaseBrowserDialog extends BaseDialog {
    public BaseBrowserDialog(String title) {
        super(title);
        addCloseListener();
        setFillParent(true);
    }

    protected void loading(boolean load) {
        if (load)
            mindustry.Vars.ui.loadfrag.show("Loading...");
        else
            mindustry.Vars.ui.loadfrag.hide();
    }

    protected void loading(boolean load, String text) {
        if (load)
            mindustry.Vars.ui.loadfrag.show(text);
        else
            mindustry.Vars.ui.loadfrag.hide();
    }

    protected boolean isPortrait() {
        return Core.graphics.isPortrait();
    }
}
