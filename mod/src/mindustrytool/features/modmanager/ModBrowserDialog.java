package mindustrytool.features.modmanager;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Scl;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.FileChooser;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import mindustrytool.features.modmanager.LocalFavoritesStore.ConflictResolution;
import mindustrytool.features.modmanager.LocalFavoritesStore.FavoriteFolder;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Signal;

public final class ModBrowserDialog extends SolimDialog {

    private static final int PAGE_SIZE = 60;

    public ModBrowserDialog() {
        super(Core.bundle.get("mod-browser.title"));
        name("heModBrowserDialog");
        closeOnBack();
        fillParent(true);
        cont().background(Styles.black8);

        shown(() -> {
            LocalFavoritesStore.getInstance().ensureLoaded();
            ModService.getInstance().fetchModListings(false, null);
        });

        children(() -> new ModBrowserView(this::hide));
    }

    private static final class ModBrowserView extends Component {
        private final Runnable onClose;
        private final Signal<String> searchQuery = Signal.of("");
        private final Signal<Boolean> orderByDate = Signal.of(false);
        private final Signal<Boolean> reverseOrder = Signal.of(false);
        private final Signal<Boolean> hideIncompatible = Signal.of(true);
        private final Signal<Integer> visibleLimit = Signal.of(PAGE_SIZE);

        private final Computed<List<ModListing>> filteredMods = new Computed<>(() -> {
            ModService service = ModService.getInstance();
            List<ModListing> raw = service.listings().get();
            service.installedRevision().get();

            String query = searchQuery.get() != null
                    ? searchQuery.get().trim().toLowerCase(Locale.ROOT)
                    : "";
            boolean byDate = Boolean.TRUE.equals(orderByDate.get());
            boolean reverse = Boolean.TRUE.equals(reverseOrder.get());
            boolean hideInvalid = Boolean.TRUE.equals(hideIncompatible.get());

            List<ModListing> filtered = new ArrayList<>();
            if (raw != null) {
                for (ModListing item : raw) {
                    if (item == null || !matchesSearch(item, query)) {
                        continue;
                    }
                    if (hideInvalid && !ModStat.isValid(ModStat.checkRemoteModStat(item))) {
                        continue;
                    }
                    filtered.add(item);
                }
            }

            Comparator<ModListing> comparator = byDate
                    ? (a, b) -> safeStr(b.getLastUpdated()).compareTo(safeStr(a.getLastUpdated()))
                    : (a, b) -> Integer.compare(b.getStars(), a.getStars());
            filtered.sort(comparator);
            if (reverse) {
                Collections.reverse(filtered);
            }
            return filtered;
        });

