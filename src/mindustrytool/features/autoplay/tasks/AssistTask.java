package mindustrytool.features.autoplay.tasks;

import arc.scene.style.Drawable;
import mindustry.Vars;
import mindustry.ai.types.BuilderAI;
import mindustry.entities.units.AIController;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;

public class AssistTask implements AutoplayTask {
    private boolean enabled = true;
    private final BuilderAI ai = new BuilderAI();

    @Override
    public String getName() {
        return "Auto Build " + Iconc.unitPoly;
    }

    @Override
    public Drawable getIcon() {
        return Icon.players;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean shouldRun() {
        if (!Vars.player.unit().canBuild()) {
            return false;
        }

        return !Vars.player.team().data().plans.isEmpty();
    }

    @Override
    public AIController getAI() {
        return ai;
    }
}
