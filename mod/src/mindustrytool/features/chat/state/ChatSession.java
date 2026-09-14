package mindustrytool.features.chat.state;

import arc.util.Nullable;
import mindustrytool.features.chat.ChatMessageParser;
import mindustrytool.services.auth.MindustryAuthProvider;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

public final class ChatSession {

    private final Computed<Boolean> loggedIn = new Computed<>(() ->
            MindustryAuthProvider.getInstance().session().get() != null);
    private final Signal<Boolean> connected = Signal.of(false);
    private final Computed<String> username = MindustryAuthProvider.getInstance().session()
            .map(session -> session != null ? session.getName() : null);

    public ChatSession() {
        username.subscribe(name -> ChatMessageParser.clearCache());
        username.peek();
    }

    public Readable<Boolean> loggedIn() {
        return loggedIn;
    }

    public boolean isLoggedIn() {
        return Boolean.TRUE.equals(loggedIn.peek());
    }

    public Readable<Boolean> connected() {
        return connected;
    }

    public boolean isConnected() {
        return Boolean.TRUE.equals(connected.peek());
    }

    public void setConnected(boolean isConnected) {
        connected.set(isConnected);
    }

    public Readable<String> username() {
        return username;
    }

    public @Nullable String currentUsername() {
        return username.peek();
    }
}
