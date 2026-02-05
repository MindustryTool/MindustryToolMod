package mindustrytool.features.chat.global;

import arc.Core;
import arc.func.Cons;
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

public class AttachContentDialog extends BaseDialog {
    
    public AttachContentDialog(Cons<String> callback) {
        super("@chat.attach-content");
        
        addCloseButton();
        
        cont.table(t -> {
            t.defaults().size(220f, 60f).pad(5);
            
            t.button("@chat.select-file", Icon.file, () -> {
                Vars.platform.showFileChooser(true, "msch", file -> {
                    if (file == null) return;
                    try {
                        byte[] bytes = file.readBytes();
                        String base64 = new String(Base64Coder.encode(bytes));
                        callback.get(base64);
                        hide();
                    } catch (Exception e) {
                        Vars.ui.showException(e);
                    }
                });
            });
            
            t.row();
            
            t.button("@chat.paste-link", Icon.paste, () -> {
                String content = Core.app.getClipboardText();
                if (content != null && !content.isEmpty()) {
                    callback.get(content);
                    hide();
                } else {
                    Vars.ui.showInfoFade("@chat.clipboard-empty");
                }
            });
        }).grow();
    }
}
