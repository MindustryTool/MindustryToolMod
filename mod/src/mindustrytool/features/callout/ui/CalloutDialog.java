package mindustrytool.features.callout.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustrytool.components.WebStyles;
import mindustrytool.features.callout.CalloutFeature;
import solim.core.Component;
import solim.layout.Card;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class CalloutDialog extends SolimDialog {

    private static class ActionDef {
        final Drawable icon;
        final String bundleKey;
        final String fallbackText;

        ActionDef(Drawable icon, String bundleKey, String fallbackText) {
            this.icon = icon;
            this.bundleKey = bundleKey;
            this.fallbackText = fallbackText;
        }
    }

    private final CalloutFeature feature;
    private final Signal<Boolean> teamChatSignal;

    public CalloutDialog(CalloutFeature feature) {
        super(Core.bundle.get("feature.callout.title"));

        this.feature = feature;
        this.teamChatSignal = Signal.of(Boolean.TRUE.equals(feature.teamChatConfig.get()));

        name("calloutDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(560f);

        Readable<Boolean> isAllChat = teamChatSignal.map(v -> !Boolean.TRUE.equals(v));

        children(() -> {
            new Component() {
                @Override
                protected Element build() {
                    return column().grow().center().padding(unit(2f)).gap(unit(2f)).children(() -> {
                        scroll().grow().scrollX(false).children(() -> {
                            column().growX().gap(unit(2f)).center().children(() -> {
                                destinationCard(isAllChat);
                                resourcesCard();
                                tacticalCard();
                                communicationCard();
                            });
                        });
                    }).element();
                }
            };
        });
    }

    private Card sectionCard() {
        return card()
                .growX()
                .padding(unit(2.5f))
                .rounded(8, WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.SECTION_BORDER)
                .gap(unit(2));
    }

    private Component destinationCard(Readable<Boolean> isAllChat) {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.chat).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.callout.settings.general"))
                        .color(Color.white)
                        .left();

                spacer();

                row().gap(unit(1.5f)).children(() -> {
                    button(() -> teamChatSignal.set(true))
                            .style(WebStyles.filterChip())
                            .checked(teamChatSignal)
                            .height(unit(8))
                            .paddingX(unit(2.5f))
                            .children(() -> text(Core.bundle.get("feature.callout.destination.team")));

                    button(() -> teamChatSignal.set(false))
                            .style(WebStyles.filterChip())
                            .checked(isAllChat)
                            .height(unit(8))
                            .paddingX(unit(2.5f))
                            .children(() -> text(Core.bundle.get("feature.callout.destination.all")));
                });
            });
        });
    }

    private Component resourcesCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.distribution).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.callout.section.resources"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            wrap().growX().gap(unit(1.5f)).children(() -> {
                Seq<Item> items = getCalloutItems();
                for (int i = 0; i < items.size; i++) {
                    Item it = items.get(i);
                    button()
                            .style(WebStyles.actionChip())
                            .paddingX(unit(2f))
                            .paddingY(unit(1.2f))
                            .onClick(() -> {
                                String name = it.localizedName;
                                String text = Core.bundle.format("feature.callout.need-resource", name) + name;
                                feature.sendCallout(text, Boolean.TRUE.equals(teamChatSignal.peek()));
                            })
                            .tooltip(it.localizedName)
                            .children(() -> {
                                row().gap(unit(1.5f)).center().children(() -> {
                                    if (it.uiIcon != null) {
                                        image(new TextureRegionDrawable(it.uiIcon)).size(unit(5f));
                                    }
                                    text(it.localizedName);
                                });
                            });
                }
            });
        });
    }

    private Component tacticalCard() {
        ActionDef[] actions = {
                new ActionDef(Icon.warning, "feature.callout.action.attack", "Attack!"),
                new ActionDef(Icon.left, "feature.callout.action.retreat", "Retreat!"),
                new ActionDef(Icon.defense, "feature.callout.action.defend-core", "Defend the core!"),
                new ActionDef(Icon.warning, "feature.callout.action.help", "Help!"),
                new ActionDef(Icon.warning, "feature.callout.action.help-me", "Help me!")
        };

        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.warning).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.callout.section.tactical"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            wrap().growX().gap(unit(1.5f)).children(() -> {
                for (ActionDef act : actions) {
                    String label = Core.bundle.get(act.bundleKey, act.fallbackText);
                    button()
                            .style(WebStyles.actionChip())
                            .paddingX(unit(2f))
                            .paddingY(unit(1.2f))
                            .onClick(() -> feature.sendCallout(label, Boolean.TRUE.equals(teamChatSignal.peek())))
                            .children(() -> {
                                row().gap(unit(1.5f)).center().children(() -> {
                                    if (act.icon != null) {
                                        icon(act.icon).size(unit(5f));
                                    }
                                    text(label);
                                });
                            });
                }
            });
        });
    }

    private Component communicationCard() {
        ActionDef[] comms = {
                new ActionDef(Icon.ok, "feature.callout.comm.yes", "Yes"),
                new ActionDef(Icon.cancel, "feature.callout.comm.no", "No"),
                new ActionDef(Icon.play, "feature.callout.comm.ready", "Ready"),
                new ActionDef(Icon.pause, "feature.callout.comm.wait", "Wait"),
                new ActionDef(Icon.cancel, "feature.callout.comm.stop", "Stop")
        };

        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.chat).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.callout.section.communication"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            wrap().growX().gap(unit(1.5f)).children(() -> {
                for (ActionDef comm : comms) {
                    String label = Core.bundle.get(comm.bundleKey, comm.fallbackText);
                    button()
                            .style(WebStyles.actionChip())
                            .paddingX(unit(2f))
                            .paddingY(unit(1.2f))
                            .onClick(() -> feature.sendCallout(label, Boolean.TRUE.equals(teamChatSignal.peek())))
                            .children(() -> {
                                row().gap(unit(1.5f)).center().children(() -> {
                                    if (comm.icon != null) {
                                        icon(comm.icon).size(unit(5f));
                                    }
                                    text(label);
                                });
                            });
                }
            });
        });
    }

    private Seq<Item> getCalloutItems() {
        Seq<Item> list = new Seq<>();
        Item[] defaults = {
                Items.silicon, Items.thorium, Items.surgeAlloy, Items.plastanium, Items.phaseFabric,
                Items.copper, Items.lead, Items.titanium, Items.graphite, Items.coal
        };
        for (Item item : defaults) {
            if (item != null) {
                list.add(item);
            }
        }
        if (list.isEmpty() && Vars.content != null) {
            String[] names = {
                    "silicon", "thorium", "surge-alloy", "plastanium", "phase-fabric",
                    "copper", "lead", "titanium", "graphite", "coal"
            };
            for (String n : names) {
                Item it = Vars.content.item(n);
                if (it != null) {
                    list.add(it);
                }
            }
        }
        return list;
    }
}
