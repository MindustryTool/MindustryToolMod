package mindustrytool.features.modmanager;

import static solim.UI.*;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Scaling;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.mod.Mods.LoadedMod;
import mindustry.ui.FileChooser;
import mindustry.ui.Styles;
import mindustrytool.Main;
import mindustrytool.components.WebStyles;
import mindustrytool.features.modmanager.PackModel.FileEntry;
import mindustrytool.features.modmanager.PackModel.ModEntry;
import solim.core.Component;
import solim.layout.Direction;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Signal;
import solim.style.SolimButtonStyle;

public final class ModPackerDialog extends SolimDialog {

    public ModPackerDialog() {
        super(Core.bundle.get("modpack.title"));
        name("heModPackerDialog");
        closeOnBack();
        fillParent(true);
        cont().background(Styles.black8);
        children(() -> new ModPackerView(this::hide));
    }

    private static final class ModPackerView extends Component {
        private final Runnable onClose;

        private final Signal<Boolean> showPackOptions = Signal.of(true);
        private final Signal<Boolean> showExtraFiles = Signal.of(false);
        private final Signal<String> modSearch = Signal.of("");

        private final Signal<String> displayName = Signal.of("My Modpack");
        private final Signal<String> internalName = Signal.of("my-modpack");
        private final Signal<String> version = Signal.of("1.0");
        private final Signal<String> author = Signal.of(
                Vars.player != null && Vars.player.name != null && !Vars.player.name.isEmpty()
                        ? Vars.player.name
                        : "Author");
        private final Signal<String> description = Signal.of("");
        private final Signal<String> iconPath = Signal.of("");
        private final Signal<String> mode = Signal.of(PackModel.MODE_DISTRIBUTE);

        private final Signal<Boolean> skipRepeat = Signal.of(true);
        private final Signal<Boolean> rawBackup = Signal.of(false);
        private final Signal<Boolean> force = Signal.of(false);
        private final Signal<Boolean> uncheck = Signal.of(false);

        private final Signal<List<ModEntry>> selectedMods = Signal.of(new ArrayList<>());
        private final Signal<List<FileEntry>> attachedFiles = Signal.of(new ArrayList<>());

        private final Computed<PackModel> currentModel = new Computed<>(() -> {
            PackModel model = new PackModel();
            model.setDisplayName(displayName.get() != null ? displayName.get() : "");
            model.setName(internalName.get() != null ? internalName.get() : "");
            model.setVersion(version.get() != null ? version.get() : "");
            model.setAuthor(author.get() != null ? author.get() : "");
            model.setDescription(description.get() != null ? description.get() : "");
            model.setIconPath(iconPath.get());
            model.setMode(mode.get() != null ? mode.get() : PackModel.MODE_DISTRIBUTE);
            model.setSkipRepeat(Boolean.TRUE.equals(skipRepeat.get()));
            model.setRawBackup(Boolean.TRUE.equals(rawBackup.get()));
            model.setForce(Boolean.TRUE.equals(force.get()));
            model.setUncheck(Boolean.TRUE.equals(uncheck.get()));
            model.setMods(new ArrayList<>(selectedMods.get()));
            model.setFiles(new ArrayList<>(attachedFiles.get()));
            return model;
        });

        private final Computed<Integer> packStat = currentModel.map(ModpackService::evaluateStat);

        private final Computed<List<ModEntry>> unselectedMods = Signal.computed(() -> {
            ModService.getInstance().installedRevision().get();
            List<ModEntry> sel = selectedMods.get() != null ? selectedMods.get() : new ArrayList<>();
            String q = modSearch.get() != null ? modSearch.get().trim().toLowerCase(Locale.ROOT) : "";
            return getUnselectedInstalledMods(sel, q);
        });

        private final Computed<List<ModEntry>> filteredSelectedMods = Signal.computed(() -> {
            List<ModEntry> sel = selectedMods.get() != null ? selectedMods.get() : new ArrayList<>();
            String q = modSearch.get() != null ? modSearch.get().trim().toLowerCase(Locale.ROOT) : "";
            List<ModEntry> filteredSel = new ArrayList<>();
            for (ModEntry entry : sel) {
                if (q.isEmpty()
                        || entry.getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                        || entry.getName().toLowerCase(Locale.ROOT).contains(q)) {
                    filteredSel.add(entry);
                }
            }
            return filteredSel;
        });

        ModPackerView(Runnable onClose) {
            this.onClose = onClose;
        }

        @Override
        protected Element build() {
            return column().grow().gap(unit(1.5f)).padding(unit(2)).children(() -> {
                topSearchAndDrawerBar();
                divider().height(3f).color(Pal.accent);
                validationBanner();
                mainSplitArea();
                divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);
                bottomActionBar();
            }).element();
        }

