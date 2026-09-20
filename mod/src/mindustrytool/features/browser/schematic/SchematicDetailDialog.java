package mindustrytool.features.browser.schematic;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Scaling;
import arc.util.Nullable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustrytool.Config;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserLayout;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.components.WebStyles;
import mindustrytool.models.response.SchematicDetailData;
import mindustrytool.models.response.SchematicDetailData.SchematicRequirement;
import mindustrytool.services.MindustryTool;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Query;
import solim.reactive.QueryKey;

/**
 * Detail dialog showing a high-resolution preview, author, stats, tags, item
 * requirements, description, and copy/save actions. Stacks vertically in
 * portrait and side-by-side in landscape.
 */
public class SchematicDetailDialog extends SolimDialog {

    public SchematicDetailDialog(SchematicDetailData detail, String itemId) {
        super(detail.getName() != null ? detail.getName() : Core.bundle.get("browser.schematic.unnamed"));

        addCloseButton();
        closeOnBack();
        fillParent(true);
        children(() -> new DetailContent(detail, itemId));
        actionButton(Core.bundle.get("browser.detail.open-online"), Icon.link,
                () -> Core.app.openURI(Config.WEB_URL + "/schematics/" + itemId));
    }

    private static class DetailContent extends BaseComponent {
        private final SchematicDetailData detail;
        private final String itemId;
        private final @Nullable String authorId;
        private final @Nullable Query<String> authorQuery;

        DetailContent(SchematicDetailData detail, String itemId) {
            this.detail = detail;
            this.itemId = itemId;
            this.authorId = detail.getCreatedBy();
            this.authorQuery = authorId != null && !authorId.isEmpty()
                    ? Query.of(QueryKey.of("user", authorId), () -> MindustryTool.getUserBatch(Collections.singletonList(authorId))
                            .thenApply(users -> users != null && !users.isEmpty() && users.get(0) != null && users.get(0).getName() != null
                                    ? users.get(0).getName()
                                    : authorId)).staleTime(Duration.ofMinutes(10))
                    : null;
        }

        @Override
        protected Element build() {
            return column().grow().padding(unit(2)).gap(unit(2)).children(() -> {
                dynamic(isPortrait(), portrait -> {
                    if (Boolean.TRUE.equals(portrait)) {
                        return portraitLayout();
                    }
                    return landscapeLayout();
                }).grow();
            }).element();
        }

        private Component portraitLayout() {
            return scroll().grow().children(() -> {
                column().grow().top().left().gap(unit(4)).children(() -> {
                    previewImagePortrait();
                    details();
                });
            });
        }

        private Component landscapeLayout() {
            return scroll().grow().children(() -> {
                row().grow().center().top().gap(unit(4)).children(() -> {
                    previewImageLandscape();
                    details();
                });
            });
        }

        private void previewImagePortrait() {
            row().grow().children(() -> {
                networkImage(BrowserImages.schematicImageUrl(itemId))
                        .placeholder(Icon.image)
                        .fallback(Icon.image)
                        .origin(Align.top | Align.left)
                        .grow()
                        .top()
                        .scaling(Scaling.fit);
            });
        }

        private void previewImageLandscape() {
            row().grow().children(() -> {
                networkImage(BrowserImages.schematicImageUrl(itemId))
                        .origin(Align.top)
                        .placeholder(Icon.image)
                        .fallback(Icon.image)
                        .grow()
                        .top()
                        .scaling(Scaling.fit);
            });
        }

        private void details() {
            column().grow().gap(unit(2)).children(() -> {
                card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                    row().growX().gap(unit(1)).children(() -> {
                        text(Core.bundle.get("browser.detail.author")).color(Color.lightGray).fontScale(1.3f);
                        if (authorQuery != null) {
                            query(authorQuery)
                            .growX()
                                    .loading(() -> text(authorId != null ? authorId : "").color(Color.white).fontScale(1.3f))
                                    .error(err -> text(authorId != null ? authorId : "").color(Color.white).fontScale(1.3f))
                                    .data(name -> text(name).color(Color.white).fontScale(1.3f));
                        } else {
                            text(authorId != null ? authorId : "").color(Color.white).fontScale(1.3f);
                        }
                    });

                    row().growX().gap(unit(1)).children(() -> {
                        text(Core.bundle.get("browser.detail.dimensions")).color(Color.lightGray);
                        text(detail.getWidth() + "x" + detail.getHeight()).color(Color.white);
                    });
                });

                card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                    new BrowserStatsBadge(
                            BrowserImages.count(detail.getLikes()),
                            BrowserImages.count(detail.getComments()),
                            BrowserImages.count(detail.getDownloads()));
                });

                BrowserLayout.renderTags(detail.getTags());

                renderRequirements();

                if (detail.getDescription() != null && !detail.getDescription().isEmpty()) {
                    card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                        text(detail.getDescription()).color(Color.lightGray).wrap(true).left().growX();
                    });
                }

                divider();

                row().growX().gap(unit(2)).children(() -> {
                    button(Core.bundle.get("browser.schematic.copy"),
                            () -> SchematicActions.copyToClipboard(itemId))
                                    .style(WebStyles.primary())
                                    .growX()
                                    .height(unit(11));

                    button(Core.bundle.get("browser.schematic.save"),
                            () -> SchematicActions.saveToLocal(itemId))
                                    .style(WebStyles.primary())
                                    .growX()
                                    .height(unit(11));
                });
            });
        }

        private void renderRequirements() {
            Seq<ItemStack> requirements = toItemSeq(
                    detail.getMeta() != null ? detail.getMeta().getRequirements() : null);

            if (requirements.isEmpty()) {
                return;
            }

            card(WebStyles.previewCardBackground()).growX().top().left().padding(unit(4)).gap(unit(2)).children(() -> {
                text(Core.bundle.get("browser.detail.requirements")).color(Color.white).growX().left();
                wrap().growX().left().gap(unit(2)).children(() -> {
                    for (ItemStack stack : requirements) {
                        row().center().gap(unit(1)).left().children(() -> {
                            image(new TextureRegionDrawable(stack.item.uiIcon)).size(unit(6));
                            text(requirementLabel(stack));
                        });
                    }
                });
            });
        }

        private String requirementLabel(ItemStack stack) {
            Building core = Vars.player != null ? Vars.player.core() : null;
            if (core == null || Vars.state.isMenu() || Vars.state.rules.infiniteResources
                    || core.items.has(stack.item, stack.amount)) {
                return "[lightgray]" + stack.amount;
            }
            return "[scarlet]" + Math.min(core.items.get(stack.item), stack.amount)
                    + "[lightgray]/" + stack.amount;
        }

        private static Seq<ItemStack> toItemSeq(List<SchematicRequirement> requirements) {
            Seq<ItemStack> seq = new Seq<ItemStack>();
            if (requirements == null) {
                return seq;
            }

            for (SchematicRequirement requirement : requirements) {
                if (requirement == null || requirement.getName() == null
                        || requirement.getAmount() == null) {
                    continue;
                }
                Item item = Vars.content.items().find(
                        found -> found.name.equalsIgnoreCase(requirement.getName()));
                if (item != null) {
                    seq.add(new ItemStack(item, requirement.getAmount()));
                }
            }
            return seq;
        }
    }
}
