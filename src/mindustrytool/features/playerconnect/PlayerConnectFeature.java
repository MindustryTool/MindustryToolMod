package mindustrytool.features.playerconnect;

import java.util.Optional;

import arc.Events;
import arc.scene.ui.Dialog;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
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
        settingDialog = new PlayerConnectSettingDialog();

        Events.on(ClientLoadEvent.class, e -> {
            if (Vars.ui.join != null) {
                // Initial injection attempt
                injector.inject(Vars.ui.join);

                // Re-inject when dialog is shown to handle cases where UI is cleared
                Vars.ui.join.shown(() -> {
                    injector.inject(Vars.ui.join);
                });
            }
        });
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
        return Optional.of(settingDialog);
    }
}
