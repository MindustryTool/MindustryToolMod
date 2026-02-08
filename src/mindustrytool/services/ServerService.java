package mindustrytool.services;

import arc.Core;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.Reflect;
import lombok.Data;
import mindustry.Vars;
import mindustry.ui.dialogs.JoinDialog.Server;
import mindustrytool.Config;
import mindustrytool.Utils;

public class ServerService {

    @Data
    private static class ServerDto {
        private String id, name, address;
        private int port, status;
    }

    public static void init() {
        fetchServers();
        Vars.ui.join.shown(() -> fetchServers());
    }

    @SuppressWarnings("unchecked")
    public static void fetchServers() {
        try {
            Seq<Server> servers = Core.settings.getJson("servers", Seq.class, Server.class, Seq::new);

            Http.get(Config.API_v4_URL + "servers?page=0&size=100")
                    .error(Log::err)
                    .submit(res -> {
                        try {
                            var serverDtos = Utils.fromJsonArray(ServerDto.class, res.getResultAsString());

                            servers.removeAll(server -> server.ip == null || server.ip.contains("mindustry-tool"));
                            serverDtos.stream().filter(server -> server.status == 0 || server.status == 1)
                                    .forEach(serverDto -> {
                                        var server = new Server();
                                        server.ip = serverDto.getAddress().replace("http://", "").replace("https://",
                                                "");
                                        server.port = serverDto.getPort();
                                        servers.add(server);
                                    });

                            Core.settings.putJson("servers", Server.class, servers);

                            if (Vars.ui.join != null) {
                                Core.app.post(() -> {
                                    Reflect.invoke(Vars.ui.join, "setupRemote");
                                    Reflect.invoke(Vars.ui.join, "refreshRemote");
                                });
                            }
                        } catch (Exception e) {
                            Log.err("Failed to parse server list", e);
                        }
                    });
        } catch (Exception e) {
            Log.err("Failed to fetch server list", e);
        }
    }
}
