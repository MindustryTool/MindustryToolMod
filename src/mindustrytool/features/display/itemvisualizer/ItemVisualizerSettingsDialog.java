package mindustrytool.features.display.itemvisualizer;

import mindustry.ui.dialogs.BaseDialog;

public class ItemVisualizerSettingsDialog extends BaseDialog {
    public ItemVisualizerSettingsDialog() {
        super("Item Visualizer Settings");
        addCloseButton();

        cont.table(t -> {
            t.defaults().pad(6).left();

            t.check("Show Item Bridges", ItemVisualizerSettings.showItemBridges, val -> {
                ItemVisualizerSettings.showItemBridges = val;
                ItemVisualizerSettings.save();
            }).row();

            t.check("Show Liquid Bridges", ItemVisualizerSettings.showLiquidBridges, val -> {
                ItemVisualizerSettings.showLiquidBridges = val;
                ItemVisualizerSettings.save();
            }).row();

            t.check("Show Routers & Distributors", ItemVisualizerSettings.showRouters, val -> {
                ItemVisualizerSettings.showRouters = val;
                ItemVisualizerSettings.save();
            }).row();
        });
    }
}