        private final Computed<List<ModListing>> visibleMods = Signal.computed(() -> {
            List<ModListing> all = filteredMods.get();
            Integer lim = visibleLimit.get();
            int limit = lim != null ? lim : PAGE_SIZE;
            if (all == null || all.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<>(all.subList(0, Math.min(limit, all.size())));
        });

        private final Computed<Integer> remainingCount = Signal.computed(() -> {
            List<ModListing> all = filteredMods.get();
            List<ModListing> vis = visibleMods.get();
            int total = all != null ? all.size() : 0;
            int shown = vis != null ? vis.size() : 0;
            return Math.max(0, total - shown);
        });

        private final Computed<Boolean> hasMoreMods = remainingCount.map(rem -> rem != null && rem > 0);

        ModBrowserView(Runnable onClose) {
            this.onClose = onClose;
        }

        @Override
        protected Element build() {
            ModService service = ModService.getInstance();
            LocalFavoritesStore favStore = LocalFavoritesStore.getInstance();

            Computed<Integer> columnCount = dvw(100f).map(w -> {
                if (w != null && w > 200f) {
                    return Math.max(1, (int) (w / unit(135f)));
                }
                if (Core.graphics != null && Core.graphics.getWidth() > 0) {
                    return Math.max(1, (int) (Core.graphics.getWidth() / Scl.scl(540f)));
                }
                return 2;
            });

            Computed<List<FavoriteFolder>> foldersSignal = Signal.computed(() -> {
                favStore.version().get();
                List<FavoriteFolder> f = favStore.folders().get();
                return f != null ? f : Collections.emptyList();
            });

            Computed<Boolean> hasFolders = foldersSignal.map(f -> f != null && !f.isEmpty());
            Computed<Boolean> noFolders = hasFolders.map(h -> !Boolean.TRUE.equals(h));

            return column().grow().gap(unit(1.5f)).padding(unit(2)).children(() -> {
                row().growX().gap(unit(2)).paddingLeft(unit(2)).paddingRight(unit(2)).center().children(() -> {
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
                                        .placeholder(Core.bundle.get("mod-browser.search.placeholder"));
                                button(() -> searchQuery.set(""))
                                        .style(WebStyles.ghost())
                                        .size(unit(9))
                                        .visible(searchQuery.map(q -> q != null && !q.isEmpty()))
                                        .children(() -> icon(Icon.cancel).size(unit(5)));
                            });

                    button(() -> orderByDate.set(!Boolean.TRUE.equals(orderByDate.peek())))
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle.get("mod-browser.sort.stars")
                                    + " / " + Core.bundle.get("mod-browser.sort.updated"))
                            .children(() -> icon(orderByDate.map(byDate -> Boolean.TRUE.equals(byDate)
                                    ? Icon.list
                                    : Icon.star)).size(unit(6f)));

                    button(() -> reverseOrder.set(!Boolean.TRUE.equals(reverseOrder.peek())))
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .children(() -> icon(reverseOrder.map(rev -> Boolean.TRUE.equals(rev)
                                    ? Icon.upOpen
                                    : Icon.downOpen)).size(unit(6f)));

                    checkbox(Core.bundle.get("mod-browser.filter.hide-invalid"), hideIncompatible);
                });

                divider().height(3f).color(Pal.accent);

                scroll().grow().scrollX(false).children(() -> {
                    dynamic(service.loading(), isLoading -> {
                        if (Boolean.TRUE.equals(isLoading) && service.listings().peek().isEmpty()) {
                            column().growX().center().padding(unit(8)).gap(unit(2)).children(() ->
                                    text(Core.bundle.get("mod-browser.loading")).color(Pal.accent));
                            return;
                        }

                        column().growX().top().gap(unit(2.5f)).paddingLeft(unit(2)).paddingRight(unit(2)).children(() -> {
                            row().growX().gap(unit(1.5f)).left().paddingLeft(unit(1)).children(() -> {
                                icon(Icon.starSmall).size(unit(5.5f)).color(Pal.accent);
                                text(Core.bundle.get("mod-browser.favorites.title")).color(Pal.accent);
                            });
                            divider().height(3f).color(Pal.accent);

                            row().growX().center().gap(unit(2)).padding(unit(3)).visible(noFolders).children(() -> {
                                icon(Icon.box).size(unit(8)).color(Pal.accent);
                                text(Core.bundle.get("mod-browser.favorites.empty")).color(WebStyles.Colors.GHOST_FG);
                            });

                            column().growX().gap(unit(1.5f)).visible(hasFolders).children(() -> {
                                reactiveGrid(foldersSignal)
                                        .columns(1)
                                        .key(FavoriteFolder::getName)
                                        .gap(unit(1.5f))
                                        .children(folder -> new FavoriteFolderSection(folder, columnCount));
                            });

                            row().growX().gap(unit(1.5f)).left().paddingLeft(unit(1)).children(() -> {
                                icon(Icon.planet).size(unit(5.5f)).color(Pal.accent);
                                text(filteredMods.map(list -> Core.bundle.format(
                                        "mod-browser.count",
                                        list != null ? list.size() : 0)))
                                        .color(Pal.accent);
                            });
                            divider().height(3f).color(Pal.accent);

                            card()
                                    .growX()
                                    .padding(unit(2f))
                                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                                    .border(1f, WebStyles.Colors.SECTION_BORDER)
                                    .children(() -> {
                                        column().growX().gap(unit(2)).children(() -> {
                                            reactiveGrid(visibleMods)
                                                    .columns(columnCount)
                                                    .key(item -> item.getRepo() != null && !item.getRepo().isEmpty()
                                                            ? item.getRepo()
                                                            : item.getInternalName())
                                                    .gap(unit(2))
                                                    .empty(() -> text(Core.bundle.get("mod-browser.empty"))
                                                            .color(WebStyles.Colors.GHOST_FG)
                                                            .padding(unit(4)))
                                                    .children(item -> new RemoteModCard(
                                                            item,
                                                            () -> showAddToFavoritesDialog(item),
                                                            () -> ModService.getInstance().notifyInstalledModsChanged()));

                                            button(
                                                    remainingCount.map(rem -> Core.bundle.format(
                                                            "mod-browser.count",
                                                            rem != null ? rem : 0)),
                                                    () -> {
                                                        Integer cur = visibleLimit.peek();
                                                        visibleLimit.set((cur != null ? cur : PAGE_SIZE) + PAGE_SIZE);
                                                    })
                                                    .style(WebStyles.secondaryText())
                                                    .visible(hasMoreMods)
                                                    .growX()
                                                    .height(unit(11));
                                        });
                                    });
                        });
                    }).growX();
                });

                divider().height(1.5f).color(WebStyles.Colors.SECTION_BORDER);

                wrap().growX().gap(unit(2)).center().children(() -> {
                    heliumBottomButton(
                            Core.bundle.get("back", "Back"),
                            Icon.leftOpen,
                            onClose);
                    heliumBottomButton(
                            Core.bundle.get("mod-manager.action.refresh"),
                            Icon.refresh,
                            () -> service.fetchModListings(true, null));
                    heliumBottomButton(
                            Core.bundle.get("mod-browser.favorites.new-folder"),
                            Icon.add,
                            this::showCreateFolderDialog);
                    heliumBottomButton(
                            Core.bundle.get("mod-browser.favorites.import"),
                            Icon.download,
                            this::showImportFolderDialog);
                });
            }).element();
        }

        private final class FavoriteFolderSection extends Component {
            private final FavoriteFolder initialFolder;
            private final Computed<Integer> columnCount;

            FavoriteFolderSection(FavoriteFolder folder, Computed<Integer> columnCount) {
                this.initialFolder = folder;
                this.columnCount = columnCount;
            }

            @Override
            protected Element build() {
                LocalFavoritesStore favStore = LocalFavoritesStore.getInstance();
                String folderName = initialFolder.getName();

                Computed<FavoriteFolder> liveFolder = Signal.computed(() -> {
                    favStore.version().get();
                    List<FavoriteFolder> all = favStore.folders().get();
                    if (all != null) {
                        for (FavoriteFolder f : all) {
                            if (f.getName().equalsIgnoreCase(folderName)) {
                                return f;
                            }
                        }
                    }
                    return initialFolder;
                });

                Computed<Boolean> isExpanded = liveFolder.map(f -> f != null && !f.isCollapsed());

                Computed<List<ModListing>> folderMods = Signal.computed(() -> {
                    FavoriteFolder f = liveFolder.get();
                    List<ModListing> allFiltered = filteredMods.get();
                    List<ModListing> result = new ArrayList<>();
                    if (f != null && allFiltered != null) {
                        for (ModListing m : allFiltered) {
                            if (f.contains(ModName.fromListing(m))) {
                                result.add(m);
                            }
                        }
                    }
                    return result;
                });

                return column().growX().gap(unit(1f)).children(() -> {
                    card()
                            .growX()
                            .padding(unit(1.2f), unit(2f), unit(1.2f), unit(2f))
                            .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                            .border(1f, WebStyles.Colors.SECTION_BORDER)
                            .children(() -> {
                                row().growX().center().gap(unit(1.5f)).children(() -> {
                                    button(() -> favStore.toggleCollapsed(folderName))
                                            .style(WebStyles.ghost())
                                            .growX()
                                            .children(() -> row().growX().left().gap(unit(1.5f)).children(() -> {
                                                icon(isExpanded.map(exp -> Boolean.TRUE.equals(exp)
                                                        ? Icon.downOpen
                                                        : Icon.rightOpen))
                                                        .size(unit(5.5f));
                                                text(liveFolder.map(f -> (f != null ? f.getName() : folderName)
                                                        + " (" + (f != null ? f.getMods().size() : 0) + ")"))
                                                        .color(Pal.accent)
                                                        .left();
                                            }));

                                    row().gap(unit(1)).right().children(() -> {
                                        button(() -> showRenameFolderDialog(liveFolder.peek()))
                                                .style(WebStyles.ghost())
                                                .size(unit(11))
                                                .tooltip(Core.bundle.get("mod-browser.favorites.rename"))
                                                .children(() -> icon(Icon.edit).size(unit(6f)));

                                        button(() -> confirmDeleteFolder(liveFolder.peek()))
                                                .style(WebStyles.ghost())
                                                .size(unit(11))
                                                .tooltip(Core.bundle.get("mod-browser.favorites.delete"))
                                                .children(() -> icon(Icon.trash).size(unit(6f)));

                                        button(() -> installAllInFolder(liveFolder.peek()))
                                                .style(WebStyles.ghost())
                                                .size(unit(11))
                                                .tooltip(Core.bundle.get("mod-browser.favorites.install-all"))
                                                .children(() -> icon(Icon.download).size(unit(6f)));

                                        button(() -> showExportFolderDialog(liveFolder.peek()))
                                                .style(WebStyles.ghost())
                                                .size(unit(11))
                                                .tooltip(Core.bundle.get("mod-browser.favorites.export"))
                                                .children(() -> icon(Icon.export).size(unit(6f)));
                                    });
                                });
                            });

                    collapser(isExpanded).growX().children(() -> {
                        card()
                                .growX()
                                .padding(unit(2f))
                                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                                .border(1f, WebStyles.Colors.SECTION_BORDER)
                                .children(() -> {
                                    reactiveGrid(folderMods)
                                            .columns(columnCount)
                                            .key(item -> item.getRepo() != null && !item.getRepo().isEmpty()
                                                    ? item.getRepo()
                                                    : item.getInternalName())
                                            .gap(unit(2))
                                            .empty(() -> text(Core.bundle.get("mod-browser.empty"))
                                                    .color(WebStyles.Colors.GHOST_FG)
                                                    .padding(unit(2)))
                                            .children(item -> new RemoteModCard(
                                                    item,
                                                    () -> favStore.toggleModInFolder(
                                                            folderName,
                                                            ModName.fromListing(item)),
                                                    () -> ModService.getInstance().notifyInstalledModsChanged()));
                                });
                    });
                }).element();
            }
        }

        private static void heliumBottomButton(String label, Drawable iconDrawable, Runnable onClick) {
            button(onClick)
                    .style(WebStyles.secondary())
                    .width(unit(56f))
                    .height(unit(13f))
                    .gap(unit(2f))
                    .children(() -> {
                        icon(iconDrawable).size(unit(6f));
                        text(label);
                    });
        }

        private void showAddToFavoritesDialog(ModListing listing) {
            LocalFavoritesStore store = LocalFavoritesStore.getInstance();
            ModName modName = ModName.fromListing(listing);

            SolimDialog dlg = new SolimDialog(Core.bundle.get("mod-browser.favorites.pick-title"))
                    .addCloseButton()
                    .closeOnBack()
                    .maxWidth(460f);

            dlg.children(() -> {
                column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                    dynamic(store.folders(), folders -> {
                        column().growX().gap(unit(1.5f)).children(() -> {
                            if (folders == null || folders.isEmpty()) {
                                text(Core.bundle.get("mod-browser.favorites.empty"))
                                        .color(WebStyles.Colors.GHOST_FG);
                            } else {
                                for (FavoriteFolder folder : folders) {
                                    boolean inFolder = folder.contains(modName);
                                    button(() -> store.toggleModInFolder(folder.getName(), modName))
                                            .style(WebStyles.secondary())
                                            .growX()
                                            .height(unit(11))
                                            .gap(unit(2))
                                            .children(() -> {
                                                icon(inFolder ? Icon.ok : Icon.box)
                                                        .color(inFolder ? Pal.accent : WebStyles.Colors.GHOST_FG)
                                                        .size(unit(5.5f));
                                                text(folder.getName())
                                                        .color(inFolder ? Pal.accent : Color.white)
                                                        .growX()
                                                        .left();
                                            });
                                }
                            }

                            button(() -> {
                                dlg.hide();
                                showCreateFolderDialog();
                            })
                                    .style(WebStyles.outline())
                                    .growX()
                                    .height(unit(11))
                                    .gap(unit(1.5f))
                                    .children(() -> {
                                        icon(Icon.add).size(unit(5.5f));
                                        text(Core.bundle.get("mod-browser.favorites.new-folder"));
                                    });
                        });
                    }).growX();
                });
            });
            dlg.show();
        }

        private void showCreateFolderDialog() {
            Signal<String> nameSignal = Signal.of("");
            SolimDialog dlg = new SolimDialog(Core.bundle.get("mod-browser.favorites.new-folder"))
                    .addCloseButton()
                    .closeOnBack()
                    .maxWidth(440f);

            dlg.children(() -> {
                column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                    text(Core.bundle.get("mod-browser.favorites.folder-name.placeholder"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left();
                    row().growX().gap(unit(1.5f)).children(() -> {
                        row().growX().gap(unit(1.5f))
                                .rounded(unit(2))
                                .paddingLeft(unit(2))
                                .paddingRight(unit(2))
                                .height(unit(11))
                                .center()
                                .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                                .children(() -> textField(nameSignal)
                                        .growX()
                                        .style(WebStyles.clearInput())
                                        .placeholder("Favorites"));
                        button(() -> {
                            String clip = Core.app.getClipboardText();
                            if (clip != null) {
                                nameSignal.set(clip.trim());
                            }
                        })
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .children(() -> icon(Icon.paste).size(unit(6f)));
                    });
                    button(Core.bundle.get("mod-browser.favorites.create"), () -> {
                        LocalFavoritesStore.getInstance().createFolder(nameSignal.peek());
                        dlg.hide();
                    })
                            .style(WebStyles.primaryText())
                            .growX()
                            .height(unit(11));
                });
            });
            dlg.show();
        }

        private void showRenameFolderDialog(FavoriteFolder folder) {
            if (folder == null) {
                return;
            }
            Signal<String> nameSignal = Signal.of(folder.getName());
            SolimDialog dlg = new SolimDialog(Core.bundle.get("mod-browser.favorites.rename"))
                    .addCloseButton()
                    .closeOnBack()
                    .maxWidth(440f);

            dlg.children(() -> {
                column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                    text(Core.bundle.get("mod-browser.favorites.folder-name.placeholder"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left();
                    row().growX().gap(unit(1.5f))
                            .rounded(unit(2))
                            .paddingLeft(unit(2))
                            .paddingRight(unit(2))
                            .height(unit(11))
                            .center()
                            .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                            .children(() -> textField(nameSignal)
                                    .growX()
                                    .style(WebStyles.clearInput()));
                    button(Core.bundle.get("mod-browser.favorites.rename"), () -> {
                        LocalFavoritesStore.getInstance().renameFolder(folder.getName(), nameSignal.peek());
                        dlg.hide();
                    })
                            .style(WebStyles.primaryText())
                            .growX()
                            .height(unit(11));
                });
            });
            dlg.show();
        }

        private void confirmDeleteFolder(FavoriteFolder folder) {
            if (folder == null || Vars.ui == null) {
                return;
            }
            Vars.ui.showConfirm(
                    Core.bundle.get("mod-browser.favorites.delete"),
                    Core.bundle.format("mod-browser.favorites.delete.confirm", folder.getName()),
                    () -> LocalFavoritesStore.getInstance().deleteFolder(folder.getName()));
        }

        private void installAllInFolder(FavoriteFolder folder) {
            if (folder == null) {
                return;
            }
            List<ModListing> all = ModService.getInstance().listings().peek();
            int queued = 0;
            for (ModName modName : folder.getMods()) {
                String repo = modName.getRepo();
                boolean isJava = false;
                if ((repo == null || repo.isEmpty()) && all != null) {
                    for (ModListing item : all) {
                        if (modName.matches(item)) {
                            repo = item.getRepo();
                            isJava = item.isHasJava();
                            break;
                        }
                    }
                }
                if (repo != null && !repo.trim().isEmpty()) {
                    ModService.getInstance().installFromRepo(repo, isJava, null);
                    queued++;
                }
            }
            if (Vars.ui != null && queued > 0) {
                Vars.ui.showInfoFade(Core.bundle.format("mod-browser.favorites.installing-count", queued));
            }
        }

        private void showExportFolderDialog(FavoriteFolder folder) {
            if (folder == null) {
                return;
            }
            String json = LocalFavoritesStore.exportFolderJson(folder);
            SolimDialog dlg = new SolimDialog(Core.bundle.get("mod-browser.favorites.export"))
                    .addCloseButton()
                    .closeOnBack()
                    .maxWidth(480f);

            dlg.children(() -> {
                column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                    text(folder.getName()).color(Pal.accent).left();
                    card()
                            .growX()
                            .padding(unit(2))
                            .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                            .border(1f, WebStyles.Colors.SECTION_BORDER)
                            .children(() -> text(json).color(WebStyles.Colors.GHOST_FG).fontScale(0.8f).wrap());

                    row().growX().gap(unit(2)).children(() -> {
                        button(() -> {
                            Core.app.setClipboardText(json);
                            if (Vars.ui != null) {
                                Vars.ui.showInfoFade(Core.bundle.get("mod-browser.favorites.copied"));
                            }
                            dlg.hide();
                        })
                                .style(WebStyles.secondary())
                                .growX()
                                .height(unit(11))
                                .gap(unit(1.5f))
                                .children(() -> {
                                    icon(Icon.copy).size(unit(5.5f));
                                    text(Core.bundle.get("mod-browser.favorites.export.clipboard"));
                                });

                        button(() -> {
                            FileChooser.save("json")
                                    .name(folder.getName() + ".json")
                                    .submit(dest -> {
                                        if (dest != null) {
                                            dest.writeBytes(json.getBytes(StandardCharsets.UTF_8), false);
                                        }
                                    });
                            dlg.hide();
                        })
                                .style(WebStyles.primary())
                                .growX()
                                .height(unit(11))
                                .gap(unit(1.5f))
                                .children(() -> {
                                    icon(Icon.file).size(unit(5.5f));
                                    text(Core.bundle.get("mod-browser.favorites.export.file"));
                                });
                    });
                });
            });
            dlg.show();
        }

        private void showImportFolderDialog() {
            Signal<String> jsonInput = Signal.of("");
            SolimDialog dlg = new SolimDialog(Core.bundle.get("mod-browser.favorites.import"))
                    .addCloseButton()
                    .closeOnBack()
                    .maxWidth(480f);

            dlg.children(() -> {
                column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                    text(Core.bundle.get("mod-browser.favorites.import.clipboard"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .wrap();
                    row().growX().gap(unit(1.5f)).children(() -> {
                        row().growX().gap(unit(1.5f))
                                .rounded(unit(2))
                                .paddingLeft(unit(2))
                                .paddingRight(unit(2))
                                .height(unit(11))
                                .center()
                                .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                                .children(() -> textField(jsonInput)
                                        .growX()
                                        .style(WebStyles.clearInput())
                                        .placeholder("{\"name\": \"...\", \"mods\": [...]}"));
                        button(() -> {
                            String clip = Core.app.getClipboardText();
                            if (clip != null) {
                                jsonInput.set(clip.trim());
                            }
                        })
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .children(() -> icon(Icon.paste).size(unit(6f)));
                    });

                    row().growX().gap(unit(2)).children(() -> {
                        button(Core.bundle.get("mod-browser.favorites.import.file"), () -> {
                            FileChooser.open("json")
                                    .submit(file -> {
                                        if (file == null) {
                                            return;
                                        }
                                        String content = new String(file.readBytes(), StandardCharsets.UTF_8);
                                        dlg.hide();
                                        handleImportedFolderJson(content);
                                    });
                        })
                                .style(WebStyles.secondaryText())
                                .growX()
                                .height(unit(11));

                        button(Core.bundle.get("mod-browser.favorites.import.clipboard"), () -> {
                            String raw = jsonInput.peek();
                            dlg.hide();
                            handleImportedFolderJson(raw);
                        })
                                .style(WebStyles.primaryText())
                                .growX()
                                .height(unit(11));
                    });
                });
            });
            dlg.show();
        }

        private void handleImportedFolderJson(String rawJson) {
            try {
                FavoriteFolder imported = LocalFavoritesStore.parseFolderJson(rawJson);
                LocalFavoritesStore store = LocalFavoritesStore.getInstance();
                if (!store.hasFolder(imported.getName())) {
                    store.importFolder(imported, ConflictResolution.KEEP_BOTH);
                    return;
                }
                showConflictResolutionDialog(imported);
            } catch (Exception e) {
                if (Vars.ui != null) {
                    Vars.ui.showException(e);
                }
            }
        }

        private void showConflictResolutionDialog(FavoriteFolder imported) {
            SolimDialog dlg = new SolimDialog(Core.bundle.get("mod-browser.favorites.conflict.title"))
                    .addCloseButton()
                    .closeOnBack()
                    .maxWidth(460f);

            dlg.children(() -> {
                column().grow().center().gap(unit(2)).padding(unit(2.5f)).children(() -> {
                    text(Core.bundle.format("mod-browser.favorites.conflict.desc", imported.getName()))
                            .color(WebStyles.Colors.GHOST_FG)
                            .wrap();

                    button(Core.bundle.get("mod-browser.favorites.conflict.keep-both"), () -> {
                        LocalFavoritesStore.getInstance().importFolder(imported, ConflictResolution.KEEP_BOTH);
                        dlg.hide();
                    }).style(WebStyles.secondaryText()).growX().height(unit(11));

                    button(Core.bundle.get("mod-browser.favorites.conflict.merge"), () -> {
                        LocalFavoritesStore.getInstance().importFolder(imported, ConflictResolution.MERGE);
                        dlg.hide();
                    }).style(WebStyles.secondaryText()).growX().height(unit(11));

                    button(Core.bundle.get("mod-browser.favorites.conflict.replace"), () -> {
                        LocalFavoritesStore.getInstance().importFolder(imported, ConflictResolution.REPLACE);
                        dlg.hide();
                    }).style(WebStyles.dangerText()).growX().height(unit(11));
                });
            });
            dlg.show();
        }

        private static boolean matchesSearch(ModListing item, String query) {
            return query.isEmpty()
                    || containsIgnoreCase(item.getName(), query)
                    || containsIgnoreCase(item.getInternalName(), query)
                    || containsIgnoreCase(item.getAuthor(), query)
                    || containsIgnoreCase(item.getDescription(), query);
        }

        private static boolean containsIgnoreCase(String value, String query) {
            return value != null && value.toLowerCase(Locale.ROOT).contains(query);
        }

        private static String safeStr(String value) {
            return value != null ? value : "";
        }
    }
}
