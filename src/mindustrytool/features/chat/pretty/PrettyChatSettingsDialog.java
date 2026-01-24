package mindustrytool.features.chat.pretty;

import arc.Core;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.chat.pretty.PrettyChatFeature.Prettier;

public class PrettyChatSettingsDialog extends BaseDialog {
    private String previewMessage = "Hello World! This is a test message.";
    private Table cards;

    public PrettyChatSettingsDialog() {
        super("@feature.prettychat.settings.title");

        addCloseButton();

        setup();
    }

    private void setup() {
        cont.clear();

        Table inputTable = new Table();
        TextField field = new TextField(previewMessage);
        field.setMessageText("Type to preview...");
        field.changed(() -> {
            previewMessage = field.getText();
            rebuildCards();
        });

        inputTable.add(field).width(800f).pad(10).row();

        inputTable.add(new Table(t -> {
            t.add("Result: ").color(arc.graphics.Color.lightGray);
            t.label(() -> PrettyChatFeature.transform(previewMessage)).color(mindustry.graphics.Pal.accent).wrap()
                    .growX();
        })).width(800f).pad(10).row();

        cont.add(inputTable).growX().padBottom(20f).row();

        cards = new Table();
        ScrollPane pane = new ScrollPane(cards);

        cont.add(pane).grow().row();
        cont.add("[red]ONLY WORK ON DESKTOP");

        rebuildCards();
    }

    private void rebuildCards() {
        cards.clear();
        cards.top();

        Seq<String> enabledIds = PrettyChatConfig.getEnabledIds();
        Seq<Prettier> prettiers = new Seq<>(PrettyChatFeature.getPrettiers());

        prettiers.sort((p1, p2) -> {
            int idx1 = enabledIds.indexOf(p1.getId());
            int idx2 = enabledIds.indexOf(p2.getId());

            if (idx1 != -1 && idx2 != -1) {
                return Integer.compare(idx1, idx2);
            }
            if (idx1 != -1)
                return -1;
            if (idx2 != -1)
                return 1;
            return 0;
        });

        for (int i = 0; i < prettiers.size; i++) {
            Prettier p = prettiers.get(i);
            boolean isEnabled = enabledIds.contains(p.getId());
            int index = i;

            Table card = new Table(Styles.black6);
            card.margin(10);

            Runnable toggleAction = () -> {
                Seq<String> current = PrettyChatConfig.getEnabledIds();
                if (current.contains(p.getId())) {
                    current.remove(p.getId());
                } else {
                    current.add(p.getId());
                }
                PrettyChatConfig.setEnabledIds(current);
                rebuildCards();
            };

            card.touchable(() -> arc.scene.event.Touchable.enabled);
            card.clicked(toggleAction);

            Table info = new Table();
            info.touchable(() -> arc.scene.event.Touchable.disabled);
            info.add(p.getName()).left().color(isEnabled ? mindustry.graphics.Pal.accent : arc.graphics.Color.white)
                    .row();

            String transformed = PrettyChatFeature.transform(previewMessage, p);
            info.add(transformed).left().color(arc.graphics.Color.lightGray).ellipsis(true)
                    .labelAlign(arc.util.Align.left)
                    .width(0f).growX();

            card.add(info).growX().padRight(10);

            if (isEnabled) {
                if (index > 0) {
                    if (enabledIds.contains(prettiers.get(index - 1).getId())) {
                        card.button(Icon.up, Styles.emptyi, () -> {
                            move(index, -1, prettiers);
                        }).size(40);
                    } else {
                        card.add().size(40);
                    }
                } else {
                    card.add().size(40);
                }

                if (index < prettiers.size - 1) {
                    if (enabledIds.contains(prettiers.get(index + 1).getId())) {
                        card.button(Icon.down, Styles.emptyi, () -> {
                            move(index, 1, prettiers);
                        }).size(40);
                    } else {
                        card.add().size(40);
                    }
                } else {
                    card.add().size(40);
                }
            }

            card.button(Icon.edit, Styles.emptyi, () -> {
                showEditDialog(p);
            }).size(40).padRight(10);

            if (isEnabled) {
                card.setBackground(Tex.buttonOver);
            }

            cards.add(card).growX().maxWidth(800f).pad(5).row();
        }
    }

    private void move(int index, int offset, Seq<Prettier> displayList) {
        if (index + offset >= 0 && index + offset < displayList.size) {
            Seq<String> enabledIds = PrettyChatConfig.getEnabledIds();
            String id1 = displayList.get(index).getId();
            String id2 = displayList.get(index + offset).getId();

            int idx1 = enabledIds.indexOf(id1);
            int idx2 = enabledIds.indexOf(id2);

            if (idx1 != -1 && idx2 != -1) {
                enabledIds.swap(idx1, idx2);
                PrettyChatConfig.setEnabledIds(enabledIds);
                rebuildCards();
            }
        }
    }

    private void showEditDialog(Prettier p) {
        BaseDialog dialog = new BaseDialog("Edit Script");

        dialog.name = "prettyChatEditDialog";

        TextArea area = new TextArea(p.getScript());

        dialog.cont.add("Script for " + p.getName()).padBottom(10).row();
        dialog.cont.add(area).size(500, 300).row();
        dialog.cont.label(() -> "Use <message> as placeholder").color(arc.graphics.Color.lightGray).pad(5).row();

        dialog.buttons.button("@close", () -> {
            dialog.hide();
            dialog.remove();
        }).size(150, 50).pad(5);

        dialog.buttons.button("Reset to Default", () -> {
            area.setText(p.getDefaultScript());
        }).size(150, 50).pad(5);

        dialog.buttons.button("Save", () -> {
            if (area.getText().equals(p.getDefaultScript())) {
                PrettyChatConfig.resetScript(p.getId());
            } else {
                PrettyChatConfig.setScript(p.getId(), area.getText());
            }
            rebuildCards();
            dialog.hide();
        }).size(150, 50).pad(5);

        Core.app.post(() -> dialog.show());
    }
}
