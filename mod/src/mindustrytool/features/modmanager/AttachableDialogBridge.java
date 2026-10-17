package mindustrytool.features.modmanager;

import arc.Core;
import arc.func.Boolp;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.scene.ui.Button;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

public final class AttachableDialogBridge {

    private static final String SHARED_OWNER_KEY = "shared-attachableDialog@a17fc3bd4";
    private static final String SHARED_FIELDS_KEY = "shared-attachableDialog@a17fc3bd4-fields";

    private static final SharedRegistryHolder HOLDER = new SharedRegistryHolder();
    private static final Set<Dialog> OVERLAY_INSTALLED = Collections.newSetFromMap(new IdentityHashMap<>());

    private AttachableDialogBridge() {
    }

    public static final class SharedRegistryHolder {
        public Map<Dialog, List<Dialog>> attachedMap = new LinkedHashMap<>();
        public Map<Dialog, Map<Dialog, Boolp>> attachedEnabledMap = new LinkedHashMap<>();
        public Map<Dialog, Map<Dialog, Drawable>> attachedIconMap = new LinkedHashMap<>();
        public Map<Dialog, Map<Dialog, String>> attachedDescMap = new LinkedHashMap<>();
        public AtomicBoolean fallback = new AtomicBoolean(false);
    }

    public static final class AttachedEntry {
        public final Dialog dialog;
        public final @Nullable Drawable icon;
        public final String description;

