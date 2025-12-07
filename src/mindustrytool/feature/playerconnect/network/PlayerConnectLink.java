package mindustrytool.feature.playerconnect.network;

import java.net.URI;
import java.net.URISyntaxException;

public class PlayerConnectLink {
    public static final String UriScheme = "player-connect";
    public final URI uri;
    public final String host;
    public final int port;
    public final String roomId;

    public PlayerConnectLink(String host, int port, String roomId) {
        if (host == null || host.isEmpty()) throw new IllegalArgumentException("Missing host");
        if (port == -1) throw new IllegalArgumentException("Missing port");
        if (roomId != null && roomId.startsWith("/")) roomId = roomId.substring(1);
        if (roomId == null || roomId.isEmpty()) throw new IllegalArgumentException("Missing room id");
        this.host = host; this.port = port; this.roomId = roomId;
        try { uri = new URI(UriScheme, null, host, port, '/' + roomId, null, null); }
        catch (URISyntaxException e) { throw new IllegalArgumentException("Invalid host"); }
    }

    @Override public String toString() { return uri.toString(); }

    public static PlayerConnectLink fromString(String link) {
        if (link.startsWith(UriScheme) && (!link.startsWith(UriScheme + "://") || link.length() == (UriScheme + "://").length()))
            throw new IllegalArgumentException("Missing host");
        URI uri;
        try { uri = URI.create(link); }
        catch (IllegalArgumentException e) {
            String cause = e.getLocalizedMessage();
            int semicolon = cause.indexOf(':');
            if (semicolon == -1) throw e;
            else throw new IllegalArgumentException(cause.substring(0, semicolon), e);
        }
        if (uri.isAbsolute() && !uri.getScheme().equals(UriScheme))
            throw new IllegalArgumentException("Not a player-connect link: " + link);
        return new PlayerConnectLink(uri.getHost(), uri.getPort(), uri.getPath());
    }
}