        private void topSearchAndDrawerBar() {
            row().growX().gap(unit(2f)).center().children(() -> {
                button(() -> showPackOptions.set(!Boolean.TRUE.equals(showPackOptions.peek())))
                        .style(WebStyles.filterChip())
                        .checked(showPackOptions)
                        .height(unit(11f))
                        .paddingLeft(unit(2.5f))
                        .paddingRight(unit(2.5f))
                        .gap(unit(1.5f))
                        .children(() -> {
                            icon(Icon.menu).size(unit(5f));
                            text(Core.bundle.get("modpack.tab.info"));
                        });

                row().growX().gap(unit(2))
                        .rounded(unit(3))
                        .paddingLeft(unit(2.5f))
                        .paddingRight(unit(2.5f))
                        .height(unit(11.5f))
                        .center()
                        .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                        .children(() -> {
                            icon(Icon.zoom).size(unit(6f)).color(WebStyles.Colors.GHOST_FG);
                            textField(modSearch)
                                    .growX()
                                    .style(WebStyles.clearInput())
                                    .placeholder(Core.bundle.get("mod-manager.search.placeholder"));
                            button(() -> modSearch.set(""))
                                    .style(WebStyles.ghost())
                                    .size(unit(9f))
                                    .visible(modSearch.map(q -> q != null && !q.isEmpty()))
                                    .children(() -> icon(Icon.cancel).size(unit(5f)));
                        });

                button(() -> showExtraFiles.set(!Boolean.TRUE.equals(showExtraFiles.peek())))
                        .style(WebStyles.filterChip())
                        .checked(showExtraFiles)
                        .height(unit(11f))
                        .paddingLeft(unit(2.5f))
                        .paddingRight(unit(2.5f))
                        .gap(unit(1.5f))
                        .children(() -> {
                            icon(Icon.file).size(unit(5f));
                            text(Core.bundle.get("modpack.tab.files"));
                        });
            });
        }

        private void validationBanner() {
            dynamic(packStat, statVal -> {
                int stat = statVal != null ? statVal : 0;
                boolean valid = PackModel.isStatValid(stat) || Boolean.TRUE.equals(uncheck.peek());
                boolean warning = PackModel.hasStatWarning(stat);
                Color bannerColor = !valid ? Color.scarlet : (warning ? Color.orange : Pal.heal);

                card()
                        .growX()
                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                        .border(1.5f, bannerColor)
                        .padding(unit(1.2f), unit(2f), unit(1.2f), unit(2f))
                        .children(() -> {
                            wrap().growX().gap(unit(1.5f)).left().children(() -> {
                                icon(valid ? Icon.ok : Icon.warning).color(bannerColor).size(unit(4.5f));
                                text(Core.bundle.get(valid ? "modpack.stat.ready" : "modpack.stat.invalid"))
                                        .color(bannerColor)
                                        .fontScale(0.9f);

                                if (PackModel.hasNonModSelect(stat)) {
                                    ModStatusBadges.statusChip(
                                            Core.bundle.get("modpack.stat.no-mods"), Color.scarlet, null);
                                }
                                if (PackModel.hasMissingInfo(stat)) {
                                    ModStatusBadges.statusChip(
                                            Core.bundle.get("modpack.stat.missing-info"), Color.scarlet, null);
                                }
                                if (PackModel.hasIconNonSquare(stat)) {
                                    ModStatusBadges.statusChip(
                                            Core.bundle.get("modpack.stat.icon-non-square"), Color.scarlet, null);
                                }
                                if (PackModel.hasFileConflict(stat)) {
                                    ModStatusBadges.statusChip(
                                            Core.bundle.get("modpack.stat.file-conflict"), Color.scarlet, null);
                                }
                                if (PackModel.hasDependMissing(stat)) {
                                    ModStatusBadges.statusChip(
                                            Core.bundle.get("modpack.stat.depend-missing"), Color.orange, null);
                                }
                                if (PackModel.hasModInvalid(stat)) {
                                    ModStatusBadges.statusChip(
                                            Core.bundle.get("modpack.stat.mod-invalid"), Color.orange, null);
                                }
                            });
                        });
            }).growX();
        }

        private void mainSplitArea() {
            row().grow().gap(unit(2f)).children(() -> {
                dynamic(showPackOptions, showOpts -> {
                    if (Boolean.TRUE.equals(showOpts)) {
                        row().growY().gap(unit(1.5f)).children(() -> {
                            packOptionsPanel();
                            divider(Direction.Y).width(2f).color(WebStyles.Colors.SECTION_BORDER).growY();
                        });
                    }
                }).growY();

                modsSplitColumns();

                dynamic(showExtraFiles, showFiles -> {
                    if (Boolean.TRUE.equals(showFiles)) {
                        row().growY().gap(unit(1.5f)).children(() -> {
                            divider(Direction.Y).width(2f).color(WebStyles.Colors.SECTION_BORDER).growY();
                            extraFilesPanel();
                        });
                    }
                }).growY();
            });
        }

