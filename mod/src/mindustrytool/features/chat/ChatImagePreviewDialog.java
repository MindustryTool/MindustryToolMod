package mindustrytool.features.chat;

import static solim.UI.*;

import arc.util.Scaling;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

/**
 * Near-fullscreen preview dialog for a chat image message. Reuses the cached
 * native-resolution texture already loaded by the message thumbnail.
 */
public class ChatImagePreviewDialog extends SolimDialog {

    public ChatImagePreviewDialog(String imageUrl) {
        super("");
        addCloseButton();
        closeOnBack();
        fillParent(true);
        hidden(this::dispose);
        children(() -> {
            networkImage(imageUrl)
                    .placeholder(Icon.image)
                    .fallback(Icon.cancel)
                    .scaling(Scaling.fit)
                    .size(dvw(95), dvh(80));
        });
    }
}