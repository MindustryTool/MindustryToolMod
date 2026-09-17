package mindustrytool.features.chat;

import static solim.UI.*;

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

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final Cons<String> callback;
    private final Signal<Boolean> uploading = Signal.of(false);

    public AttachContentDialog(Cons<String> callback) {
        super(Core.bundle.get("chat.attach-content", "Attach Content"));
        this.callback = callback;

        addCloseButton();
        maxWidth(Vars.mobile ? dvh(80).get() : dvh(40).get());

        children(() -> {
            column().grow().padding(unit(3)).gap(unit(2)).children(() -> {
                dynamic(uploading, isUploading -> {
                    if (Boolean.TRUE.equals(isUploading)) {
                        return row().growX().padding(unit(4)).center().gap(unit(2)).children(() -> {
                            image(Icon.refresh).size(unit(6), unit(6)).color(Color.white);
                            text(Core.bundle.get("chat.uploading-image", "Uploading image..."))
                                    .color(Color.lightGray);
                        });
                    }

                    return column().growX().gap(unit(2)).children(() -> {
                        button(this::selectImageFile)
                                .style(Styles.defaultb)
                                .growX()
                                .height(unit(12))
                                .children(() -> {
                                    row().growX().gap(unit(2)).children(() -> {
                                        image(Icon.image).size(unit(6), unit(6));
                                        text(Core.bundle.get("chat.select-image", "Image (.png)"))
                                                .color(Color.white)
                                                .left();
                                    });
                                });

                        button(this::selectSchematicFile)
                                .style(Styles.defaultb)
                                .growX()
                                .height(unit(12))
                                .children(() -> {
                                    row().growX().gap(unit(2)).children(() -> {
                                        image(Icon.file).size(unit(6), unit(6));
                                        text(Core.bundle.get("chat.select-file", "Select File (.msch)"))
                                                .color(Color.white)
                                                .left();
                                    });
                                });

                        button(this::selectSaveFile)
                                .style(Styles.defaultb)
                                .growX()
                                .height(unit(12))
                                .children(() -> {
                                    row().growX().gap(unit(2)).children(() -> {
                                        image(Icon.map).size(unit(6), unit(6));
                                        text(Core.bundle.get("map", "Map / Save (.msav)"))
                                                .color(Color.white)
                                                .left();
                                    });
                                });

                        button(this::pasteFromClipboard)
                                .style(Styles.defaultb)
                                .growX()
                                .height(unit(12))
                                .children(() -> {
                                    row().growX().gap(unit(2)).children(() -> {
                                        image(Icon.paste).size(unit(6), unit(6));
                                        text(Core.bundle.get("chat.paste-link", "Paste from Clipboard"))
                                                .color(Color.white)
                                                .left();
                                    });
                                });
                    });
                });
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
            Vars.ui.showInfoFade(Core.bundle.get("chat.clipboard-empty", "Clipboard is empty"));
        }
    }
}