        private void packOptionsPanel() {
            card()
                    .width(unit(92f))
                    .growY()
                    .padding(unit(2f))
                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                    .border(1f, WebStyles.Colors.SECTION_BORDER)
                    .children(() -> {
                        column().grow().gap(unit(1.5f)).children(() -> {
                            row().growX().gap(unit(1.5f)).left().children(() -> {
                                icon(Icon.settings).size(unit(5f)).color(Pal.accent);
                                text(Core.bundle.get("modpack.tab.info")).color(Pal.accent);
                            });
                            divider().height(3f).color(Pal.accent);

                            scroll().grow().scrollX(false).children(() -> {
                                column().growX().gap(unit(2f)).paddingRight(unit(1f)).children(() -> {
                                    row().growX().gap(unit(2f)).left().children(() -> {
                                        card()
                                                .size(unit(22f))
                                                .padding(unit(1.5f))
                                                .center()
                                                .rounded(unit(2), WebStyles.Colors.SECONDARY)
                                                .border(1f, WebStyles.Colors.BORDER_INPUT)
                                                .onClick(this::promptChoosePackIcon)
                                                .children(() -> icon(Icon.book).size(unit(13f)).color(Pal.accent));

                                        column().growX().gap(unit(1f)).left().children(() -> {
                                            text(Core.bundle.get("modpack.field.icon"))
                                                    .color(WebStyles.Colors.GHOST_FG)
                                                    .fontScale(0.85f);
                                            styledInput(iconPath, "icon.png");
                                            button(this::promptChoosePackIcon)
                                                    .style(WebStyles.secondary())
                                                    .growX()
                                                    .height(unit(9f))
                                                    .gap(unit(1f))
                                                    .children(() -> {
                                                        icon(Icon.folder).size(unit(4.5f));
                                                        text(Core.bundle.get("mod-manager.action.import-file"))
                                                                .fontScale(0.82f);
                                                    });
                                        });
                                    });

                                    stackedField(Core.bundle.get("modpack.field.display-name"), displayName);
                                    stackedField(Core.bundle.get("modpack.field.internal-name"), internalName);
                                    stackedField(Core.bundle.get("modpack.field.description"), description);

                                    row().growX().gap(unit(1.5f)).children(() -> {
                                        column().growX().gap(unit(0.5f)).left().children(() -> {
                                            text(Core.bundle.get("modpack.field.author"))
                                                    .color(WebStyles.Colors.GHOST_FG)
                                                    .fontScale(0.85f);
                                            styledInput(author, "");
                                        });
                                        column().growX().gap(unit(0.5f)).left().children(() -> {
                                            text(Core.bundle.get("modpack.field.version"))
                                                    .color(WebStyles.Colors.GHOST_FG)
                                                    .fontScale(0.85f);
                                            styledInput(version, "");
                                        });
                                    });

                                    divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);

                                    text(Core.bundle.get("modpack.mode.title")).color(Pal.accent);
                                    row().growX().gap(unit(1f)).children(() -> {
                                        modeToggleBtn(PackModel.MODE_DISTRIBUTE, "modpack.mode.distribute");
                                        modeToggleBtn(PackModel.MODE_SHADOW, "modpack.mode.shadow");
                                        modeToggleBtn(PackModel.MODE_OVERRIDE, "modpack.mode.override");
                                    });

                                    card()
                                            .growX()
                                            .padding(unit(1.5f))
                                            .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                                            .border(1f, WebStyles.Colors.SECTION_BORDER)
                                            .children(() -> {
                                                dynamic(mode, curMode -> {
                                                    String descKey = PackModel.MODE_SHADOW.equalsIgnoreCase(curMode)
                                                            ? "modpack.mode.shadow.desc"
                                                            : (PackModel.MODE_OVERRIDE.equalsIgnoreCase(curMode)
                                                                    ? "modpack.mode.override.desc"
                                                                    : "modpack.mode.distribute.desc");
                                                    text(Core.bundle.get(descKey))
                                                            .color(WebStyles.Colors.GHOST_FG)
                                                            .fontScale(0.82f)
                                                            .growX()
                                                            .wrap();
                                                }).growX();
                                            });

                                    divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);

                                    text(Core.bundle.get("modpack.options.title")).color(Pal.accent);
                                    checkbox(Core.bundle.get("modpack.option.skip-repeat"), skipRepeat).wrap();
                                    checkbox(Core.bundle.get("modpack.option.raw-backup"), rawBackup).wrap();
                                    checkbox(Core.bundle.get("modpack.option.force"), force).wrap();
                                    checkbox(Core.bundle.get("modpack.option.uncheck"), uncheck).wrap();
                                });
                            });
                        });
                    });
        }

        private void modeToggleBtn(String modeValue, String labelKey) {
            Computed<Boolean> isCurrent = mode.map(m -> modeValue.equalsIgnoreCase(m));
            button(Core.bundle.get(labelKey), () -> mode.set(modeValue))
                    .style(WebStyles.filterChipText())
                    .checked(isCurrent)
                    .growX()
                    .height(unit(9.5f));
        }

        private void stackedField(String label, Signal<String> signal) {
            column().growX().gap(unit(0.5f)).left().children(() -> {
                text(label).color(WebStyles.Colors.GHOST_FG).fontScale(0.85f);
                styledInput(signal, "");
            });
        }

        private static void styledInput(Signal<String> signal, String placeholderText) {
            row().growX().gap(unit(1f))
                    .rounded(unit(2))
                    .paddingLeft(unit(2f))
                    .paddingRight(unit(2f))
                    .height(unit(10f))
                    .center()
                    .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                    .children(() -> textField(signal)
                            .growX()
                            .style(WebStyles.clearInput())
                            .placeholder(placeholderText));
        }

        private void promptChoosePackIcon() {
            FileChooser.open("png").submit(file -> {
                if (file != null) {
                    iconPath.set(file.file().getAbsolutePath());
                }
            });
        }

        private void modsSplitColumns() {
            row().grow().gap(unit(2f)).children(() -> {
                card()
                        .grow()
                        .padding(unit(2f))
                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                        .border(1f, WebStyles.Colors.SECTION_BORDER)
                        .children(() -> {
                            column().grow().gap(unit(1.5f)).children(() -> {
                                row().growX().gap(unit(1.5f)).left().children(() -> {
                                    icon(Icon.none).size(unit(5f)).color(Pal.accent);
                                    text(unselectedMods.map(list -> Core.bundle.format(
                                            "modpack.mods.available-header",
                                            list != null ? list.size() : 0)))
                                            .color(Pal.accent)
                                            .growX();
                                    button(this::addAllInstalledMods)
                                            .style(WebStyles.secondary())
                                            .height(unit(9f))
                                            .paddingLeft(unit(2f))
                                            .paddingRight(unit(2f))
                                            .gap(unit(1f))
                                            .children(() -> {
                                                icon(Icon.rightOpen).size(unit(4.5f));
                                                text(Core.bundle.get("modpack.mods.add-all")).fontScale(0.82f);
                                            });
                                });

                                divider().height(3f).color(Pal.accent);

                                scroll().grow().scrollX(false).children(() -> {
                                    column().growX().top().paddingRight(unit(1f)).children(() -> {
                                        reactiveGrid(unselectedMods)
                                                .columns(1)
                                                .key(ModEntry::getName)
                                                .gap(unit(1.5f))
                                                .empty(() -> text(Core.bundle.get("modpack.mods.available-empty"))
                                                        .color(WebStyles.Colors.GHOST_FG)
                                                        .padding(unit(4f)))
                                                .children(entry -> new PackModItemCard(
                                                        entry,
                                                        false,
                                                        () -> addSelectedMod(entry)));
                                    });
                                });
                            });
                        });

                divider(Direction.Y).width(2f).color(WebStyles.Colors.SECTION_BORDER).growY();

                card()
                        .grow()
                        .padding(unit(2f))
                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                        .border(1f, WebStyles.Colors.SECTION_BORDER)
                        .children(() -> {
                            column().grow().gap(unit(1.5f)).children(() -> {
                                row().growX().gap(unit(1.5f)).left().children(() -> {
                                    icon(Icon.ok).size(unit(5f)).color(Pal.accent);
                                    text(filteredSelectedMods.map(list -> Core.bundle.format(
                                            "modpack.mods.selected-header",
                                            list != null ? list.size() : 0)))
                                            .color(Pal.accent)
                                            .growX();
                                    button(() -> selectedMods.set(new ArrayList<>()))
                                            .style(WebStyles.secondary())
                                            .height(unit(9f))
                                            .paddingLeft(unit(2f))
                                            .paddingRight(unit(2f))
                                            .gap(unit(1f))
                                            .children(() -> {
                                                icon(Icon.leftOpen).size(unit(4.5f));
                                                text(Core.bundle.get("modpack.mods.clear-all")).fontScale(0.82f);
                                            });
                                });

                                divider().height(3f).color(Pal.accent);

                                scroll().grow().scrollX(false).children(() -> {
                                    column().growX().top().paddingRight(unit(1f)).children(() -> {
                                        reactiveGrid(filteredSelectedMods)
                                                .columns(1)
                                                .key(ModEntry::getName)
                                                .gap(unit(1.5f))
                                                .empty(() -> text(Core.bundle.get("modpack.mods.selected-empty"))
                                                        .color(WebStyles.Colors.GHOST_FG)
                                                        .padding(unit(4f)))
                                                .children(entry -> new PackModItemCard(
                                                        entry,
                                                        true,
                                                        () -> removeSelectedMod(entry.getName())));
                                    });
                                });
                            });
                        });
            });
        }

        private static final class PackModItemCard extends Component {
            private final ModEntry entry;
            private final boolean isSelected;
            private final Runnable onTransfer;

            PackModItemCard(ModEntry entry, boolean isSelected, Runnable onTransfer) {
                this.entry = entry;
                this.isSelected = isSelected;
                this.onTransfer = onTransfer;
            }

            @Override
            protected Element build() {
                Drawable modIcon = resolveLoadedModIcon(entry.getName());
                String title = entry.getDisplayName() != null && !entry.getDisplayName().isEmpty()
                        ? entry.getDisplayName()
                        : entry.getName();

                return card()
                        .growX()
                        .padding(unit(1.5f))
                        .rounded(unit(2), WebStyles.Colors.SECONDARY)
                        .border(1f, WebStyles.Colors.BORDER_INPUT)
                        .children(() -> {
                            row().growX().gap(unit(2f)).top().children(() -> {
                                if (isSelected) {
                                    button(onTransfer)
                                            .style(WebStyles.ghost())
                                            .size(unit(11f))
                                            .tooltip(Core.bundle.get("modpack.mods.remove"))
                                            .children(() -> icon(Icon.leftOpen).size(unit(6f)));

                                    divider(Direction.Y).width(2f).color(WebStyles.Colors.SECTION_BORDER).growY();
                                }

                                card()
                                        .size(unit(19f))
                                        .padding(unit(1f))
                                        .center()
                                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                                        .border(1f, WebStyles.Colors.SECTION_BORDER)
                                        .children(() -> {
                                            if (modIcon != null) {
                                                image(modIcon)
                                                        .size(unit(17f))
                                                        .scaling(Scaling.fit);
                                            } else {
                                                icon(Icon.book)
                                                        .size(unit(11f))
                                                        .color(Pal.accent);
                                            }
                                        });

                                column().growX().top().left().gap(unit(0.6f)).children(() -> {
                                    row().growX().top().gap(unit(1.5f)).children(() -> {
                                        text(title)
                                                .color(Pal.accent)
                                                .wrap()
                                                .growX()
                                                .left();
                                        ModStatusBadges.renderCornerIcons(entry.getStat(), false, null, false);
                                    });

                                    if (!entry.getVersion().isEmpty()) {
                                        text("v" + entry.getVersion())
                                                .color(WebStyles.Colors.GHOST_FG)
                                                .fontScale(0.82f)
                                                .left();
                                    }

                                    if (entry.getShortDescription() != null && !entry.getShortDescription().isEmpty()) {
                                        text(entry.getShortDescription())
                                                .color(WebStyles.Colors.SECONDARY_FG)
                                                .fontScale(0.85f)
                                                .growX()
                                                .wrap()
                                                .left();
                                    }

                                    text(Core.bundle.format("mod-manager.by-author", entry.getAuthor()))
                                            .color(WebStyles.Colors.GHOST_FG)
                                            .fontScale(0.8f)
                                            .left();
                                });

                                if (!isSelected) {
                                    divider(Direction.Y).width(2f).color(WebStyles.Colors.SECTION_BORDER).growY();

                                    button(onTransfer)
                                            .style(WebStyles.ghost())
                                            .size(unit(11f))
                                            .tooltip(Core.bundle.get("modpack.mods.add"))
                                            .children(() -> icon(Icon.rightOpen).size(unit(6f)));
                                }
                            });
                        }).element();
            }
        }

        private static @Nullable Drawable resolveLoadedModIcon(String internalName) {
            if (Vars.mods == null || internalName == null) {
                return null;
            }
            LoadedMod mod = Vars.mods.getMod(internalName.toLowerCase(Locale.ROOT));
            return mod != null && mod.iconTexture != null
                    ? new TextureRegionDrawable(new TextureRegion(mod.iconTexture))
                    : null;
        }

        private void extraFilesPanel() {
            card()
                    .width(unit(88f))
                    .growY()
                    .padding(unit(2f))
                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                    .border(1f, WebStyles.Colors.SECTION_BORDER)
                    .children(() -> {
                        column().grow().gap(unit(1.5f)).children(() -> {
                            row().growX().gap(unit(1.5f)).left().children(() -> {
                                icon(Icon.file).size(unit(5f)).color(Pal.accent);
                                text(Core.bundle.get("modpack.tab.files")).color(Pal.accent);
                            });
                            divider().height(3f).color(Pal.accent);

                            column().growX().gap(unit(1f)).children(() -> {
                                extraFileActionBtn(
                                        Core.bundle.get("modpack.files.add-saves"),
                                        Icon.save,
                                        this::attachAllSaves);
                                extraFileActionBtn(
                                        Core.bundle.get("modpack.files.add-schematics"),
                                        Icon.paste,
                                        this::attachAllSchematics);
                                extraFileActionBtn(
                                        Core.bundle.get("modpack.files.add-settings"),
                                        Icon.settings,
                                        this::attachSettingsBin);
                                extraFileActionBtn(
                                        Core.bundle.get("modpack.files.add-custom"),
                                        Icon.add,
                                        this::promptAttachCustomFile);
                                extraFileActionBtn(
                                        Core.bundle.get("modpack.mods.clear-all"),
                                        Icon.trash,
                                        () -> attachedFiles.set(new ArrayList<>()));
                            });

                            divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);

                            scroll().grow().scrollX(false).children(() -> {
                                column().growX().top().children(() -> {
                                    reactiveGrid(attachedFiles)
                                            .columns(1)
                                            .key(FileEntry::getTargetPath)
                                            .gap(unit(1f))
                                            .empty(() -> text(Core.bundle.get("modpack.files.empty"))
                                                    .color(WebStyles.Colors.GHOST_FG)
                                                    .padding(unit(2f)))
                                            .children(entry -> new AttachedFileCard(
                                                    entry,
                                                    () -> removeAttachedFile(entry.getTargetPath())));
                                });
                            });
                        });
                    });
        }

        private static final class AttachedFileCard extends Component {
            private final FileEntry entry;
            private final Runnable onRemove;

            AttachedFileCard(FileEntry entry, Runnable onRemove) {
                this.entry = entry;
                this.onRemove = onRemove;
            }

            @Override
            protected Element build() {
                return card()
                        .growX()
                        .padding(unit(1.5f))
                        .rounded(unit(2), WebStyles.Colors.SECONDARY)
                        .border(1f, WebStyles.Colors.BORDER_INPUT)
                        .children(() -> {
                            row().growX().gap(unit(1.5f)).left().children(() -> {
                                ModStatusBadges.statusChip(entry.getCategory(), Pal.accent, null);
                                text(entry.getTargetPath())
                                        .color(WebStyles.Colors.SECONDARY_FG)
                                        .fontScale(0.85f)
                                        .growX()
                                        .wrap();
                                button(onRemove)
                                        .style(WebStyles.ghost())
                                        .size(unit(9f))
                                        .children(() -> icon(Icon.trash)
                                                .size(unit(5f))
                                                .color(WebStyles.Colors.DANGER));
                            });
                        }).element();
            }
        }

        private static void extraFileActionBtn(String label, Drawable iconDrawable, Runnable onClick) {
            button(onClick)
                    .style(WebStyles.secondary())
                    .growX()
                    .height(unit(9.5f))
                    .gap(unit(1.5f))
                    .children(() -> {
                        icon(iconDrawable).size(unit(4.5f));
                        text(label).fontScale(0.85f);
                    });
        }

        private void bottomActionBar() {
            wrap().growX().gap(unit(1.5f)).center().children(() -> {
                heliumBottomButton(
                        Core.bundle.get("back", "Back"),
                        Icon.left,
                        WebStyles.secondary(),
                        onClose);

                heliumBottomButton(
                        Core.bundle.get("modpack.action.new"),
                        Icon.add,
                        WebStyles.secondary(),
                        this::resetToNewPack);

                heliumBottomButton(
                        Core.bundle.get("modpack.action.open"),
                        Icon.folder,
                        WebStyles.secondary(),
                        this::promptOpenModpackToEdit);

                heliumBottomButton(
                        Core.bundle.get("modpack.action.install"),
                        Icon.download,
                        WebStyles.outline(),
                        this::promptInstallModpack);

                heliumBottomButton(
                        Core.bundle.get("modpack.action.backups"),
                        Icon.refresh,
                        WebStyles.secondary(),
                        this::showBackupsDialog);

                heliumBottomButton(
                        Core.bundle.get("modpack.action.export"),
                        Icon.export,
                        WebStyles.primary(),
                        this::promptExportModpack);
            });
        }

        private static void heliumBottomButton(
                String label,
                Drawable iconDrawable,
                SolimButtonStyle style,
                Runnable onClick) {
            button(onClick)
                    .style(style)
                    .width(unit(48f))
                    .height(unit(13f))
                    .gap(unit(1.5f))
                    .children(() -> {
                        icon(iconDrawable).size(unit(6f));
                        text(label);
                    });
        }

        private void resetToNewPack() {
            displayName.set("My Modpack");
            internalName.set("my-modpack");
            version.set("1.0");
            description.set("");
            iconPath.set("");
            mode.set(PackModel.MODE_DISTRIBUTE);
            skipRepeat.set(true);
            rawBackup.set(false);
            force.set(false);
            uncheck.set(false);
            selectedMods.set(new ArrayList<>());
            attachedFiles.set(new ArrayList<>());
        }

        private List<ModEntry> getUnselectedInstalledMods(List<ModEntry> selected, String query) {
            Set<String> selectedNames = new HashSet<>();
            for (ModEntry s : selected) {
                selectedNames.add(s.getName().toLowerCase(Locale.ROOT));
            }
            List<ModEntry> result = new ArrayList<>();
            if (Vars.mods == null) {
                return result;
            }
            Seq<LoadedMod> installed = Vars.mods.list();
            if (installed == null) {
                return result;
            }
            for (LoadedMod mod : installed) {
                if (mod == null || mod == Main.self || mod.file == null || !mod.file.exists()) {
                    continue;
                }
                if (selectedNames.contains(mod.name.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                ModEntry entry = toModEntry(mod);
                if (!query.isEmpty()
                        && !entry.getDisplayName().toLowerCase(Locale.ROOT).contains(query)
                        && !entry.getName().toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }
                result.add(entry);
            }
            return result;
        }

        private static ModEntry toModEntry(LoadedMod mod) {
            List<String> deps = new ArrayList<>();
            if (mod.meta != null && mod.meta.dependencies != null) {
                for (String dep : mod.meta.dependencies) {
                    if (dep != null) {
                        deps.add(dep);
                    }
                }
            }
            return new ModEntry(
                    mod.name,
                    mod.meta != null && mod.meta.displayName != null ? mod.meta.displayName : mod.name,
                    mod.meta != null && mod.meta.version != null ? mod.meta.version : "1.0",
                    mod.meta != null && mod.meta.author != null ? mod.meta.author : "",
                    mod.file != null ? mod.file.name() : (mod.name + ".zip"),
                    mod.file != null ? mod.file.file().getAbsolutePath() : "",
                    mod.meta != null && mod.meta.shortDescription() != null ? mod.meta.shortDescription() : "",
                    deps,
                    ModStat.checkModStat(mod, false));
        }

        private void addAllInstalledMods() {
            if (Vars.mods == null) {
                return;
            }
            List<ModEntry> next = new ArrayList<>(selectedMods.peek());
            Set<String> existing = new HashSet<>();
            for (ModEntry e : next) {
                existing.add(e.getName().toLowerCase(Locale.ROOT));
            }
            for (LoadedMod mod : Vars.mods.list()) {
                if (mod == null || mod == Main.self || mod.file == null || !mod.file.exists()) {
                    continue;
                }
                if (existing.add(mod.name.toLowerCase(Locale.ROOT))) {
                    next.add(toModEntry(mod));
                }
            }
            selectedMods.set(next);
        }

        private void addSelectedMod(ModEntry entry) {
            List<ModEntry> next = new ArrayList<>(selectedMods.peek());
            for (ModEntry e : next) {
                if (e.getName().equalsIgnoreCase(entry.getName())) {
                    return;
                }
            }
            next.add(entry);
            selectedMods.set(next);
        }

        private void removeSelectedMod(String name) {
            List<ModEntry> next = new ArrayList<>(selectedMods.peek());
            next.removeIf(e -> e.getName().equalsIgnoreCase(name));
            selectedMods.set(next);
        }

        private void attachAllSaves() {
            if (Vars.saveDirectory != null && Vars.saveDirectory.exists()) {
                addFileEntry(new FileEntry("saves", Vars.saveDirectory.file().getAbsolutePath(), "saves"));
            }
        }

        private void attachAllSchematics() {
            if (Vars.schematicDirectory != null && Vars.schematicDirectory.exists()) {
                addFileEntry(new FileEntry(
                        "schematics",
                        Vars.schematicDirectory.file().getAbsolutePath(),
                        "schematics"));
            }
        }

        private void attachSettingsBin() {
            Fi settingsFi = Vars.dataDirectory.child("settings.bin");
            if (settingsFi.exists()) {
                addFileEntry(new FileEntry("settings.bin", settingsFi.file().getAbsolutePath(), "settings"));
            }
        }

        private void promptAttachCustomFile() {
            FileChooser.open("msav", "msch", "bin", "json", "txt", "zip", "png").submit(file -> {
                if (file != null) {
                    addFileEntry(new FileEntry("custom/" + file.name(), file.file().getAbsolutePath(), "custom"));
                }
            });
        }

        private void addFileEntry(FileEntry entry) {
            List<FileEntry> next = new ArrayList<>(attachedFiles.peek());
            next.removeIf(f -> f.getTargetPath().equalsIgnoreCase(entry.getTargetPath()));
            next.add(entry);
            attachedFiles.set(next);
        }

        private void removeAttachedFile(String targetPath) {
            List<FileEntry> next = new ArrayList<>(attachedFiles.peek());
            next.removeIf(f -> f.getTargetPath().equalsIgnoreCase(targetPath));
            attachedFiles.set(next);
        }

        private void promptOpenModpackToEdit() {
            FileChooser.open("zip").submit(file -> {
                if (file == null) {
                    return;
                }
                try {
                    Fi extractDir = Vars.dataDirectory.child("tmp").child("modpack_edit");
                    PackModel loaded = ModpackService.readModpack(file, extractDir);
                    displayName.set(loaded.getDisplayName());
                    internalName.set(loaded.getName());
                    version.set(loaded.getVersion());
                    author.set(loaded.getAuthor());
                    description.set(loaded.getDescription());
                    iconPath.set(loaded.getIconPath() != null ? loaded.getIconPath() : "");
                    mode.set(loaded.getMode());
                    skipRepeat.set(loaded.isSkipRepeat());
                    rawBackup.set(loaded.isRawBackup());
                    force.set(loaded.isForce());
                    uncheck.set(loaded.isUncheck());
                    selectedMods.set(loaded.getMods());
                    attachedFiles.set(loaded.getFiles());
                } catch (Exception e) {
                    if (Vars.ui != null) {
                        Vars.ui.showException(e);
                    }
                }
            });
        }

        private void promptExportModpack() {
            PackModel model = currentModel.peek();
            int stat = ModpackService.evaluateStat(model);
            if (!PackModel.isStatValid(stat) && !model.isUncheck()) {
                if (Vars.ui != null) {
                    Vars.ui.showErrorMessage(Core.bundle.get("modpack.export.invalid"));
                }
                return;
            }
            String fileName = (model.getName().isEmpty() ? "modpack" : model.getName()) + ".zip";
            FileChooser.save("zip")
                    .name(fileName)
                    .submit(dest -> {
                        if (dest == null) {
                            return;
                        }
                        try {
                            ModpackService.exportModpack(model, dest);
                            if (Vars.ui != null) {
                                Vars.ui.showInfoFade(Core.bundle.format("mod-manager.export.success", dest.name()));
                            }
                        } catch (Exception e) {
                            if (Vars.ui != null) {
                                Vars.ui.showException(e);
                            }
                        }
                    });
        }

        private void promptInstallModpack() {
            FileChooser.open("zip").submit(file -> {
                if (file != null) {
                    showInstallModpackDialog(file);
                }
            });
        }

        private void showInstallModpackDialog(Fi archiveFile) {
            Fi previewDir = Vars.dataDirectory.child("tmp").child("modpack_preview");
            final PackModel preview;
            try {
                preview = ModpackService.readModpack(archiveFile, previewDir);
            } catch (Exception e) {
                if (Vars.ui != null) {
                    Vars.ui.showException(e);
                }
                return;
            }

            Signal<String> chosenMode = Signal.of(preview.getMode());
            Signal<Boolean> backupBeforeInstall = Signal.of(true);

            SolimDialog dlg = new SolimDialog(Core.bundle.get("modpack.install.title"))
                    .addCloseButton()
                    .closeOnBack()
                    .maxWidth(480f);

            dlg.hidden(previewDir::deleteDirectory);
            dlg.children(() -> {
                column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                    text(preview.getDisplayName() + " v" + preview.getVersion()).color(Pal.accent);
                    text(Core.bundle.format("mod-manager.by-author", preview.getAuthor()))
                            .color(WebStyles.Colors.GHOST_FG);
                    if (!preview.getDescription().isEmpty()) {
                        text(preview.getDescription()).color(WebStyles.Colors.GHOST_FG).wrap();
                    }

                    text(Core.bundle.format(
                            "modpack.install.summary",
                            preview.getMods().size(),
                            preview.getFiles().size())).color(WebStyles.Colors.SECONDARY_FG);

                    if (!preview.isForce()) {
                        row().growX().gap(unit(1)).children(() -> {
                            for (String m : new String[] {
                                    PackModel.MODE_DISTRIBUTE,
                                    PackModel.MODE_SHADOW,
                                    PackModel.MODE_OVERRIDE }) {
                                button(m, () -> chosenMode.set(m))
                                        .style(WebStyles.filterChipText())
                                        .checked(chosenMode.map(cur -> m.equalsIgnoreCase(cur)))
                                        .growX()
                                        .height(unit(9.5f));
                            }
                        });
                    }

                    checkbox(Core.bundle.get("modpack.install.create-backup"), backupBeforeInstall).wrap();

                    button(Core.bundle.get("modpack.action.install"), () -> {
                        try {
                            ModpackService.installModpack(
                                    archiveFile,
                                    Vars.dataDirectory,
                                    chosenMode.peek(),
                                    Boolean.TRUE.equals(backupBeforeInstall.peek()));
                            dlg.hide();
                            ModService.getInstance().notifyInstalledModsChanged();
                            if (Vars.ui != null) {
                                Vars.ui.showConfirm(
                                        Core.bundle.get("modpack.install.success.title"),
                                        Core.bundle.get("modpack.install.success.desc"),
                                        () -> Core.app.exit());
                            }
                        } catch (Exception e) {
                            if (Vars.ui != null) {
                                Vars.ui.showException(e);
                            }
                        }
                    }).style(WebStyles.primaryText()).growX().height(unit(11));
                });
            });
            dlg.show();
        }

        private void showBackupsDialog() {
            Signal<List<Fi>> backupsSignal = Signal.of(ModpackService.listBackups(Vars.dataDirectory));
            SolimDialog dlg = new SolimDialog(Core.bundle.get("modpack.backups.title"))
                    .addCloseButton()
                    .closeOnBack()
                    .maxWidth(500f);

            dlg.children(() -> {
                column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                    button(() -> {
                        try {
                            Fi created = ModpackService.createBackupArchive(Vars.dataDirectory, "manual");
                            backupsSignal.set(ModpackService.listBackups(Vars.dataDirectory));
                            if (Vars.ui != null) {
                                Vars.ui.showInfoFade(Core.bundle.format("modpack.backups.created", created.name()));
                            }
                        } catch (Exception e) {
                            if (Vars.ui != null) {
                                Vars.ui.showException(e);
                            }
                        }
                    }).style(WebStyles.primary()).growX().height(unit(11f)).gap(unit(1.5f)).children(() -> {
                        icon(Icon.save).size(unit(5.5f));
                        text(Core.bundle.get("modpack.backups.create-now"));
                    });

                    scroll().grow().scrollX(false).children(() -> {
                        column().growX().top().children(() -> {
                            reactiveGrid(backupsSignal)
                                    .columns(1)
                                    .key(Fi::name)
                                    .gap(unit(1.5f))
                                    .empty(() -> text(Core.bundle.get("modpack.backups.empty"))
                                            .color(WebStyles.Colors.GHOST_FG)
                                            .padding(unit(2)))
                                    .children(backup -> new BackupArchiveCard(
                                            backup,
                                            () -> {
                                                try {
                                                    ModpackService.restoreBackupArchive(backup, Vars.dataDirectory);
                                                    dlg.hide();
                                                    if (Vars.ui != null) {
                                                        Vars.ui.showConfirm(
                                                                Core.bundle.get("modpack.backups.restored.title"),
                                                                Core.bundle.get("modpack.backups.restored.desc"),
                                                                () -> Core.app.exit());
                                                    }
                                                } catch (Exception e) {
                                                    if (Vars.ui != null) {
                                                        Vars.ui.showException(e);
                                                    }
                                                }
                                            },
                                            () -> {
                                                backup.delete();
                                                backupsSignal.set(ModpackService.listBackups(Vars.dataDirectory));
                                            }));
                        });
                    });
                });
            });
            dlg.show();
        }

        private static final class BackupArchiveCard extends Component {
            private final Fi backup;
            private final Runnable onRestore;
            private final Runnable onDelete;

            BackupArchiveCard(Fi backup, Runnable onRestore, Runnable onDelete) {
                this.backup = backup;
                this.onRestore = onRestore;
                this.onDelete = onDelete;
            }

            @Override
            protected Element build() {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
                return card()
                        .growX()
                        .padding(unit(1.5f))
                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                        .border(1f, WebStyles.Colors.SECTION_BORDER)
                        .children(() -> {
                            row().growX().gap(unit(1.5f)).left().children(() -> {
                                column().growX().left().children(() -> {
                                    text(backup.name()).color(WebStyles.Colors.SECONDARY_FG).wrap();
                                    text(fmt.format(new Date(backup.lastModified())))
                                            .color(WebStyles.Colors.GHOST_FG)
                                            .fontScale(0.8f);
                                });

                                button(onRestore)
                                        .style(WebStyles.secondaryText())
                                        .height(unit(9.5f))
                                        .text(Core.bundle.get("modpack.backups.restore"));

                                button(onDelete)
                                        .style(WebStyles.ghost())
                                        .size(unit(9.5f))
                                        .children(() -> icon(Icon.trash)
                                                .size(unit(5f))
                                                .color(WebStyles.Colors.DANGER));
                            });
                        }).element();
            }
        }
    }
}
