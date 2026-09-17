package mindustrytool.features.chat.state;

import arc.util.Nullable;
import mindustrytool.features.chat.ChatMessageParser;
import mindustrytool.services.auth.MindustryAuthProvider;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class ChatSession {

    public static final String MENU_STATE = "menu";
    public static final String SERVER_PREFIX = "server: ";
    public static final String PLAYER_CONNECT_PREFIX = "player-connect: ";
    public static final String CAMPAIGN_PREFIX = "campaign: ";
    public static final String EDITOR_STATE = "editing: ";
    public static final String CUSTOM_GAME_STATE = "custom-game";

    private final Computed<Boolean> loggedIn = new Computed<>(() ->
            MindustryAuthProvider.getInstance().session().get() != null);
    private final Signal<Boolean> connected = Signal.of(false);
    private final Signal<String> presence = Signal.of(MENU_STATE);
    private final Signal<String> lastNonMenu = Signal.of(MENU_STATE);
    private final Computed<String> username = MindustryAuthProvider.getInstance().session()
            .map(session -> session != null ? session.getName() : null);

    public ChatSession() {
        username.subscribe(name -> ChatMessageParser.clearCache());
        username.peek();
    }

    public Computed<Boolean> loggedIn() {
        return loggedIn;
    }

    public boolean isLoggedIn() {
        return Boolean.TRUE.equals(loggedIn.peek());
    }

    public Signal<Boolean> connected() {
        return connected;
    }

    public boolean isConnected() {
        return Boolean.TRUE.equals(connected.peek());
    }

    public void setConnected(boolean isConnected) {
        connected.set(isConnected);
    }

    public Readable<String> presence() {
        return presence;
    }

    public String currentPresence() {
        return presence.peek();
    }

    public void setPresence(String value) {
        presence.set(value != null ? value : MENU_STATE);
    }

    public Readable<String> lastNonMenu() {
        return lastNonMenu;
    }

    public String currentLastNonMenu() {
        return lastNonMenu.peek();
    }

    public void setLastNonMenu(String value) {
        lastNonMenu.set(value != null ? value : MENU_STATE);
    }

    public Readable<String> username() {
        return username;
    }

    public @Nullable String currentUsername() {
        return username.peek();
    }
}
