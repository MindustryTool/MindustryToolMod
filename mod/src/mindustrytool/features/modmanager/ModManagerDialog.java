package mindustrytool.features.modmanager;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Interp;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;
import arc.util.Align;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.mod.Mods.LoadedMod;
import mindustry.ui.FileChooser;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.modmanager.AttachableDialogBridge.AttachedEntry;
import solim.core.Component;
import solim.display.SolimImage;
import solim.input.Button;
import solim.layout.Direction;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.ForEach;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.style.SolimButtonStyle;

public final class ModManagerDialog extends SolimDialog {

    private final Runnable onOpenBrowser;
    private final Runnable onOpenModpacks;
    private final Runnable onOpenVanillaMods;
    private final Signal<Integer> switcherRevision = Signal.of(0);
    private boolean switchingDialog = false;

    public ModManagerDialog(Runnable onOpenVanillaMods) {
        this(
                () -> new ModBrowserDialog().show(),
                () -> new ModPackerDialog().show(),
                onOpenVanillaMods);
    }

    public ModManagerDialog(
            Runnable onOpenBrowser,
            Runnable onOpenModpacks,
            Runnable onOpenVanillaMods) {
        super(Core.bundle.get("mods", Core.bundle.get("feature.mod-manager.name")));
        this.onOpenBrowser = onOpenBrowser;
        this.onOpenModpacks = onOpenModpacks;
        this.onOpenVanillaMods = onOpenVanillaMods;

        name("heModManagerDialog");
        closeOnBack();
        fillParent(true);
        cont().background(Styles.black8);

        shown(() -> {
            switcherRevision.set(switcherRevision.peek() + 1);
            if (Core.app != null) {
                Core.app.post(() -> switcherRevision.set(switcherRevision.peek() + 1));
            }
            ModService.getInstance().notifyInstalledModsChanged();
            ModService.getInstance().checkInstalledModUpdates();
        });

        hidden(() -> {
            if (!switchingDialog && Vars.mods != null && Vars.mods.requiresReload() && Vars.ui != null) {
                Vars.ui.showConfirm(
                        Core.bundle.get("mod-manager.reload-required"),
                        Core.bundle.get("mod-manager.action.relaunch"),
                        () -> Core.app.exit());
            }
        });

        children(() -> new ModManagerView(
                this.dialog(),
                this.switcherRevision,
                this::hide,
                this::switchToDialog,
                this.onOpenBrowser,
                this.onOpenModpacks,
                this.onOpenVanillaMods));
    }

    private void switchToDialog(Runnable action) {
        switchingDialog = true;
        try {
            dialog().hide(null);
            if (action != null) {
                action.run();
            }
        } finally {
            if (Core.app != null) {
                Core.app.post(() -> switchingDialog = false);
            } else {
                switchingDialog = false;
            }
        }
    }

    private static final class SwitcherItem {
        final String key;
        final Drawable icon;
        final String label;
        final boolean active;
        final Runnable onSelect;

        SwitcherItem(
                String key,
                Drawable icon,
                String label,
                boolean active,
                Runnable onSelect) {
            this.key = key;
            this.icon = icon;
            this.label = label;
            this.active = active;
            this.onSelect = onSelect;
        }
    }

    static final class ModManagerView extends Component {
        private final Dialog selfDialog;
        private final Readable<Integer> switcherRevision;
        private final Runnable onClose;
        private final Cons<Runnable> onSwitchDialog;
        private final Runnable onOpenBrowser;
        private final Runnable onOpenModpacks;
        private final Runnable onOpenVanillaMods;

        private final Signal<String> searchQuery = Signal.of("");
        private final Signal<Integer> localTick = Signal.of(0);
        private final Signal<Boolean> portraitMenuOpen = Signal.of(false);
        private final Signal<Boolean> switcherHovered = Signal.of(false);
        private final Signal<Boolean> portraitSwitchExpanded = Signal.of(false);

