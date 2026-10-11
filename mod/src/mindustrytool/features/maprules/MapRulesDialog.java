package mindustrytool.features.maprules;

import static solim.UI.button;
import static solim.UI.column;
import static solim.UI.icon;
import static solim.UI.row;
import static solim.UI.spacer;
import static solim.UI.text;
import static solim.UI.textField;
import static solim.UI.unit;

import arc.Core;
import arc.graphics.Color;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.maps.Map;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;

/**
 * Main rules dialog with save-map naming modal.
 */
public class MapRulesDialog extends SolimDialog {

    private final MapRulesFeature feature;
    private final MapRulesView view;

    public MapRulesDialog(MapRulesFeature feature) {
        super(Core.bundle.get("feature.map-rules.title"));
        this.feature = feature;
        this.view = new MapRulesView(feature);
        this.view.setSaveHandler(this::showSaveModal);
        feature.mutator.setResyncHandler(view::refresh);

        name("mapRulesDialog");
        maxWidth(500f);
        addCloseButton();
        closeOnBack();
        shown(view::refresh);
        hidden(() -> feature.mutator.flushPending());

        children(() -> {
            view.element();
        });
    }

    private String t(String key) {
        return Core.bundle.has(key) ? Core.bundle.get(key) : key;
    }

    private @Nullable String savedFilePath(Map saved) {
        try {
            if (saved.file != null) {
                return saved.file.absolutePath();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public void showSaveModal() {
        String initial = feature.saveService.currentMapName();
        Signal<String> nameInput = Signal.of(initial != null ? initial : "");
        Signal<Boolean> overwrite = Signal.of(false);
        SolimDialog modal = new SolimDialog(t("feature.map-rules.save_prompt"));
        modal.name("mapRulesSaveModal");
        modal.maxWidth(500f);
        modal.addCloseButton();
        modal.closeOnBack();
        modal.children(() -> {
            column().grow().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    textField(nameInput)
                            .growX()
                            .height(unit(11))
                            .style(WebStyles.clearInput())
                            .placeholder(t("feature.map-rules.save_name"));
                    row().growX().gap(unit(2)).children(() -> {
                        spacer();
                        button(t("feature.map-rules.cancel"), modal::hide)
                                .style(WebStyles.ghostText())
                                .height(unit(10));
                        button(t("feature.map-rules.save_confirm"), () -> {
                            confirmSave(nameInput.peek(), overwrite.peek(), modal);
                        }).style(WebStyles.primaryText()).height(unit(10));
                    });
                });
            });
        });
        modal.show();
    }

    private void confirmSave(String name, Boolean overwriteFlag, SolimDialog modal) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        boolean overwrite = Boolean.TRUE.equals(overwriteFlag);
        if (!overwrite && feature.saveService.hasCollision(name.trim())) {
            showOverwriteModal(name.trim(), modal);
            return;
        }
        Map saved = feature.saveService.save(name.trim(), overwrite);
        modal.hide();
        if (saved != null) {
            String path = savedFilePath(saved);
            if (path != null) {
                view.setLastSaved(path);
            }
            Vars.ui.showInfoFade(Core.bundle.format("feature.map-rules.save_success", name.trim()));
        } else {
            Vars.ui.showInfoFade(Core.bundle.format("feature.map-rules.save_error", name.trim()));
        }
        view.refresh();
    }

    private void showOverwriteModal(String name, SolimDialog parent) {
        SolimDialog confirm = new SolimDialog(name);
        confirm.name("mapRulesOverwriteModal");
        confirm.maxWidth(500f);
        confirm.addCloseButton();
        confirm.closeOnBack();
        confirm.children(() -> {
            column().grow().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    text(name).color(Color.white).center();
                    row().growX().gap(unit(2)).children(() -> {
                        spacer();
                        button(t("feature.map-rules.cancel"), confirm::hide)
                                .style(WebStyles.ghostText())
                                .height(unit(10));
                        button(t("feature.map-rules.save_overwrite"), () -> {
                            Map saved = feature.saveService.save(name, true);
                            confirm.hide();
                            parent.hide();
                            if (saved != null) {
                                String path = savedFilePath(saved);
                                if (path != null) {
                                    view.setLastSaved(path);
                                }
                                Vars.ui.showInfoFade(Core.bundle.format("feature.map-rules.save_success", name));
                            } else {
                                Vars.ui.showInfoFade(Core.bundle.format("feature.map-rules.save_error", name));
                            }
                            view.refresh();
                        }).style(WebStyles.dangerText()).height(unit(10))
                                .children(() -> icon(Icon.warning).size(unit(5)));
                    });
                });
            });
        });
        confirm.show();
    }
}
