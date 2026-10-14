package mindustrytool.features.modmanager;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Scaling;
import java.util.Locale;
import java.util.Set;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.mod.Mods.LoadedMod;
import mindustrytool.components.WebStyles;
import mindustrytool.services.update.ChangelogFormatter;
import solim.core.Component;
import solim.layout.Direction;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class RemoteModCard extends Component {

    private final ModListing listing;
    private final Runnable onFavoriteClicked;
    private final Runnable onInstalledChanged;
    private final Signal<Boolean> expanded = Signal.of(false);
    private final Signal<Integer> detailTab = Signal.of(0);

    public RemoteModCard(
            ModListing listing,
            Runnable onFavoriteClicked,
            Runnable onInstalledChanged) {
        this.listing = listing;
        this.onFavoriteClicked = onFavoriteClicked;
        this.onInstalledChanged = onInstalledChanged;
    }

    @Override
    protected Element build() {
        ModService service = ModService.getInstance();
        LocalFavoritesStore favStore = LocalFavoritesStore.getInstance();

        String displayName = listing.getName() != null && !listing.getName().isEmpty()
                ? listing.getName()
                : listing.getInternalName();
        String version = listing.getVersion() != null ? listing.getVersion() : "";
        String author = listing.getAuthor() != null && !listing.getAuthor().isEmpty()
                ? listing.getAuthor()
                : "Unknown";
        String shortDesc = listing.getShortDescription();
        String fullDesc = listing.getDescription() != null && !listing.getDescription().isEmpty()
                ? listing.getDescription()
                : shortDesc;
        String repo = listing.getRepo() != null ? listing.getRepo().trim() : "";
        String repoKey = repo.toLowerCase(Locale.ROOT);
        int stat = ModStat.checkRemoteModStat(listing);
        ModName modName = ModName.fromListing(listing);

        Computed<Boolean> isFavorited = Signal.computed(() -> {
            favStore.version().get();
            return favStore.isFavorited(modName);
        });

        Computed<Color> starColor = isFavorited.map(fav -> Boolean.TRUE.equals(fav) ? Pal.accent : Color.white);

        Computed<LoadedMod> installedMod = Signal.computed(() -> {
            service.installedRevision().get();
            return service.findInstalledMod(listing);
        });

        Computed<Boolean> isInstalled = installedMod.map(m -> m != null);

        Computed<String> installedVer = installedMod.map(m -> m != null && m.meta != null && m.meta.version != null
                ? m.meta.version
                : "");

        Computed<Boolean> hasUpdate = Signal.computed(() -> {
            boolean inst = Boolean.TRUE.equals(isInstalled.get());
            String instV = installedVer.get();
            return inst && VersionCompareHelper.tryCompareVersion(instV != null ? instV : "", version) < 0;
        });

        Computed<Boolean> isInstalling = Signal.computed(() -> {
            Set<String> set = service.installingRepos().get();
            return set != null && set.contains(repoKey);
        });

        Computed<Boolean> canDownload = isInstalling.map(inst -> !Boolean.TRUE.equals(inst) && !repo.isEmpty());

        Computed<Color> downloadIconColor = hasUpdate.map(
                upd -> Boolean.TRUE.equals(upd) ? ModStatusBadges.LIGHT_BLUE : Color.white);

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
                                    .children(() -> {
                                        stack()
                                                .size(unit(20))
                                                .layer(() -> networkImage(listing.getIconUrl())
                                                        .scaling(Scaling.fit)
                                                        .size(unit(20)))
                                                .layer(() -> column()
                                                        .size(unit(20))
                                                        .bottom()
                                                        .left()
                                                        .children(() -> card()
                                                                .rounded(unit(1), WebStyles.Colors.DARK_90)
                                                                .padding(unit(0.3f), unit(1f), unit(0.3f), unit(1f))
                                                                .children(() -> row()
                                                                        .gap(unit(0.6f))
                                                                        .left()
                                                                        .children(() -> {
                                                                            icon(Icon.starSmall)
                                                                                    .size(unit(4f))
                                                                                    .color(Pal.accent);
                                                                            text(String.valueOf(listing.getStars()))
                                                                                    .color(Color.white)
                                                                                    .fontScale(0.78f);
                                                                        }))));
                                    });

                            column().growX().top().left().gap(unit(0.8f)).paddingTop(unit(0.5f))
                                    .onClick(toggleExpand)
                                    .children(() -> {
                                        row().growX().top().gap(unit(1.5f)).children(() -> {
                                            text(displayName)
                                                    .color(Pal.accent)
                                                    .wrap()
                                                    .growX()
                                                    .left();
                                            dynamic(installedMod, inst -> {
                                                boolean instFlag = inst != null;
                                                boolean updFlag = Boolean.TRUE.equals(hasUpdate.peek());
                                                ModStatusBadges.renderCornerIcons(
                                                        stat,
                                                        true,
                                                        updFlag ? version : null,
                                                        instFlag);
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
                                button(onFavoriteClicked)
                                        .style(WebStyles.ghost())
                                        .size(unit(11))
                                        .tooltip(Core.bundle.get("mod-browser.action.favorite"))
                                        .children(() -> icon(Icon.star)
                                                .size(unit(6f))
                                                .color(starColor));

                                button(() -> showDownloadModDialog(
                                        displayName,
                                        version,
                                        installedVer.peek() != null ? installedVer.peek() : "",
                                        Boolean.TRUE.equals(isInstalled.peek()),
                                        Boolean.TRUE.equals(hasUpdate.peek()),
                                        stat))
                                        .style(WebStyles.ghost())
                                        .size(unit(11))
                                        .enabled(canDownload)
                                        .tooltip(hasUpdate.map(upd -> Core.bundle.get(Boolean.TRUE.equals(upd)
                                                ? "mod-browser.action.reinstall"
                                                : "mod-browser.action.install")))
                                        .children(() -> icon(isInstalling.map(inst -> Boolean.TRUE.equals(inst)
                                                ? Icon.refreshSmall
                                                : Icon.downloadSmall))
                                                .size(unit(6f))
                                                .color(downloadIconColor));
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
                                text(Core.bundle.format("mod-manager.by-author", author))
                                        .color(Pal.accent)
                                        .wrap()
                                        .left();

                                if (!repo.isEmpty()) {
                                    String url = "https://github.com/" + repo;
                                    row().growX().gap(unit(1.5f)).left().children(() -> {
                                        icon(Icon.githubSmall).size(unit(5)).color(WebStyles.Colors.GHOST_FG);
                                        button(url, () -> Core.app.openURI(url))
                                                .style(WebStyles.ghostText())
                                                .left();
                                    });
                                }

                                dynamic(installedMod, inst -> {
                                    boolean instFlag = inst != null;
                                    boolean updFlag = Boolean.TRUE.equals(hasUpdate.peek());
                                    ModStatusBadges.renderDetailedStatusRows(
                                            stat,
                                            updFlag ? version : null,
                                            true,
                                            instFlag);
                                }).growX();

                                divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);

                                row().growX().left().gap(unit(1)).children(() -> {
                                    subTabButton(Core.bundle.get("mod-manager.tab.formatted"), 0);
                                    subTabButton(Core.bundle.get("mod-manager.tab.raw"), 1);
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
                                                } else {
                                                    text(fullDesc != null && !fullDesc.trim().isEmpty()
                                                            ? fullDesc
                                                            : Core.bundle.get("mod-manager.no-description"))
                                                            .color(WebStyles.Colors.SECONDARY_FG)
                                                            .wrap();
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

    private void showDownloadModDialog(
            String displayName,
            String remoteVer,
            String installedVer,
            boolean isInstalled,
            boolean hasUpdate,
            int stat) {
        String title = Core.bundle.get(hasUpdate ? "mod-browser.action.reinstall" : "mod-browser.action.install");
        SolimDialog dlg = new SolimDialog(title)
                .addCloseButton()
                .closeOnBack()
                .maxWidth(480f);

        dlg.children(() -> {
            column().grow().center().gap(unit(2.5f)).padding(unit(2.5f)).children(() -> {
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
                                        .children(() -> networkImage(listing.getIconUrl())
                                                .scaling(Scaling.fit)
                                                .size(unit(20)));

                                column().growX().top().left().gap(unit(1f)).children(() -> {
                                    text(displayName).color(Pal.accent).wrap().left();
                                    if (!isInstalled) {
                                        text(remoteVer).color(WebStyles.Colors.GHOST_FG).left();
                                    } else if (hasUpdate) {
                                        text(installedVer + "  >>>  " + remoteVer)
                                                .color(Pal.accent)
                                                .left();
                                    } else {
                                        text(installedVer + "  (" + Core.bundle.get("mod-browser.badge.installed") + ")")
                                                .color(WebStyles.Colors.GHOST_FG)
                                                .left();
                                    }
                                });

                                ModStatusBadges.renderCornerIcons(stat, true, hasUpdate ? remoteVer : null, isInstalled);
                            });
                        });

                row().growX().gap(unit(2)).children(() -> {
                    button(Core.bundle.get("cancel", "Cancel"), dlg::hide)
                            .style(WebStyles.secondaryText())
                            .growX()
                            .height(unit(11));

                    button(() -> {
                        dlg.hide();
                        ModService.getInstance().installFromRepo(
                                listing.getRepo(),
                                listing.isHasJava(),
                                () -> {
                                    onInstalledChanged.run();
                                    if (Vars.ui != null) {
                                        Vars.ui.showInfoFade(Core.bundle.get("mod-browser.badge.installed"));
                                    }
                                });
                    })
                            .style(WebStyles.primaryText())
                            .growX()
                            .height(unit(11))
                            .gap(unit(1.5f))
                            .children(() -> {
                                icon(Icon.download).size(unit(5.5f));
                                text(title);
                            });
                });
            });
        });
        dlg.show();
    }
}