        private final Computed<ModSplitSnapshot> snapshot = new Computed<>(() -> {
            ModService service = ModService.getInstance();
            service.installedRevision().get();
            Map<String, String> updates = service.availableUpdates().get();
            localTick.get();

            String query = searchQuery.get() != null
                    ? searchQuery.get().trim().toLowerCase(Locale.ROOT)
                    : "";

            List<LoadedMod> enabledList = new ArrayList<>();
            List<LoadedMod> disabledList = new ArrayList<>();

            if (Vars.mods != null) {
                Seq<LoadedMod> raw = Vars.mods.list();
                if (raw != null) {
                    for (LoadedMod mod : raw) {
                        if (mod == null || !matchesQuery(mod, query)) {
                            continue;
                        }
                        String repo = mod.getRepo();
                        String updateVer = (repo != null && !repo.trim().isEmpty() && updates != null)
                                ? updates.get(repo.trim().toLowerCase(Locale.ROOT))
                                : null;
                        int stat = ModStat.checkModStat(mod, updateVer != null);
                        if (mod.enabled() && ModStat.isValid(stat)) {
                            enabledList.add(mod);
                        } else {
                            disabledList.add(mod);
                        }
                    }
                }
            }

            Comparator<LoadedMod> byName = (a, b) -> displayName(a).compareToIgnoreCase(displayName(b));
            enabledList.sort(byName);
            disabledList.sort(byName);

            boolean reloadRequired = Vars.mods != null && Vars.mods.requiresReload();
            return new ModSplitSnapshot(enabledList, disabledList, reloadRequired);
        });

        private final Computed<List<LoadedMod>> enabledMods = snapshot.map(
                s -> s != null ? s.enabled : new ArrayList<>());

        private final Computed<List<LoadedMod>> disabledMods = snapshot.map(
                s -> s != null ? s.disabled : new ArrayList<>());

        private final Computed<Boolean> reloadRequired = snapshot.map(
                s -> s != null && s.reloadRequired);

        private final Computed<List<SwitcherItem>> switcherItems;

        ModManagerView(
                Dialog selfDialog,
                Readable<Integer> switcherRevision,
                Runnable onClose,
                Cons<Runnable> onSwitchDialog,
                Runnable onOpenBrowser,
                Runnable onOpenModpacks,
                Runnable onOpenVanillaMods) {
            this.selfDialog = selfDialog;
            this.switcherRevision = switcherRevision;
            this.onClose = onClose;
            this.onSwitchDialog = onSwitchDialog;
            this.onOpenBrowser = onOpenBrowser;
            this.onOpenModpacks = onOpenModpacks;
            this.onOpenVanillaMods = onOpenVanillaMods;

            this.switcherItems = new Computed<>(() -> {
                this.switcherRevision.get();
                List<SwitcherItem> items = new ArrayList<>();
                List<AttachedEntry> attached = Vars.ui != null
                        ? AttachableDialogBridge.getEntries(Vars.ui.mods)
                        : Collections.emptyList();

                if (!attached.isEmpty()) {
                    for (AttachedEntry entry : attached) {
                        boolean isSelf = entry.dialog == this.selfDialog;
                        Runnable action = isSelf
                                ? () -> this.switcherHovered.set(false)
                                : () -> {
                                    this.switcherHovered.set(false);
                                    this.portraitSwitchExpanded.set(false);
                                    this.onSwitchDialog.get(() -> {
                                        if (Core.scene != null) {
                                            entry.dialog.show(Core.scene);
                                        } else {
                                            entry.dialog.show();
                                        }
                                    });
                                };
                        items.add(new SwitcherItem(
                                "dialog:" + System.identityHashCode(entry.dialog),
                                entry.icon,
                                entry.description,
                                isSelf,
                                action));
                    }
                } else {
                    items.add(new SwitcherItem(
                            "self",
                            resolveSelfIcon(),
                            Core.bundle.get("mod-manager.switch-from-vanilla"),
                            true,
                            () -> this.switcherHovered.set(false)));
                }

                items.add(new SwitcherItem(
                        "vanilla",
                        Icon.left,
                        Core.bundle.get("back"),
                        false,
                        () -> {
                            this.switcherHovered.set(false);
                            this.portraitSwitchExpanded.set(false);
                            this.onSwitchDialog.get(this.onOpenVanillaMods);
                        }));
                return items;
            });
        }

