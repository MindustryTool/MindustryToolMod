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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.mod.Mods.LoadedMod;
import mindustry.ui.FileChooser;
import mindustrytool.Main;
import mindustrytool.components.WebStyles;
import mindustrytool.features.modmanager.PackModel.FileEntry;
import mindustrytool.services.update.ChangelogFormatter;
import solim.core.Component;
import solim.layout.Direction;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class ModCard extends Component {

    private final LoadedMod mod;
    private final Runnable onStateChanged;
    private final Signal<Boolean> expanded = Signal.of(false);
    private final Signal<Integer> detailTab = Signal.of(0);

    public ModCard(LoadedMod mod, Runnable onStateChanged) {
        this.mod = mod;
        this.onStateChanged = onStateChanged;
    }

    public ModCard(
            LoadedMod mod,
            int stat,
            @Nullable String updateVersion,
            Runnable onStateChanged) {
        this(mod, onStateChanged);
    }

    @Override
    protected Element build() {
        ModService service = ModService.getInstance();
        String displayName = mod.meta != null && mod.meta.displayName != null
                ? mod.meta.displayName
                : mod.name;
        String version = mod.meta != null && mod.meta.version != null
                ? mod.meta.version
                : "";
        String author = mod.meta != null && mod.meta.author != null && !mod.meta.author.isEmpty()
                ? mod.meta.author
                : "Unknown";
        String shortDesc = mod.meta != null && mod.meta.shortDescription() != null
                ? mod.meta.shortDescription()
                : "";
        String fullDesc = mod.meta != null && mod.meta.description != null
                ? mod.meta.description
                : shortDesc;
        String repo = mod.getRepo();
        boolean isSelf = mod == Main.self;
        List<UnlockableContent> contents = collectModContents(mod);

        Computed<String> reactiveUpdateVersion = Signal.computed(() -> {
            Map<String, String> updates = service.availableUpdates().get();
            return repo != null && !repo.trim().isEmpty() && updates != null
                    ? updates.get(repo.trim().toLowerCase(Locale.ROOT))
                    : null;
        });

        Computed<Integer> reactiveStat = Signal.computed(() -> {
            service.installedRevision().get();
            String upd = reactiveUpdateVersion.get();
            return ModStat.checkModStat(mod, upd != null);
        });

        Computed<Boolean> canToggle = reactiveStat.map(st -> !isSelf && (mod.enabled() || ModStat.isValid(st != null ? st : 0)));

        Runnable toggleExpand = () -> expanded.set(!Boolean.TRUE.equals(expanded.peek()));

        return column().growX().gap(unit(1f)).children(() -> {
            card()
                    .growX()
                    .padding(unit(1.5f))
                    .rounded(unit(2), WebStyles.Colors.SECONDARY)
                    .border(1f, WebStyles.Colors.BORDER_INPUT)
                    .children(() -> {
                        row().growX().top().gap(unit(2.5f)).children(() -> {
                            card()
                                    .size(unit(22))
                                    .padding(unit(1))
                                    .center()
                                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                                    .border(1f, WebStyles.Colors.SECTION_BORDER)
                                    .onClick(toggleExpand)
                                    .children(() -> image(resolveModIcon(mod))
                                            .scaling(Scaling.fit)
                                            .size(unit(20)));

                            column().growX().top().left().gap(unit(0.8f)).paddingTop(unit(0.5f))
                                    .onClick(toggleExpand)
                                    .children(() -> {
                                        row().growX().top().gap(unit(1.5f)).children(() -> {
                                            text(displayName)
                                                    .color(Pal.accent)
                                                    .wrap()
                                                    .growX()
                                                    .left();
                                            dynamic(reactiveStat, st -> {
                                                int s = st != null ? st : 0;
                                                String upd = reactiveUpdateVersion.get();
                                                ModStatusBadges.renderCornerIcons(s, false, upd, true);
                                            });
                                        });
                                        if (!version.isEmpty()) {
                                            text(version)
                                                    .color(WebStyles.Colors.GHOST_FG)
                                                    .fontScale(0.8f)
                                                    .wrap()
                                                    .growX()
                                                    .left();
                                        }
                                        if (!shortDesc.isEmpty()) {
                                            text(shortDesc)
                                                    .color(WebStyles.Colors.SECONDARY_FG)
                                                    .wrap()
                                                    .growX()
                                                    .left();
                                        }
                                    });

                            divider(Direction.Y).width(2f).color(WebStyles.Colors.SECTION_BORDER).growY();

                            column().width(unit(11)).center().gap(unit(0.5f)).children(() -> {
                                button(() -> {
                                    if (!isSelf && Vars.mods != null) {
                                        Vars.mods.setEnabled(mod, !mod.enabled());
                                        ModService.getInstance().notifyInstalledModsChanged();
                                        onStateChanged.run();
                                    }
                                })
                                        .style(WebStyles.ghost())
                                        .size(unit(11))
                                        .enabled(canToggle)
                                        .tooltip(Core.bundle.get(mod.enabled()
                                                ? "mod-manager.action.disable"
                                                : "mod-manager.action.enable"))
                                        .children(() -> icon(mod.enabled() ? Icon.rightOpen : Icon.leftOpen)
                                                .size(unit(6)));

                                button(() -> showShareModDialog(
                                        displayName,
                                        version,
                                        shortDesc,
                                        repo,
                                        reactiveStat.peek() != null ? reactiveStat.peek() : 0,
                                        reactiveUpdateVersion.peek()))
                                        .style(WebStyles.ghost())
                                        .size(unit(11))
                                        .tooltip(Core.bundle.get("mod-manager.action.export"))
                                        .children(() -> icon(Icon.exportSmall).size(unit(6)));
                            });
                        });
                    });

            collapser(expanded).growX().children(() -> {
                card()
                        .growX()
                        .padding(unit(2.5f))
                        .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                        .border(1f, WebStyles.Colors.SECTION_BORDER)
                        .children(() -> {
                            column().growX().gap(unit(1.5f)).left().children(() -> {
                                row().growX().top().gap(unit(2)).children(() -> {
                                    column().growX().left().gap(unit(1.2f)).children(() -> {
                                        text(Core.bundle.format("mod-manager.by-author", author))
                                                .color(Pal.accent)
                                                .wrap()
                                                .left();

                                        row().growX().gap(unit(1.5f)).left().children(() -> {
                                            icon(Icon.githubSmall).size(unit(5)).color(WebStyles.Colors.GHOST_FG);
                                            if (repo != null && !repo.trim().isEmpty()) {
                                                String url = "https://github.com/" + repo.trim();
                                                button(url, () -> Core.app.openURI(url))
                                                        .style(WebStyles.ghostText())
                                                        .left();
                                            } else {
                                                text(Core.bundle.get("mod-manager.stat.local.desc"))
                                                        .color(WebStyles.Colors.GHOST_FG)
                                                        .fontScale(0.88f);
                                            }
                                        });

                                        dynamic(reactiveStat, st -> {
                                            int s = st != null ? st : 0;
                                            String upd = reactiveUpdateVersion.get();
                                            ModStatusBadges.renderDetailedStatusRows(s, upd, false, true);
                                        }).growX();
                                    });

                                    divider(Direction.Y).width(2f).color(WebStyles.Colors.SECTION_BORDER).growY();

                                    column().width(unit(11)).top().center().gap(unit(0.5f)).children(() -> {
                                        dynamic(reactiveUpdateVersion, upd -> {
                                            if (upd != null && repo != null && !repo.trim().isEmpty()) {
                                                button(() -> ModService.getInstance().installFromRepo(
                                                        repo,
                                                        mod.isJava(),
                                                        onStateChanged))
                                                        .style(WebStyles.ghost())
                                                        .size(unit(11))
                                                        .tooltip(Core.bundle.format(
                                                                "mod-manager.action.update.tooltip", upd))
                                                        .children(() -> icon(Icon.upSmall)
                                                                .size(unit(6))
                                                                .color(ModStatusBadges.LIGHT_BLUE));
                                            }
                                        });

                                        if (!isSelf) {
                                            button(this::confirmDeleteMod)
                                                    .style(WebStyles.ghost())
                                                    .size(unit(11))
                                                    .tooltip(Core.bundle.get("mod-manager.action.delete"))
                                                    .children(() -> icon(Icon.trashSmall).size(unit(6)));
                                        }
                                    });
                                });

                                divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);

                                row().growX().left().gap(unit(1)).children(() -> {
                                    subTabButton(Core.bundle.get("mod-manager.tab.formatted"), 0);
                                    subTabButton(Core.bundle.get("mod-manager.tab.raw"), 1);
                                    if (!contents.isEmpty()) {
                                        subTabButton(
                                                Core.bundle.format("mod-manager.tab.contents", contents.size()),
                                                2);
                                    }
                                });

                                card()
                                        .growX()
                                        .padding(unit(2.5f))
                                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                                        .border(1f, WebStyles.Colors.SECTION_BORDER)
                                        .children(() -> {
                                            dynamic(detailTab, activeIdx -> {
                                                int idx = activeIdx != null ? activeIdx : 0;
                                                if (idx == 0) {
                                                    if (fullDesc != null && !fullDesc.trim().isEmpty()) {
                                                        ChangelogFormatter.renderMarkdown(fullDesc);
                                                    } else {
                                                        text(Core.bundle.get("mod-manager.no-description"))
                                                                .color(WebStyles.Colors.GHOST_FG);
                                                    }
                                                } else if (idx == 1) {
                                                    text(fullDesc != null && !fullDesc.trim().isEmpty()
                                                            ? fullDesc
                                                            : Core.bundle.get("mod-manager.no-description"))
                                                            .color(WebStyles.Colors.SECONDARY_FG)
                                                            .wrap();
                                                } else {
                                                    wrap().growX().gap(unit(1)).left().children(() -> {
                                                        for (UnlockableContent c : contents) {
                                                            Drawable cIcon = c.uiIcon != null
                                                                    ? new TextureRegionDrawable(c.uiIcon)
                                                                    : Tex.nomap;
                                                            button(() -> {
                                                                if (Vars.ui != null && Vars.ui.content != null) {
                                                                    Vars.ui.content.show(c);
                                                                }
                                                            })
                                                                    .style(WebStyles.ghost())
                                                                    .size(unit(11))
                                                                    .tooltip(c.localizedName)
                                                                    .children(() -> image(cIcon)
                                                                            .scaling(Scaling.fit)
                                                                            .size(unit(8)));
                                                        }
                                                    });
                                                }
                                            }).growX();
                                        });
                            });
                        });
            });
        }).element();
    }

    private void subTabButton(String label, int index) {
        Readable<Boolean> active = detailTab.map(cur -> cur != null && cur == index);
        button(label, () -> detailTab.set(index))
                .style(WebStyles.filterChipText())
                .checked(active)
                .height(unit(9));
    }

    private static Drawable resolveModIcon(LoadedMod mod) {
        return mod.iconTexture != null
                ? new TextureRegionDrawable(new TextureRegion(mod.iconTexture))
                : Tex.nomap;
    }

    private static List<UnlockableContent> collectModContents(LoadedMod mod) {
        List<UnlockableContent> result = new ArrayList<>();
        if (!mod.enabled() || Vars.content == null || Vars.content.getContentMap() == null) {
            return result;
        }
        for (Seq<Content> seq : Vars.content.getContentMap()) {
            if (seq == null) {
                continue;
            }
            for (Content item : seq) {
                if (item instanceof UnlockableContent) {
                    UnlockableContent uc = (UnlockableContent) item;
                    if (uc.minfo != null && uc.minfo.mod == mod && !uc.isHidden()) {
                        result.add(uc);
                    }
                }
            }
        }
        return result;
    }

    private void confirmDeleteMod() {
        String displayName = mod.meta != null && mod.meta.displayName != null
                ? mod.meta.displayName
                : mod.name;
        if (Vars.ui != null) {
            Vars.ui.showConfirm(
                    Core.bundle.get("mod-manager.delete.title"),
                    Core.bundle.format("mod-manager.delete.confirm", displayName),
                    () -> {
                        if (Vars.mods != null) {
                            Vars.mods.removeMod(mod);
                            ModService.getInstance().notifyInstalledModsChanged();
                            onStateChanged.run();
                        }
                    });
        }
    }

    private void showShareModDialog(
            String displayName,
            String version,
            String shortDesc,
            @Nullable String repo,
            int stat,
            @Nullable String updateVersion) {
        SolimDialog dlg = new SolimDialog(displayName)
                .addCloseButton()
                .closeOnBack()
                .maxWidth(500f);

        dlg.children(() -> {
            column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                card()
                        .growX()
                        .padding(unit(2))
                        .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                        .border(1f, WebStyles.Colors.SECTION_BORDER)
                        .children(() -> {
                            row().growX().top().gap(unit(2.5f)).left().children(() -> {
                                card()
                                        .size(unit(22))
                                        .padding(unit(1))
                                        .center()
                                        .rounded(unit(2), WebStyles.Colors.SECONDARY)
                                        .border(1f, WebStyles.Colors.BORDER_INPUT)
                                        .children(() -> image(resolveModIcon(mod))
                                                .scaling(Scaling.fit)
                                                .size(unit(20)));

                                column().growX().top().left().gap(unit(0.8f)).children(() -> {
                                    text(displayName).color(Pal.accent).wrap().left();
                                    if (!version.isEmpty()) {
                                        text(version).color(WebStyles.Colors.GHOST_FG).fontScale(0.8f).wrap().left();
                                    }
                                    if (!shortDesc.isEmpty()) {
                                        text(shortDesc).color(WebStyles.Colors.SECONDARY_FG).wrap().left();
                                    }
                                });

                                ModStatusBadges.renderCornerIcons(stat, false, updateVersion, true);
                            });
                        });

                if (repo != null && !repo.trim().isEmpty()) {
                    String url = "https://github.com/" + repo.trim();
                    button(() -> {
                        Core.app.openURI(url);
                        dlg.hide();
                    })
                            .style(WebStyles.secondaryText())
                            .growX()
                            .height(unit(11))
                            .gap(unit(2))
                            .children(() -> {
                                icon(Icon.githubSmall).size(unit(6));
                                text("GitHub");
                            });
                }

                if (mod.file != null && mod.file.exists()) {
                    button(() -> {
                        exportModArchive();
                        dlg.hide();
                    })
                            .style(WebStyles.secondaryText())
                            .growX()
                            .height(unit(11))
                            .gap(unit(2))
                            .children(() -> {
                                icon(Icon.exportSmall).size(unit(6));
                                text(Core.bundle.get("mod-manager.action.export"));
                            });

                    button(() -> {
                        Core.app.openFolder(mod.file.file().getAbsolutePath());
                        dlg.hide();
                    })
                            .style(WebStyles.secondaryText())
                            .growX()
                            .height(unit(11))
                            .gap(unit(2))
                            .children(() -> {
                                icon(Icon.folder).size(unit(6));
                                text(Core.bundle.get("mod-manager.action.open-folder"));
                            });
                }
            });
        });
        dlg.show();
    }

    private void exportModArchive() {
        if (mod.file == null || !mod.file.exists()) {
            return;
        }
        String ext = mod.file.extension().isEmpty() ? "zip" : mod.file.extension();
        FileChooser.save(ext)
                .name(mod.name + "." + ext)
                .submit(dest -> {
                    if (dest == null) {
                        return;
                    }
                    try {
                        if (mod.file.isDirectory()) {
                            PackModel tempPack = new PackModel();
                            tempPack.setName(mod.name);
                            tempPack.setDisplayName(mod.name);
                            tempPack.setVersion("1.0");
                            tempPack.setAuthor("export");
                            tempPack.getFiles().add(new FileEntry(
                                    mod.name,
                                    mod.file.file().getAbsolutePath(),
                                    "custom"));
                            ModpackService.exportModpack(tempPack, dest);
                        } else {
                            Fi target = dest.extension().isEmpty()
                                    ? dest.sibling(dest.name() + "." + mod.file.extension())
                                    : dest;
                            mod.file.copyTo(target);
                        }
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
}