        public AttachedEntry(Dialog dialog, @Nullable Drawable icon, String description) {
            this.dialog = dialog;
            this.icon = icon;
            this.description = description;
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized void syncSharedRegistry() {
        try {
            Properties properties = System.getProperties();
            Object owner = properties.get(SHARED_OWNER_KEY);
            Object fieldsObj = properties.get(SHARED_FIELDS_KEY);
            if (owner != null && owner != HOLDER && fieldsObj instanceof List<?>) {
                List<?> rawFields = (List<?>) fieldsObj;
                for (Object item : rawFields) {
                    if (!(item instanceof Field)) {
                        continue;
                    }
                    Field field = (Field) item;
                    field.setAccessible(true);
                    Object value = field.get(owner);
                    if (value == null) {
                        continue;
                    }
                    String fieldName = field.getName();
                    if ("attachedMap".equals(fieldName) && value instanceof Map<?, ?>) {
                        HOLDER.attachedMap = (Map<Dialog, List<Dialog>>) value;
                    } else if ("attachedEnabledMap".equals(fieldName) && value instanceof Map<?, ?>) {
                        HOLDER.attachedEnabledMap = (Map<Dialog, Map<Dialog, Boolp>>) value;
                    } else if ("attachedIconMap".equals(fieldName) && value instanceof Map<?, ?>) {
                        HOLDER.attachedIconMap = (Map<Dialog, Map<Dialog, Drawable>>) value;
                    } else if ("attachedDescMap".equals(fieldName) && value instanceof Map<?, ?>) {
                        HOLDER.attachedDescMap = (Map<Dialog, Map<Dialog, String>>) value;
                    } else if ("fallback".equals(fieldName) && value instanceof AtomicBoolean) {
                        HOLDER.fallback = (AtomicBoolean) value;
                    }
                }
            } else if (owner == null) {
                List<Field> exportedFields = new ArrayList<>();
                for (Field field : SharedRegistryHolder.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    exportedFields.add(field);
                }
                properties.put(SHARED_OWNER_KEY, HOLDER);
                properties.put(SHARED_FIELDS_KEY, exportedFields);
            }
        } catch (Throwable ignored) {
        }
    }

    public static synchronized void attach(
            @Nullable Dialog targetDialog,
            @Nullable Dialog customDialog,
            @Nullable Drawable switchIcon,
            @Nullable String switchDesc,
            @Nullable Boolp enabledProv) {
        if (targetDialog == null || customDialog == null || switchDesc == null || enabledProv == null) {
            return;
        }
        syncSharedRegistry();

        List<Dialog> dialogs = HOLDER.attachedMap.get(targetDialog);
        if (dialogs == null) {
            targetDialog.shown(() -> {
                syncSharedRegistry();
                if (HOLDER.fallback.get()) {
                    return;
                }
                Core.app.post(() -> {
                    Dialog first = showFirstAttached(targetDialog);
                    if (first != null) {
                        targetDialog.clearActions();
                        targetDialog.hide(null);
                    }
                });
            });
            dialogs = new ArrayList<>();
            HOLDER.attachedMap.put(targetDialog, dialogs);
        }

        dialogs.remove(customDialog);
        dialogs.add(0, customDialog);

        Map<Dialog, Boolp> enabledMap = HOLDER.attachedEnabledMap.get(targetDialog);
        if (enabledMap == null) {
            enabledMap = new LinkedHashMap<>();
            HOLDER.attachedEnabledMap.put(targetDialog, enabledMap);
        }
        enabledMap.put(customDialog, enabledProv);

        Map<Dialog, Drawable> iconMap = HOLDER.attachedIconMap.get(targetDialog);
        if (iconMap == null) {
            iconMap = new LinkedHashMap<>();
            HOLDER.attachedIconMap.put(targetDialog, iconMap);
        }
        iconMap.put(customDialog, switchIcon != null ? switchIcon : Icon.wrench);

        Map<Dialog, String> descMap = HOLDER.attachedDescMap.get(targetDialog);
        if (descMap == null) {
            descMap = new LinkedHashMap<>();
            HOLDER.attachedDescMap.put(targetDialog, descMap);
        }
        descMap.put(customDialog, switchDesc);

        installVanillaOverlayHook(targetDialog);
    }

    public static synchronized List<AttachedEntry> getEntries(@Nullable Dialog targetDialog) {
        if (targetDialog == null) {
            return Collections.emptyList();
        }
        syncSharedRegistry();
        List<Dialog> dialogs = HOLDER.attachedMap.get(targetDialog);
        Map<Dialog, Drawable> icons = HOLDER.attachedIconMap.get(targetDialog);
        Map<Dialog, String> descs = HOLDER.attachedDescMap.get(targetDialog);
        if (dialogs == null || dialogs.isEmpty() || icons == null || descs == null) {
            return Collections.emptyList();
        }
        List<AttachedEntry> result = new ArrayList<>(dialogs.size());
        for (Dialog dialog : dialogs) {
            if (dialog == null) {
                continue;
            }
            Drawable icon = icons.get(dialog);
            String desc = descs.get(dialog);
            if (desc != null) {
                result.add(new AttachedEntry(dialog, icon != null ? icon : Icon.wrench, desc));
            }
        }
        return result;
    }

    public static @Nullable Dialog showFirstAttached(@Nullable Dialog targetDialog) {
        if (targetDialog == null) {
            return null;
        }
        syncSharedRegistry();
        List<Dialog> dialogs = HOLDER.attachedMap.get(targetDialog);
        Map<Dialog, Boolp> enabledMap = HOLDER.attachedEnabledMap.get(targetDialog);
        if (dialogs == null || enabledMap == null) {
            return null;
        }
        for (Dialog candidate : dialogs) {
            if (candidate == null) {
                continue;
            }
            Boolp enabled = enabledMap.get(candidate);
            if (enabled != null && enabled.get()) {
                return candidate.show();
            }
        }
        return null;
    }

    public static void openFallback(@Nullable Dialog targetDialog) {
        if (targetDialog == null) {
            return;
        }
        syncSharedRegistry();
        HOLDER.fallback.set(true);
        try {
            targetDialog.show();
        } finally {
            HOLDER.fallback.set(false);
        }
    }

    public static boolean isFallbackActive() {
        syncSharedRegistry();
        return HOLDER.fallback.get();
    }

    private static void installVanillaOverlayHook(Dialog targetDialog) {
        if (!OVERLAY_INSTALLED.add(targetDialog)) {
            return;
        }
        Core.app.post(() -> {
            try {
                targetDialog.fill(overlay -> {
                    overlay.name = "mindustrytool-vanilla-mod-switcher";
                    Runnable rebuild = () -> {
                        overlay.clearChildren();
                        if (Core.graphics != null && Core.graphics.isPortrait()) {
                            return;
                        }
                        float topPad = targetDialog.titleTable != null ? targetDialog.titleTable.getPrefHeight() : 0f;
                        overlay.top().left().table(t -> {
                            t.top().left();
                            Table switcher = buildVanillaLeftSwitchTable(targetDialog);
                            t.add(switcher).top().left();
                        }).padTop(topPad).grow();
                    };
                    targetDialog.shown(rebuild);
                    targetDialog.resized(rebuild);
                    rebuild.run();
                });
            } catch (Throwable ignored) {
            }
        });
    }

    private static Table buildVanillaLeftSwitchTable(Dialog targetDialog) {
        List<AttachedEntry> entries = getEntries(targetDialog);
        boolean[] hovered = { false };
        List<Collapser> collapsers = new ArrayList<>();

        Table table = new Table();
        table.left().defaults().left().growX().fillY();

        for (AttachedEntry entry : entries) {
            addVanillaSideButton(
                    table,
                    entry.icon,
                    entry.description,
                    hovered,
                    collapsers,
                    () -> {
                        targetDialog.hide(null);
                        if (Core.scene != null) {
                            entry.dialog.show(Core.scene);
                        } else {
                            entry.dialog.show();
                        }
                    });
            table.row();
        }

        addVanillaSideButton(
                table,
                Icon.left,
                Core.bundle != null ? Core.bundle.get("back", "Back") : "Back",
                hovered,
                collapsers,
                () -> {});
        return table;
    }

    private static void addVanillaSideButton(
            Table parent,
            Drawable iconDrawable,
            String labelText,
            boolean[] hovered,
            List<Collapser> collapsers,
            Runnable onClick) {
        Button btn = parent.button(b -> {
            b.left().defaults().left();
            b.image(iconDrawable).size(46f).pad(4f);
            Table textTable = new Table();
            textTable.add(labelText).pad(4f).padRight(8f);
            Collapser collapser = new Collapser(textTable, true);
            collapser.setDuration(0.2f);
            collapsers.add(collapser);
            b.add(collapser);
        }, Styles.cleart, onClick).get();

        btn.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
                if (pointer == -1 && (fromActor == null || !fromActor.isDescendantOf(btn))) {
                    hovered[0] = true;
                    for (Collapser c : collapsers) {
                        c.setCollapsed(false, true);
                    }
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
                if (pointer == -1 && (toActor == null || !toActor.isDescendantOf(btn))) {
                    hovered[0] = false;
                    for (Collapser c : collapsers) {
                        c.setCollapsed(true, true);
                    }
                }
            }
        });
    }
}