        private static Drawable resolveSelfIcon() {
            try {
                return FileIcon.of("mindustrytool.png");
            } catch (Throwable ignored) {
                return Icon.book;
            }
        }

        @Override
        protected Element build() {
            effect(() -> {
                this.switcherRevision.get();
                this.switcherHovered.set(false);
                this.portraitSwitchExpanded.set(false);
            });

            boolean portrait = Core.graphics != null && Core.graphics.isPortrait();
            if (portrait) {
                return column().grow().gap(unit(2)).padding(unit(2)).children(() -> {
                    searchBar();
                    portraitBody();
                    relaunchBanner();
                    divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);
                    portraitFooter();
                    portraitSwitcher();
                }).element();
            }

            Readable<Float> mainWidth = dvw(72f).map(w -> {
                if (w != null && w > 200f) {
                    return w;
                }
                if (Core.graphics != null && Core.graphics.getWidth() > 0) {
                    float scl = Math.max(1f, Scl.scl());
                    return (Core.graphics.getWidth() / scl) * 0.72f;
                }
                return 1380f;
            });

            return column().grow().gap(0f).children(() -> {
                divider().height(3f).color(Pal.accent);

                stack()
                        .grow()
                        .layer(() -> column()
                                .grow()
                                .center()
                                .children(() -> column()
                                        .width(mainWidth)
                                        .growY()
                                        .gap(unit(2))
                                        .paddingTop(unit(1.5f))
                                        .paddingBottom(unit(1.5f))
                                        .children(() -> {
                                            searchBar();
                                            landscapeSplitBody();
                                            relaunchBanner();
                                            divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);
                                            landscapeFooter();
                                        })))
                        .layer(() -> column()
                                .grow()
                                .top()
                                .left()
                                .children(this::landscapeSwitcher));
            }).element();
        }

        void landscapeSwitcher() {
            ForEach<SwitcherItem> switcherList = forEach(switcherItems)
                    .key(item -> item.key)
                    .top()
                    .left();
            switcherList.children(this::buildSwitcherButton);
            Element switcherTable = switcherList.element();
            switcherTable.addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
                    if (pointer == -1 && (fromActor == null || !fromActor.isDescendantOf(switcherTable))) {
                        switcherHovered.set(true);
                    }
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
                    if (pointer == -1 && (toActor == null || !toActor.isDescendantOf(switcherTable))) {
                        switcherHovered.set(false);
                    }
                }
            });
        }

        private Component buildSwitcherButton(SwitcherItem item) {
            Button btn = button(item.onSelect)
                    .style(Styles.cleart != null ? Styles.cleart : WebStyles.ghost().style())
                    .growX()
                    .left()
                    .padding(0f)
                    .gap(0f)
                    .children(() -> {
                        row().left().padding(unit(1f)).children(() -> image(item.icon).size(unit(11.5f)));
                        collapser(switcherHovered)
                                .direction(Direction.HORIZONTAL)
                                .duration(0.3f, Interp.pow3Out)
                                .left()
                                .padding(unit(1f))
                                .paddingRight(unit(2f))
                                .children(() -> text(item.label)
                                        .color(item.active ? Pal.accent : Color.white));
                    });
            Element buttonElement = btn.element();
            buttonElement.addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
                    if (pointer == -1 && (fromActor == null || !fromActor.isDescendantOf(buttonElement))) {
                        switcherHovered.set(true);
                    }
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
                    Element root = buttonElement.parent != null ? buttonElement.parent : buttonElement;
                    if (pointer == -1 && (toActor == null || !toActor.isDescendantOf(root))) {
                        switcherHovered.set(false);
                    }
                }
            });
            return btn;
        }

        private void portraitSwitcher() {
            column().growX().gap(0f).children(() -> {
                button(() -> portraitSwitchExpanded.set(!Boolean.TRUE.equals(portraitSwitchExpanded.peek())))
                        .style(Styles.flati != null ? Styles.flati : WebStyles.ghost().style())
                        .growX()
                        .height(unit(8f))
                        .center()
                        .children(() -> {
                            SolimImage arrow = image(Icon.upOpen).size(unit(5f));
                            Element arrowEl = arrow.element();
                            arrowEl.setOrigin(Align.center);
                            final boolean[] initialized = { false };
                            effect(() -> {
                                boolean exp = Boolean.TRUE.equals(portraitSwitchExpanded.get());
                                arrowEl.setOrigin(Align.center);
                                arrowEl.clearActions();
                                if (!initialized[0]) {
                                    initialized[0] = true;
                                    arrowEl.setRotation(exp ? 180f : 0f);
                                } else {
                                    arrowEl.addAction(Actions.rotateTo(exp ? 180f : 0f, 0.3f, Interp.pow3Out));
                                }
                            });
                        });
                collapser(portraitSwitchExpanded)
                        .duration(0.3f, Interp.pow3Out)
                        .growX()
                        .children(() -> card(Styles.grayPanel)
                                .growX()
                                .padding(unit(1.5f))
                                .children(() -> forEach(switcherItems)
                                        .key(item -> item.key)
                                        .growX()
                                        .left()
                                        .children(this::buildPortraitSwitcherButton)));
            });
        }

        private Component buildPortraitSwitcherButton(SwitcherItem item) {
            return button(item.onSelect)
                    .style(Styles.cleart != null ? Styles.cleart : WebStyles.ghost().style())
                    .growX()
                    .left()
                    .padding(unit(1f))
                    .paddingRight(unit(3f))
                    .gap(unit(2f))
                    .children(() -> {
                        image(item.icon).size(unit(11.5f));
                        text(item.label)
                                .color(item.active ? Pal.accent : Color.white);
                    });
        }

        private void searchBar() {
            row().growX().gap(unit(2))
                    .rounded(unit(3))
                    .paddingLeft(unit(2.5f))
                    .paddingRight(unit(2.5f))
                    .height(unit(12f))
                    .center()
                    .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                    .children(() -> {
                        icon(Icon.zoom).size(unit(6)).color(WebStyles.Colors.GHOST_FG);
                        textField(searchQuery)
                                .growX()
                                .style(WebStyles.clearInput())
                                .placeholder(Core.bundle.get("mod-manager.search.placeholder"));
                        button(() -> searchQuery.set(""))
                                .style(WebStyles.ghost())
                                .size(unit(9))
                                .visible(searchQuery.map(q -> q != null && !q.isEmpty()))
                                .children(() -> icon(Icon.cancel).size(unit(5)));
                    });
        }

        private void relaunchBanner() {
            collapser(reloadRequired).growX().children(() -> {
                card()
                        .growX()
                        .padding(unit(1.5f))
                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                        .border(1f, WebStyles.Colors.DANGER)
                        .center()
                        .children(() -> row().gap(unit(2)).center().children(() -> {
                            text(Core.bundle.get("mod-manager.reload-required"))
                                    .color(WebStyles.Colors.DANGER);
                            button(Core.bundle.get("mod-manager.action.relaunch"), () -> Core.app.exit())
                                    .style(WebStyles.dangerText())
                                    .height(unit(9));
                        }));
            });
        }

        private void landscapeSplitBody() {
            row().grow().gap(unit(1.5f)).children(() -> {
                card()
                        .grow()
                        .padding(0f)
                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                        .border(1f, WebStyles.Colors.SECTION_BORDER)
                        .children(() -> {
                            column().grow().top().gap(0f).children(() -> {
                                row().growX().center().paddingTop(unit(1.5f)).paddingBottom(unit(1.5f)).children(() ->
                                        text(enabledMods.map(list -> Core.bundle.format(
                                                "mod-manager.section.enabled",
                                                list != null ? list.size() : 0)))
                                                .color(Pal.accent));
                                divider().height(3f).color(Pal.accent);

                                scroll().grow().scrollX(false).children(() -> {
                                    column().growX().top().padding(unit(1.5f)).children(() -> {
                                        reactiveGrid(enabledMods)
                                                .columns(1)
                                                .key(mod -> mod.name)
                                                .gap(unit(1.5f))
                                                .empty(() -> text(Core.bundle.get("mod-manager.empty.enabled"))
                                                        .color(WebStyles.Colors.GHOST_FG)
                                                        .padding(unit(4)))
                                                .children(mod -> new ModCard(mod, this::bumpTick));
                                    });
                                });
                            });
                        });

                divider(Direction.Y).width(2f).color(WebStyles.Colors.SECTION_BORDER).growY();

                card()
                        .grow()
                        .padding(0f)
                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                        .border(1f, WebStyles.Colors.SECTION_BORDER)
                        .children(() -> {
                            column().grow().top().gap(0f).children(() -> {
                                row().growX().center().paddingTop(unit(1.5f)).paddingBottom(unit(1.5f)).children(() ->
                                        text(disabledMods.map(list -> Core.bundle.format(
                                                "mod-manager.section.disabled",
                                                list != null ? list.size() : 0)))
                                                .color(Pal.accent));
                                divider().height(3f).color(Pal.accent);

                                scroll().grow().scrollX(false).children(() -> {
                                    column().growX().top().padding(unit(1.5f)).children(() -> {
                                        reactiveGrid(disabledMods)
                                                .columns(1)
                                                .key(mod -> mod.name)
                                                .gap(unit(1.5f))
                                                .empty(() -> text(Core.bundle.get("mod-manager.empty.disabled"))
                                                        .color(WebStyles.Colors.GHOST_FG)
                                                        .padding(unit(4)))
                                                .children(mod -> new ModCard(mod, this::bumpTick));
                                    });
                                });
                            });
                        });
            });
        }

        private void portraitBody() {
            card()
                    .grow()
                    .padding(unit(1.5f))
                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                    .border(1f, WebStyles.Colors.SECTION_BORDER)
                    .children(() -> {
                        column().grow().top().gap(unit(1.5f)).children(() -> {
                            button(() -> portraitMenuOpen.set(!Boolean.TRUE.equals(portraitMenuOpen.peek())))
                                    .style(WebStyles.secondary())
                                    .growX()
                                    .height(unit(10))
                                    .gap(unit(1.5f))
                                    .children(() -> {
                                        icon(portraitMenuOpen.map(open -> Boolean.TRUE.equals(open)
                                                ? Icon.upOpen
                                                : Icon.menuSmall)).size(unit(5));
                                        text(Core.bundle.get("feature.mod-manager.name"));
                                    });

                            collapser(portraitMenuOpen).growX().children(() -> {
                                column().growX().gap(unit(1)).padding(unit(1)).children(() -> {
                                    heliumActionButton(
                                            Core.bundle.get("mod-manager.action.browser"),
                                            Icon.planet,
                                            WebStyles.outline(),
                                            onOpenBrowser);
                                    heliumActionButton(
                                            Core.bundle.get("mod-manager.action.import-file"),
                                            Icon.file,
                                            WebStyles.secondary(),
                                            this::importModFile);
                                    heliumActionButton(
                                            Core.bundle.get("mod-manager.action.import-github"),
                                            Icon.download,
                                            WebStyles.secondary(),
                                            this::showImportGithubDialog);
                                    heliumActionButton(
                                            Core.bundle.get("mod-manager.action.modpack"),
                                            Icon.list,
                                            WebStyles.secondary(),
                                            onOpenModpacks);
                                    heliumActionButton(
                                            Core.bundle.get("mod-manager.action.open-folder"),
                                            Icon.save,
                                            WebStyles.secondary(),
                                            this::openModFolder);
                                    heliumActionButton(
                                            Core.bundle.get("mod-manager.action.guide"),
                                            Icon.link,
                                            WebStyles.secondary(),
                                            () -> Core.app.openURI(ModService.MOD_GUIDE_URL));
                                    heliumActionButton(
                                            Core.bundle.get("mod-manager.action.vanilla"),
                                            Icon.menu,
                                            WebStyles.secondary(),
                                            onOpenVanillaMods);
                                });
                            });

                            divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);

                            scroll().grow().scrollX(false).children(() -> {
                                column().growX().top().gap(unit(2)).children(() -> {
                                    text(enabledMods.map(list -> Core.bundle.format(
                                            "mod-manager.section.enabled",
                                            list != null ? list.size() : 0)))
                                            .color(Pal.accent)
                                            .left();
                                    divider().height(3f).color(Pal.accent);
                                    reactiveGrid(enabledMods)
                                            .columns(1)
                                            .key(mod -> mod.name)
                                            .gap(unit(1.5f))
                                            .empty(() -> text(Core.bundle.get("mod-manager.empty.enabled"))
                                                    .color(WebStyles.Colors.GHOST_FG)
                                                    .padding(unit(2)))
                                            .children(mod -> new ModCard(mod, this::bumpTick));

                                    text(disabledMods.map(list -> Core.bundle.format(
                                            "mod-manager.section.disabled",
                                            list != null ? list.size() : 0)))
                                            .color(Pal.accent)
                                            .left();
                                    divider().height(3f).color(Pal.accent);
                                    reactiveGrid(disabledMods)
                                            .columns(1)
                                            .key(mod -> mod.name)
                                            .gap(unit(1.5f))
                                            .empty(() -> text(Core.bundle.get("mod-manager.empty.disabled"))
                                                    .color(WebStyles.Colors.GHOST_FG)
                                                    .padding(unit(2)))
                                            .children(mod -> new ModCard(mod, this::bumpTick));
                                });
                            });
                        });
                    });
        }

        private void landscapeFooter() {
            column().growX().gap(unit(1.5f)).children(() -> {
                row().growX().gap(unit(2)).children(() -> {
                    heliumActionButton(
                            Core.bundle.get("mod-manager.action.browser"),
                            Icon.planet,
                            WebStyles.outline(),
                            onOpenBrowser)
                            .height(unit(12f));
                    heliumActionButton(
                            Core.bundle.get("mod-manager.action.import-file"),
                            Icon.file,
                            WebStyles.outline(),
                            this::importModFile)
                            .height(unit(12f));
                    heliumActionButton(
                            Core.bundle.get("mod-manager.action.import-github"),
                            Icon.download,
                            WebStyles.outline(),
                            this::showImportGithubDialog)
                            .height(unit(12f));
                });

                row().growX().gap(unit(2)).children(() -> {
                    heliumActionButton(
                            Core.bundle.get("back", "Back"),
                            Icon.leftOpen,
                            WebStyles.secondary(),
                            onClose)
                            .height(unit(13.5f));
                    heliumActionButton(
                            Core.bundle.get("mod-manager.action.refresh"),
                            Icon.refresh,
                            WebStyles.secondary(),
                            this::refreshMods)
                            .height(unit(13.5f));
                    heliumActionButton(
                            Core.bundle.get("mod-manager.action.modpack"),
                            Icon.list,
                            WebStyles.secondary(),
                            onOpenModpacks)
                            .height(unit(13.5f));
                    heliumActionButton(
                            Core.bundle.get("mod-manager.action.open-folder"),
                            Icon.save,
                            WebStyles.secondary(),
                            this::openModFolder)
                            .height(unit(13.5f));
                    heliumActionButton(
                            Core.bundle.get("mod-manager.action.guide"),
                            Icon.link,
                            WebStyles.secondary(),
                            () -> Core.app.openURI(ModService.MOD_GUIDE_URL))
                            .height(unit(13.5f));
                });
            });
        }

        private void portraitFooter() {
            row().growX().gap(unit(2)).children(() -> {
                heliumActionButton(
                        Core.bundle.get("back", "Back"),
                        Icon.leftOpen,
                        WebStyles.secondary(),
                        onClose)
                        .height(unit(13f));
                heliumActionButton(
                        Core.bundle.get("mod-manager.action.refresh"),
                        Icon.refresh,
                        WebStyles.secondary(),
                        this::refreshMods)
                        .height(unit(13f));
            });
        }

        private static Button heliumActionButton(
                String label,
                Drawable iconDrawable,
                SolimButtonStyle style,
                Runnable onClick) {
            return button(onClick)
                    .style(style)
                    .growX()
                    .height(unit(11f))
                    .gap(unit(2f))
                    .children(() -> {
                        icon(iconDrawable).size(unit(6f));
                        text(label);
                    });
        }

        private void refreshMods() {
            ModService.getInstance().fetchModListings(true, this::bumpTick);
            ModService.getInstance().checkInstalledModUpdates();
            bumpTick();
        }

        private void bumpTick() {
            Integer v = localTick.peek();
            localTick.set((v != null ? v : 0) + 1);
        }

        private void openModFolder() {
            if (Vars.modDirectory == null) {
                return;
            }
            Core.app.openFolder(Vars.modDirectory.file().getAbsolutePath());
        }

        private void importModFile() {
            FileChooser.open("zip", "jar")
                    .submitMulti(files -> {
                        if (files == null) {
                            return;
                        }
                        for (Fi file : files) {
                            ModService.getInstance().importLocalModFile(file, this::bumpTick);
                        }
                    });
        }

        private void showImportGithubDialog() {
            Signal<String> repoInput = Signal.of("");
            SolimDialog dlg = new SolimDialog(Core.bundle.get("mod-manager.action.import-github"))
                    .addCloseButton()
                    .closeOnBack()
                    .maxWidth(460f);

            dlg.children(() -> {
                column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                    text(Core.bundle.get("mod-manager.github.prompt")).color(WebStyles.Colors.GHOST_FG).wrap();
                    row().growX().gap(unit(1.5f)).children(() -> {
                        row().growX().gap(unit(1.5f))
                                .rounded(unit(2))
                                .paddingLeft(unit(2))
                                .paddingRight(unit(2))
                                .height(unit(11))
                                .center()
                                .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                                .children(() -> textField(repoInput)
                                        .growX()
                                        .style(WebStyles.clearInput())
                                        .placeholder("owner/repo"));
                        button(() -> {
                            String clip = Core.app.getClipboardText();
                            if (clip != null) {
                                repoInput.set(clip.trim());
                            }
                        })
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .children(() -> icon(Icon.paste).size(unit(6)));
                    });

                    button(Core.bundle.get("mod-manager.action.import-github"), () -> {
                        String raw = repoInput.peek();
                        dlg.hide();
                        ModService.getInstance().installFromRepo(raw, false, this::bumpTick);
                    })
                            .style(WebStyles.primaryText())
                            .growX()
                            .height(unit(11));
                });
            });
            dlg.show();
        }

        private static boolean matchesQuery(LoadedMod mod, String query) {
            if (query.isEmpty()) {
                return true;
            }
            if (mod.name != null && mod.name.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
            if (mod.meta != null) {
                if (mod.meta.displayName != null && mod.meta.displayName.toLowerCase(Locale.ROOT).contains(query)) {
                    return true;
                }
                if (mod.meta.author != null && mod.meta.author.toLowerCase(Locale.ROOT).contains(query)) {
                    return true;
                }
            }
            return false;
        }

        private static String displayName(LoadedMod mod) {
            return mod.meta != null && mod.meta.displayName != null
                    ? mod.meta.displayName
                    : (mod.name != null ? mod.name : "");
        }
    }

    private static final class ModSplitSnapshot {
        final List<LoadedMod> enabled;
        final List<LoadedMod> disabled;
        final boolean reloadRequired;

        ModSplitSnapshot(List<LoadedMod> enabled, List<LoadedMod> disabled, boolean reloadRequired) {
            this.enabled = enabled;
            this.disabled = disabled;
            this.reloadRequired = reloadRequired;
        }
    }
}
