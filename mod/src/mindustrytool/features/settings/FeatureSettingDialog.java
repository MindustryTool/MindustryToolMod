package mindustrytool.features.settings;

import arc.Core;
import arc.scene.Element;
import arc.scene.Group;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import mindustry.Vars;
import mindustrytool.Config;
import mindustrytool.utils.JsonUtils;
import solim.overlay.SolimDialog;
import mindustrytool.components.FileIcon;

public final class FeatureSettingDialog extends SolimDialog {
    public FeatureSettingDialog() {
        name("featureSettingDialog");
        addCloseButton();
        closeOnBack();
        children(() -> new FeatureSettingsView());

        actionButton(
                Core.bundle.get("feature.button.report-bug"),
                FileIcon.of("info.png"),
                200f,
                64f,
                () -> Core.app.openURI(Config.DISCORD_INVITE_URL));

        if (!Vars.mobile) {
            actionButton(
                    Core.bundle.get("feature.button.copy-ui-tree", "Copy UI tree"),
                    200f,
                    64f,
                    () -> {
                        UiNode root = new UiNode(
                                "Scene", 0f, 0f, Core.graphics.getWidth(), Core.graphics.getHeight(),
                                new ArrayList<>());

                        discoverSolimElements(
                                root,
                                Core.scene.root,
                                element -> "featureSettingDialog".equals(element.name));

                        Core.app.setClipboardText(JsonUtils.toJsonPretty(root));
                    });
        }
    }

    private void discoverSolimElements(UiNode parent, Element element, Predicate<Element> pred) {
        if (pred.test(element)) {
            parent.children.add(buildUiTree(element));
            return;
        }

        if (element instanceof Group group) {
            for (Element child : group.getChildren()) {
                discoverSolimElements(parent, child, pred);
            }
        }
    }

    private UiNode buildUiTree(Element element) {
        UiNode node = createNode(element);

        if (element instanceof Group group) {
            for (Element child : group.getChildren()) {
                node.children.add(buildUiTree(child));
            }
        }

        return node;
    }

    private UiNode createNode(Element element) {
        return new UiNode(
                element.getClass().getSimpleName(),
                element.x,
                element.y,
                element.getWidth(),
                element.getHeight(),
                new ArrayList<>());
    }

    @AllArgsConstructor
    @Data
    private static class UiNode {
        public String name;
        public float x, y;
        public float width, height;
        public List<UiNode> children;
    }
}
