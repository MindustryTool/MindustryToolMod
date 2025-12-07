package mindustrytool.presentation.builder;

import arc.Core;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;

import mindustrytool.core.util.*;
import mindustrytool.core.model.*;
import mindustrytool.data.api.PagingRequest;
import mindustrytool.presentation.dialog.FilterDialog;

public class BrowserSearchBar {
    public static void render(Table parent, TextField searchField, String search, 
            arc.struct.ObjectMap<String, String> options, PagingRequest<?> request,
            Debouncer debouncer, FilterDialog filterDialog, SearchConfig searchConfig,
            Runnable onSearch, Runnable hide) {
        parent.table(sb -> {
            sb.button("@back", Icon.leftSmall, hide).width(150).pad(2);
            sb.table(sbc -> {
                sbc.left();
                sbc.add(searchField).growX();
            }).fillX().expandX().padBottom(2).padLeft(2).padRight(2);
            sb.button(Icon.filterSmall, () -> loadingWrapper(request, () -> filterDialog.show(searchConfig))).pad(2).width(60);
            sb.button(Icon.zoomSmall, () -> loadingWrapper(request, onSearch)).pad(2).width(60);
        }).fillX().expandX();
    }

    private static void loadingWrapper(PagingRequest<?> request, Runnable action) {
        Core.app.post(() -> {
            if (request.isLoading()) mindustry.Vars.ui.showInfoFade("Loading");
            else action.run();
        });
    }
}
