package mindustrytool.features.playerconnect;

import java.util.Optional;

import arc.scene.ui.Dialog;
import mindustry.Vars;
import mindustry.gen.Icon;
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
        // Feature enabled
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
