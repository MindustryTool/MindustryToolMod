package mindustrytool.features.settings;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Signal;

import java.util.Arrays;
import java.util.List;

public class SolimDemoView extends Component {

    private final Signal<Integer> counter = Signal.of(0);
    private final Signal<String> textInput = Signal.of("");
    private final Signal<Float> sliderValue = Signal.of(0.5f);
    private final Signal<Boolean> checkboxValue = Signal.of(false);
    private final Signal<Color> pickedColor = Signal.of(Color.valueOf("ff6b6b"));
    private final Signal<String> dropdownValue = Signal.of("Option 1");

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            basicComponentsCard();
            layoutComponentsCard();
            reactiveBindingsCard();
            interactiveComponentsCard();
            stylingCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(
                Icon.box,
                "Solim UI Demo",
                "DEMO",
                "Interactive demonstration of Solim UI framework components, layouts, reactivity, and styling.");
    }

    private Component basicComponentsCard() {
        return SettingCard.section(Icon.pencil, "Basic Components", () -> {
            text("Text Input")
                    .props(color(Pal.accent), left());

            textField(textInput)
                    .props(growX(), style(WebStyles.clearInput()), placeholder("Type something..."));

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Slider")
                    .props(color(Pal.accent), left());

            SettingSlider.of(
                    "",
                    sliderValue,
                    0f, 1f, 0.01f,
                    sliderValue.map(v -> Math.round(v * 100) + "%")
            );

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                checkbox("", checkboxValue)
                        .props(tooltip("Checkbox example"));
                text("Checkbox")
                        .props(color(Color.white), left(), growX());
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Dropdown")
                    .props(color(Pal.accent), left());

            row().props(growX(), gap(unit(2))).children(() -> {
                List<String> options = Arrays.asList("Option 1", "Option 2", "Option 3", "Option 4");
                select(dropdownValue, options)
                        .props(growX());
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Color Picker")
                    .props(color(Pal.accent), left());

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                button(() -> {
                    Color[] colors = {
                        Color.valueOf("ff6b6b"),
                        Color.valueOf("4ecdc4"),
                        Color.valueOf("ffe66d"),
                        Color.valueOf("95e1d3"),
                        Color.valueOf("f38181"),
                        Color.valueOf("aa96da")
                    };
                    Color current = pickedColor.get();
                    int idx = 0;
                    for (int i = 0; i < colors.length; i++) {
                        if (colors[i].equals(current)) {
                            idx = (i + 1) % colors.length;
                            break;
                        }
                    }
                    pickedColor.set(colors[idx]);
                })
                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)),
                                tooltip("Cycle colors"), color(pickedColor))
                        .children(() -> icon(Icon.pencil).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                text(pickedColor.map(c -> "#" + c.toString().substring(2).toUpperCase()))
                        .props(color(Color.white), left(), growX());
            });
        });
    }

    private Component layoutComponentsCard() {
        return SettingCard.section(Icon.pencil, "Layout Components", () -> {
            text("row() - Horizontal layout")
                    .props(color(Pal.accent), left());

            row().props(growX(), gap(unit(2)), center(), padding(unit(2)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1f, WebStyles.Colors.SECTION_BORDER)).children(() -> {
                button(() -> {}).props(style(WebStyles.secondary())).children(() -> text("Left"));
                button(() -> {}).props(style(WebStyles.secondary())).children(() -> text("Center"));
                button(() -> {}).props(style(WebStyles.secondary())).children(() -> text("Right"));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("column() - Vertical layout")
                    .props(color(Pal.accent), left());

            column().props(growX(), gap(unit(2)), padding(unit(2)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1f, WebStyles.Colors.SECTION_BORDER)).children(() -> {
                text("Top item").props(color(Color.white));
                text("Middle item").props(color(Color.white));
                text("Bottom item").props(color(Color.white));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("wrap() - Flow layout")
                    .props(color(Pal.accent), left());

            wrap().props(growX(), top(), left(), gap(unit(2))).children(() -> {
                for (int i = 0; i < 6; i++) {
                    int finalI = i;
                    button(() -> {}).props(style(WebStyles.filterChip()), height(unit(9)), prefHeight(unit(9)))
                            .children(() -> text("Item " + (finalI + 1)));
                }
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Card Component")
                    .props(color(Pal.accent), left());

            card().props(padding(unit(3)), rounded(unit(3), WebStyles.Colors.SECTION_BG), border(1f, WebStyles.Colors.SECTION_BORDER)).children(() -> {
                column().props(gap(unit(1))).children(() -> {
                    text("Card Title").props(color(Pal.accent));
                    text("This is a card component with padding, rounded corners, and border.").props(color(WebStyles.Colors.GHOST_FG), textWrap(true), growX());
                    row().props(growX(), right(), gap(unit(1)), marginTop(unit(2))).children(() -> {
                        button(() -> {}).props(style(WebStyles.primary()), height(unit(9)), prefHeight(unit(9)))
                                .children(() -> text("Action"));
                        button(() -> {}).props(style(WebStyles.ghost()), height(unit(9)), prefHeight(unit(9)))
                                .children(() -> text("Cancel"));
                    });
                });
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Divider")
                    .props(color(Pal.accent), left());

            column().props(gap(unit(1))).children(() -> {
                text("Above divider").props(color(Color.white));
                divider().props(color(WebStyles.Colors.SECTION_BORDER));
                text("Below divider").props(color(Color.white));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Spacer")
                    .props(color(Pal.accent), left());

            row().props(growX(), center()).children(() -> {
                text("Left").props(color(Color.white));
                spacer();
                text("Right").props(color(Color.white));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Tooltip (hover me)")
                    .props(color(Pal.accent), left(), tooltip("This is a tooltip!"));

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Badge")
                    .props(color(Pal.accent), left());

            row().props(gap(unit(1))).children(() -> {
                badge("Default");
                badge("Primary").props(color(Color.white), background(WebStyles.Colors.PRIMARY));
                badge("Success").props(color(Color.white), background(Color.green));
                badge("Warning").props(color(Color.white), background(Color.yellow));
                badge("Error").props(color(Color.white), background(Color.scarlet));
            });
        });
    }

    private Component reactiveBindingsCard() {
        return SettingCard.section(Icon.refresh, "Reactive Bindings", () -> {
            text("Signal<Integer> counter = Signal.of(0);")
                    .props(color(WebStyles.Colors.GHOST_FG), fontScale(0.8f), textWrap(true), growX());

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                text("Reactive Value: " + counter.map(String::valueOf).get())
                        .props(color(Pal.accent), growX(), left(), fontScale(1.2f));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Computed<String> doubled = counter.map(i -> \"Doubled: \" + (i * 2));")
                    .props(color(WebStyles.Colors.GHOST_FG), fontScale(0.8f), textWrap(true), growX());

            row().props(growX(), gap(unit(2)), center(), marginTop(unit(1))).children(() -> {
                text(counter.map(i -> "Doubled: " + (i * 2)))
                        .props(color(Color.green), growX(), left(), fontScale(1.2f));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Signal.computed(() -> counter.get() > 5 ? \"High\" : \"Low\")")
                    .props(color(WebStyles.Colors.GHOST_FG), fontScale(0.8f), textWrap(true), growX());

            row().props(growX(), gap(unit(2)), center(), marginTop(unit(1))).children(() -> {
                text(counter.map(i -> i > 5 ? "High" : "Low"))
                        .props(color(counter.map(i -> i > 5 ? Color.red : Color.blue)), growX(), left(), fontScale(1.2f));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Binding to component properties:")
                    .props(color(WebStyles.Colors.GHOST_FG), fontScale(0.8f), textWrap(true), growX());

            row().props(growX(), gap(unit(2)), center(), marginTop(unit(1))).children(() -> {
                button(() -> {}).props(
                                style(WebStyles.secondary()),
                                height(unit(11)), prefHeight(unit(11)),
                                visible(counter.map(i -> i >= 0)),
                                enabled(counter.map(i -> i < 10)),
                                color(counter.map(i -> i > 5 ? Color.red : Color.green))
                        )
                        .children(() -> text("Visible & Enabled (count < 10)"));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Reactive grid with keyed items:")
                    .props(color(WebStyles.Colors.GHOST_FG), fontScale(0.8f), textWrap(true), growX());

            Signal<Seq<String>> items = Signal.of(Seq.with("A", "B", "C"));
            reactiveGrid(items)
                    .key(s -> s)
                    .props(gap(unit(1)))
                    .items(s -> {
                        return card()
                                .props(padding(unit(2)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1f, WebStyles.Colors.SECTION_BORDER))
                                .children(() -> text(s).props(color(Color.white), center()));
                    });

            row().props(growX(), gap(unit(1)), center(), marginTop(unit(2))).children(() -> {
                button(() -> items.update(seq -> {
                    Seq<String> copy = seq.copy();
                    copy.add("Item " + (copy.size + 1));
                    return copy;
                }))
                        .props(style(WebStyles.secondary()), height(unit(9)), prefHeight(unit(9)))
                        .children(() -> icon(Icon.add).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));

                button(() -> items.update(seq -> {
                    if (seq.isEmpty()) return seq;
                    Seq<String> copy = seq.copy();
                    copy.remove(copy.size - 1);
                    return copy;
                }))
                        .props(style(WebStyles.secondary()), height(unit(9)), prefHeight(unit(9)))
                        .children(() -> icon(Icon.cancel).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));
            });
        });
    }

    private Component interactiveComponentsCard() {
        return SettingCard.section(Icon.play, "Interactive Components", () -> {
            text("Counter")
                    .props(color(Pal.accent), left());

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                text(counter.map(String::valueOf))
                        .props(color(Color.white), growX(), center(), fontScale(2f));
            });

            row().props(growX(), gap(unit(2)), center(), marginTop(unit(2))).children(() -> {
                button(() -> counter.update(v -> v - 1))
                        .props(style(WebStyles.secondary()), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                        .children(() -> icon(Icon.cancel).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                button(() -> counter.set(0))
                        .props(style(WebStyles.outline()), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                        .children(() -> icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                button(() -> counter.update(v -> v + 1))
                        .props(style(WebStyles.primary()), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                        .children(() -> icon(Icon.add).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Button Styles:")
                    .props(color(Pal.accent), left());

            wrap().props(gap(unit(1))).children(() -> {
                button(() -> {}).props(style(WebStyles.ghost()), height(unit(11)), prefHeight(unit(11)))
                        .children(() -> text("Ghost"));
                button(() -> {}).props(style(WebStyles.secondary()), height(unit(11)), prefHeight(unit(11)))
                        .children(() -> text("Secondary"));
                button(() -> {}).props(style(WebStyles.primary()), height(unit(11)), prefHeight(unit(11)))
                        .children(() -> text("Primary"));
                button(() -> {}).props(style(WebStyles.outline()), height(unit(11)), prefHeight(unit(11)))
                        .children(() -> text("Outline"));
                button(() -> {}).props(style(WebStyles.danger()), height(unit(11)), prefHeight(unit(11)))
                        .children(() -> text("Danger"));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Icon Buttons:")
                    .props(color(Pal.accent), left());

            wrap().props(gap(unit(1))).children(() -> {
                button(() -> {})
                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip("Settings"))
                        .children(() -> icon(Icon.settings).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
                button(() -> {})
                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip("Refresh"))
                        .children(() -> icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
                button(() -> {})
                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip("Download"))
                        .children(() -> icon(Icon.down).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
                button(() -> {})
                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip("Upload"))
                        .children(() -> icon(Icon.up).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
                button(() -> {})
                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip("Delete"))
                        .children(() -> icon(Icon.cancel).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
                button(() -> {})
                        .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip("Edit"))
                        .children(() -> icon(Icon.pencil).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
            });
        });
    }

    private Component stylingCard() {
        return SettingCard.section(Icon.pencil, "Styling & Themes", () -> {
            text("Color Palette:")
                    .props(color(Pal.accent), left());

            wrap().props(gap(unit(1))).children(() -> {
                Color[] colors = {
                    Color.valueOf("ff6b6b"), Color.valueOf("4ecdc4"), Color.valueOf("ffe66d"),
                    Color.valueOf("95e1d3"), Color.valueOf("f38181"), Color.valueOf("aa96da"),
                    Color.valueOf("fcbad3"), Color.valueOf("a8d8ea"), Color.valueOf("ffd3b6"),
                    Color.valueOf("ffaaa5"), Color.valueOf("ff8b94"), Color.valueOf("c5e0b4")
                };
                for (Color c : colors) {
                    Color finalC = c;
                    button(() -> pickedColor.set(finalC))
                            .props(style(WebStyles.ghost()), size(unit(9)), prefWidth(unit(9)), prefHeight(unit(9)),
                                    color(c), tooltip(c.toString()))
                            .children(() -> {});
                }
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Text Styles:")
                    .props(color(Pal.accent), left());

            column().props(gap(unit(1))).children(() -> {
                text("Default text").props(color(Color.white));
                text("Ghost text").props(color(WebStyles.Colors.GHOST_FG));
                text("Accent text").props(color(Pal.accent));
                text("Large text").props(color(Color.white), fontScale(1.5f));
                text("Small text").props(color(Color.white), fontScale(0.75f));
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            text("Background Colors:")
                    .props(color(Pal.accent), left());

            wrap().props(gap(unit(2))).children(() -> {
                Color[] bgColors = {
                    WebStyles.Colors.SECTION_BG,
                    WebStyles.Colors.SECONDARY,
                    WebStyles.Colors.CLEAR_BG,
                    Color.darkGray,
                    Color.gray
                };
                for (Color c : bgColors) {
                    Color finalC = c;
                    card()
                            .props(padding(unit(2)), rounded(unit(2), finalC), border(1f, WebStyles.Colors.SECTION_BORDER), size(unit(20)), prefWidth(unit(20)), prefHeight(unit(20)))
                            .children(() -> text(finalC.toString()).props(color(Color.white), center(), fontScale(0.7f), textWrap(true)));
                }
            });
        });
    }
}
