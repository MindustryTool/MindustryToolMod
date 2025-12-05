package mindustrytool.gui;

import java.security.InvalidParameterException;

import arc.Core;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.config.Config;
import mindustrytool.data.MapDetailData;
import arc.scene.ui.Label; // Import cho Label
import arc.scene.ui.ScrollPane; // Import cho ScrollPane
import mindustry.gen.Tex; // Import cho Tex để dùng Tex.pane

public class MapInfoDialog extends BaseDialog {
    // Không cần khai báo 'desc' là TextField nữa, sử dụng Label để giữ nội dung
    private Label descLabel;
    private ScrollPane descScrollPane; 

    public MapInfoDialog() {
        super("");
        
        // Khởi tạo Label
        descLabel = new Label("");
        descLabel.setWrap(true); // Quan trọng để văn bản cuộn theo chiều ngang

        // Gói Label trong ScrollPane
        descScrollPane = new ScrollPane(descLabel);
        descScrollPane.setFadeScrollBars(true); // Tùy chọn: làm thanh cuộn mờ đi khi không dùng

        setFillParent(true);
        addCloseListener();
    }

    public void show(MapDetailData data) {
        if (data == null) {
            throw new InvalidParameterException("Map can not be null");
        }
        cont.clear();

        title.setText("[[" + Core.bundle.get("map") + "] " + data.name());
        cont.add(new ImageHandler(data.id(), ImageHandler.ImageType.MAP)).row();
        
        cont.table(card -> {
            card.center();
            card.add(Core.bundle.format("message.author")).marginRight(4).padRight(4);
            UserCard.draw(card, data.createdBy());
        }).fillX().left().minWidth(Core.graphics.getHeight() * 2 / 3);
        cont.row();
        
        // SỬA LỖI: Ép kiểu sang long (DetailStats thường dùng long)
        cont.table(stats -> DetailStats.draw(stats, (long)data.likes(), (long)data.comments(), (long)data.downloads()))
                .fillX()
                .center();
        cont.row();
        
        cont.table(container -> TagContainer.draw(container, data.tags()))
                .fillX()
                .left()
                .row();

        cont.row();
        
        // Cập nhật nội dung cho Label
        descLabel.setText(data.description());
        
        // 1. Thay thế TextField bằng Table (Pane) chứa Label cuộn được
        cont.table(Tex.pane, descContainer -> { // Sử dụng Tex.pane để tạo viền
            descContainer.add(descScrollPane).grow().pad(4); // Thêm ScrollPane vào container
        })
            .height(Core.graphics.getHeight() / 4f) // Đặt chiều cao hiển thị cho pane
            .fillX()
            .left() 
            .row();
        
        buttons.clearChildren();
        buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
        buttons.button("@back", Icon.left, this::hide).pad(4);
        buttons.button("@open", Icon.link, () -> Core.app.openURI(Config.WEB_URL + "/maps/" + data.id()));

        show();
    }
}