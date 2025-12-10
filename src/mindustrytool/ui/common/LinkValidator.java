package mindustrytool.ui.common;

import arc.Core;
import mindustrytool.network.PlayerConnectLink;

public class LinkValidator {
    public static Result validate(String link) {
        try {
            PlayerConnectLink.fromString(link);
            return new Result(true, "@message.join-room.valid");
        } catch (Exception e) {
            return new Result(false, Core.bundle.get("message.join-room.invalid") + ' ' + e.getLocalizedMessage());
        }
    }

    public static class Result {
        public final boolean isValid;
        public final String message;
        public Result(boolean v, String m) { isValid = v; message = m; }
    }
}
