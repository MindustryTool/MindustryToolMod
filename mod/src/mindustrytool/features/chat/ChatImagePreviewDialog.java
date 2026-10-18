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
                            solim.modifier.Values.placeholder(Icon.image),
                            solim.modifier.Values.fallback(Icon.cancel),
                            solim.modifier.Values.scaling(Scaling.fit),
                            solim.modifier.Values.size(dvw(95f), dvh(80f))
                    );
        });
    }
}
