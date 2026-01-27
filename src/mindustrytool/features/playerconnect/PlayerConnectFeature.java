package mindustrytool.features.playerconnect;

import java.util.Optional;

import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.IntFormat;
import mindustry.ui.Styles;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class PlayerConnectFeature implements Feature {
    private CreateRoomDialog createRoomDialog;
    private JoinRoomDialog joinRoomDialog;
    private PlayerConnectJoinInjector injector;
    private PlayerConnectSettingDialog settingDialog;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.player-connect.name")
                .description("@feature.player-connect.description")
                .icon(Icon.planet)
                .order(3)
                .build();
    }

    @Override
    public void init() {
        createRoomDialog = new CreateRoomDialog();
        joinRoomDialog = new JoinRoomDialog();
        injector = new PlayerConnectJoinInjector();

        if (Vars.ui.join != null) {
            Vars.ui.join.shown(() -> {
                injector.inject(Vars.ui.join);
            });
        }
    }

    @Override
    public void onEnable() {
        if (Vars.ui.hudGroup != null) {
            Table parent = Vars.ui.hudGroup.find("fps/ping");

            if (parent == null) {
                Log.err("fps/ping not found");
                return;
            }

            IntFormat ping = new IntFormat("ping");

            parent.label(() -> ping.get(PlayerConnect.ping))
                    .visible(() -> PlayerConnect.isHosting())
                    .left()
                    .style(Styles.outlineLabel)
                    .name("pc-ping").get();

            parent.row();
        }
    }

    @Override
    public void onDisable() {
        if (createRoomDialog != null)
            createRoomDialog.hide();
        if (joinRoomDialog != null)
            joinRoomDialog.hide();
    }

    public CreateRoomDialog getCreateRoomDialog() {
        return createRoomDialog;
    }

    public JoinRoomDialog getJoinRoomDialog() {
        return joinRoomDialog;
    }

    @Override
    public Optional<Dialog> setting() {
        if (settingDialog == null) {
            settingDialog = new PlayerConnectSettingDialog();
        }
        return Optional.of(settingDialog);
    }
}
