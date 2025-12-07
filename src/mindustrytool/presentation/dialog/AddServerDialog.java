package mindustrytool.presentation.dialog;

import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.core.model.ServerHost;
import mindustrytool.presentation.builder.ServerFieldBuilder;
import mindustrytool.feature.playerconnect.network.PlayerConnectProviders;

public class AddServerDialog extends BaseDialog {
    private ServerHost renaming;
    private int renamingIndex = -1;
    private final String[] edit = {"", ""};
    private final ServerHost temp = new ServerHost();
    private final Runnable onRefresh;

    public AddServerDialog(Runnable onRefresh) {
        super("@joingame.title");
        this.onRefresh = onRefresh;
        buttons.defaults().size(140f, 60f).pad(4f);
        ServerFieldBuilder.build(cont, edit, temp);
        shown(() -> {
            title.setText(renaming != null ? "@server.edit" : "@server.add");
            if (renaming != null) { edit[0] = renaming.name; edit[1] = renaming.get(); }
            else { edit[0] = edit[1] = ""; }
        });
        buttons.button("@cancel", () -> { renaming = null; renamingIndex = -1; edit[0] = edit[1] = ""; hide(); });
        buttons.button("@ok", this::save).disabled(b -> !temp.wasValid || edit[0].isEmpty() || edit[1].isEmpty());
    }

    private void save() {
        if (renaming != null) { PlayerConnectProviders.custom.removeIndex(renamingIndex); PlayerConnectProviders.custom.insert(renamingIndex, edit[0], edit[1]); renaming = null; renamingIndex = -1; }
        else { PlayerConnectProviders.custom.put(edit[0], edit[1]); }
        PlayerConnectProviders.saveCustom();
        onRefresh.run();
        hide();
        edit[0] = edit[1] = "";
    }

    public void showEdit(ServerHost host, int index) { renaming = host; renamingIndex = index; show(); }
}
