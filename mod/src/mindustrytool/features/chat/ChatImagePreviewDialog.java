package mindustrytool.features.chat;

import static solim.UI.*;

import arc.util.Scaling;
import mindustry.gen.Icon;
import solim.overlay.SolimDialog;

public class ChatImagePreviewDialog extends SolimDialog {

    public ChatImagePreviewDialog(String imageUrl) {
        super("");
        addCloseButton();
        closeOnBack();
        hidden(this::dispose);
        children(() -> {
            networkImage(imageUrl)
                    .props(
                            placeholder(Icon.image),
                            fallback(Icon.cancel),
                            scaling(Scaling.fit),
                            size(dvw(95f), dvh(80f))
                    );
        });
    }
}
