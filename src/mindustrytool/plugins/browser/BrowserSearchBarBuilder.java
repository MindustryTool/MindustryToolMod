package mindustrytool.plugins.browser;

import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustry.gen.Icon;
import arc.struct.Seq;
import arc.func.Cons;

public class BrowserSearchBarBuilder {
    public static <T> void build(Table parent, String search, ObjectMap<String, String> options,
            PagingRequest<T> request, SearchConfig searchConfig, Debouncer debouncer,
            FilterDialog filterDialog, Cons<Seq<T>> handler, Runnable hide) {
        parent.table(sb -> {
            sb.button("@back", Icon.leftSmall, hide).width(150).pad(2);
            sb.table(sbc -> { 
                sbc.left(); 
                sbc.field(search, res -> { 
                    options.put("name", res); 
                    request.setPage(0); 
                    debouncer.debounce(() -> loadingWrapper(request, () -> request.getPage(handler))); 
                }).growX(); 
            }).fillX().expandX().padBottom(2).padLeft(2).padRight(2);
            sb.button(Icon.filterSmall, () -> loadingWrapper(request, () -> filterDialog.show(searchConfig))).pad(2).width(60);
            sb.button(Icon.zoomSmall, () -> loadingWrapper(request, () -> request.getPage(handler))).pad(2).width(60);
        }).fillX().expandX();
        parent.row();
        parent.pane(tagBar -> TagBar.draw(tagBar, searchConfig, sc -> { 
            options.put("tags", sc.getSelectedTagsString()); 
            request.setPage(0); 
            debouncer.debounce(() -> loadingWrapper(request, () -> request.getPage(handler))); 
        })).scrollY(false);
    }

    private static void loadingWrapper(PagingRequest<?> request, Runnable action) { 
        arc.Core.app.post(() -> { 
            if (request.isLoading()) mindustry.Vars.ui.showInfoFade("Loading"); 
            else action.run(); 
        }); 
    }
}
