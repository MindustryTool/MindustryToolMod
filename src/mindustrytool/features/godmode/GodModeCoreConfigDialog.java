package mindustrytool.features.godmode;

import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustrytool.services.TapListener;

public class GodModeCoreConfigDialog extends BaseDialog {
    public GodModeCoreConfigDialog(Block block, GodModeDialogs.CorePlaceConsumer onPlace) {
        super("Place Core");
        addCloseButton();

        cont.table(t -> {
            Team[] selectedTeam = { Vars.player.team() };

            t.button(b -> {
                b.image(Tex.whiteui).color(selectedTeam[0].color).size(24).padRight(5);
                b.label(() -> selectedTeam[0].name);
            }, () -> {
                new GodModeTeamSelectionDialog(team -> selectedTeam[0] = team).show();
            }).size(150, 40).pad(5).row();

            t.button("Place", Icon.hammer, () -> {
                remove();
                TapListener.getInstance().select((x, y) -> {
                    onPlace.accept(block, selectedTeam[0], x, y);
                });
            }).growX().pad(5).maxWidth(300);
        }).maxWidth(800);
    }
}
