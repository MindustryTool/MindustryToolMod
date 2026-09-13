package mindustrytool.features.browser.schematic;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Scaling;
import java.util.Collections;
import java.util.List;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.ui.Styles;
import mindustrytool.Config;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.models.response.SchematicDetailData;
import mindustrytool.models.response.SchematicDetailData.SchematicRequirement;
import mindustrytool.models.response.TagData;
import mindustrytool.services.MindustryTool;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.signal.Signal;

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
        private final Signal<String> authorName;

        DetailContent(SchematicDetailData detail, String itemId) {
            this.detail = detail;
            this.itemId = itemId;
            this.authorName = Signal.of(detail.getCreatedBy() != null ? detail.getCreatedBy() : "");
            resolveAuthorName(detail.getCreatedBy());
        }

        @Override
        protected Element build() {
            return column().grow().padding(unit(2)).gap(unit(2)).children(() -> {
                dynamic(isPortrait(), portrait -> {
                    if (Boolean.TRUE.equals(portrait)) {
                        return portraitLayout();
                    }
                    return landscapeLayout();
                });
            }).element();
        }

        private Component portraitLayout() {
            return column().grow().gap(unit(2)).children(() -> {
                previewImagePortrait();
                scroll().grow().children(() -> details());
            });
        }

        private Component landscapeLayout() {
            return row().grow().gap(unit(4)).children(() -> {
                previewImageLandscape();
                scroll().grow().children(() -> details());
            });
        }

        private void previewImagePortrait() {
            networkImage(BrowserImages.schematicImageUrl(itemId))
                    .placeholder(Icon.image)
                    .fallback(Icon.image)
                    .growX()
                    .height(dvh(45f))
                    .rounded(8)
                    .scaling(Scaling.fit);
        }

        private void previewImageLandscape() {
            networkImage(BrowserImages.schematicImageUrl(itemId))
                    .placeholder(Icon.image)
                    .fallback(Icon.image)
                    .width(dvw(55f))
                    .growY()
                    .rounded(8)
                    .scaling(Scaling.fit);
        }

        private void details() {
            column().growX().gap(unit(2)).children(() -> {
                row().growX().gap(unit(1)).children(() -> {
                    text(Core.bundle.get("browser.detail.author")).color(Color.lightGray).fontScale(0.9f);
                    text(authorName).color(Color.white).fontScale(0.9f);
                });

                row().growX().gap(unit(1)).children(() -> {
                    text(Core.bundle.get("browser.detail.dimensions")).color(Color.lightGray).fontScale(0.9f);
                    text(detail.getWidth() + "x" + detail.getHeight()).color(Color.white).fontScale(0.9f);
                });

                new BrowserStatsBadge(
                        BrowserImages.count(detail.getLikes()),
                        BrowserImages.count(detail.getComments()),
                        BrowserImages.count(detail.getDownloads()));

                renderTags();
                renderRequirements();

                if (detail.getDescription() != null && !detail.getDescription().isEmpty()) {
                    text(detail.getDescription()).color(Color.lightGray).wrap(true).left().growX();
                }

                spacer();

                row().growX().gap(unit(2)).children(() -> {
                    button(Core.bundle.get("browser.schematic.copy"),
                            () -> SchematicActions.copyToClipboard(itemId))
                            .style(Styles.defaultb)
                            .growX()
                            .height(unit(10));

                    button(Core.bundle.get("browser.schematic.save"),
                            () -> SchematicActions.saveToLocal(itemId))
                            .style(Styles.defaultb)
                            .growX()
                            .height(unit(10));
                });
            });
        }

        private void renderTags() {
            List<TagData> tags = detail.getTags();
            if (tags == null || tags.isEmpty()) {
                return;
            }
            text(Core.bundle.get("browser.detail.tags")).color(Color.white).left();
            grid(isPortrait().map(p -> Boolean.TRUE.equals(p) ? 2 : 4)).growX().gap(unit(1)).children(() -> {
                for (TagData tag : tags) {
                    if (tag == null || tag.getName() == null) {
                        continue;
                    }
                    text(tag.getName())
                            .style(Styles.defaultLabel)
                            .color(tag.color())
                            .fontScale(0.85f);
                }
            });
        }

        private void renderRequirements() {
            Seq<ItemStack> requirements = toItemSeq(
                    detail.getMeta() != null ? detail.getMeta().getRequirements() : null);
            if (requirements.isEmpty()) {
                return;
            }
            text(Core.bundle.get("browser.detail.requirements")).color(Color.white).left();
            grid(isPortrait().map(p -> Boolean.TRUE.equals(p) ? 2 : 4)).growX().gap(unit(1)).children(() -> {
                for (ItemStack stack : requirements) {
                    row().gap(unit(1)).left().children(() -> {
                        image(new TextureRegionDrawable(stack.item.uiIcon)).size(unit(8));
                        text(requirementLabel(stack)).fontScale(0.9f);
                    });
                }
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

        private void resolveAuthorName(String createdBy) {
            if (createdBy == null || createdBy.isEmpty()) {
                return;
            }
            MindustryTool.getUserBatch(Collections.singletonList(createdBy))
                    .whenComplete((users, throwable) -> {
                        if (throwable != null || users == null || users.isEmpty()) {
                            return;
                        }
                        Core.app.post(() -> {
                            if (!isDisposed() && users.get(0) != null && users.get(0).getName() != null) {
                                authorName.set(users.get(0).getName());
                            }
                        });
                    });
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
