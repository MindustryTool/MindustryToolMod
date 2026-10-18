package mindustrytool.features.chat;

import static solim.UI.*;
import static solim.modifier.Values.color;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.left;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.FileChooser;
import mindustry.ui.Styles;
import solim.overlay.SolimDialog;

import arc.util.Log;
import mindustrytool.services.MindustryTool;
import solim.reactive.Signal;

public class AttachContentDialog extends SolimDialog {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private final Cons<String> callback;
    private final Signal<Boolean> uploading = Signal.of(false);

    public AttachContentDialog(Cons<String> callback) {
        super(Core.bundle.get("chat.attach-content", "Attach Content"));
        this.callback = callback;

        addCloseButton();
        maxWidth(dvh(Vars.mobile ? 80f : 40f));

        children(() -> {
            column().props(grow(), padding(unit(3)), gap(unit(2))).children(() -> {
                when(uploading)
                        .thenDo(() -> row().props(growX(), padding(unit(4)), center(), gap(unit(2))).children(() -> {
                            image(Icon.refresh).props(size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(Color.white));
                            text(Core.bundle.get("chat.uploading-image", "Uploading image..."))
                                    .props(color(Color.lightGray));
                        }))
                        .elseDo(() -> column().props(growX(), gap(unit(2))).children(() -> {
                            button(this::selectImageFile)
                                    .props(style(Styles.defaultb), growX(), height(unit(12)), prefHeight(unit(12)))
                                    .children(() -> {
                                        row().props(growX(), gap(unit(2))).children(() -> {
                                            image(Icon.image).props(size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                                            text(Core.bundle.get("chat.select-image", "Image (.png)"))
                                                    .props(color(Color.white), left());
                                        });
                                    });

                            button(this::selectSchematicFile)
                                    .props(style(Styles.defaultb), growX(), height(unit(12)), prefHeight(unit(12)))
                                    .children(() -> {
                                        row().props(growX(), gap(unit(2))).children(() -> {
                                            image(Icon.file).props(size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                                            text(Core.bundle.get("chat.select-file", "Select File (.msch)"))
                                                    .props(color(Color.white), left());
                                        });
                                    });

                            button(this::selectSaveFile)
                                    .props(style(Styles.defaultb), growX(), height(unit(12)), prefHeight(unit(12)))
                                    .children(() -> {
                                        row().props(growX(), gap(unit(2))).children(() -> {
                                            image(Icon.map).props(size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                                            text(Core.bundle.get("map", "Map / Save (.msav)"))
                                                    .props(color(Color.white), left());
                                        });
                                    });

                            button(this::pasteFromClipboard)
                                    .props(style(Styles.defaultb), growX(), height(unit(12)), prefHeight(unit(12)))
                                    .children(() -> {
                                        row().props(growX(), gap(unit(2))).children(() -> {
                                            image(Icon.paste).props(size(unit(6), unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                                            text(Core.bundle.get("chat.paste-link", "Paste from Clipboard"))
                                                    .props(color(Color.white), left());
                                        });
                                    });
                        }))
                        .props(grow());
            });
        });
    }

    private void selectImageFile() {
        FileChooser.open("png").submit(file -> {
            if (file == null) {
                return;
            }
            if (file.length() > MAX_IMAGE_BYTES) {
                Vars.ui.showInfoFade(Core.bundle.format("chat.image-too-large", 5));
                return;
            }

            uploading.set(true);
            try {
                byte[] bytes = file.readBytes();
                MindustryTool.uploadMedia(bytes, file.name())
                        .thenAccept(url -> Core.app.post(() -> {
                            uploading.set(false);
                            if (url != null && !url.trim().isEmpty()) {
                                callback.get(url.trim());
                                hide();
                            }
                        }))
                        .exceptionally(err -> {
                            Core.app.post(() -> {
                                uploading.set(false);
                                Log.err(err);
                                Vars.ui.showException(err);
                            });
                            return null;
                        });
            } catch (Throwable t) {
                uploading.set(false);
                Vars.ui.showException(t);
            }
        });
    }

    private void selectSchematicFile() {
        FileChooser.open("msch").submit(file -> {
            if (file == null)
                return;
            try {
                byte[] bytes = file.readBytes();
                String base64 = new String(Base64Coder.encode(bytes));
                callback.get(base64);
                hide();
            } catch (Throwable t) {
                Vars.ui.showException(t);
            }
        });
    }

    private void selectSaveFile() {
        FileChooser.open("msav").submit(file -> {
            if (file == null)
                return;
            try {
                byte[] bytes = file.readBytes();
                String base64 = new String(Base64Coder.encode(bytes));
                callback.get(base64);
                hide();
            } catch (Throwable t) {
                Vars.ui.showException(t);
            }
        });
    }

    private void pasteFromClipboard() {
        String content = Core.app.getClipboardText();
        if (content != null && !content.trim().isEmpty()) {
            callback.get(content.trim());
            hide();
        } else {
            Vars.ui.showInfoFade(Core.bundle.get("chat.clipboard-empty"));
        }
    }
}
