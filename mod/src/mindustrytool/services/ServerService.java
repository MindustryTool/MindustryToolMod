package mindustrytool.services;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;
import mindustrytool.utils.ReflectUtil;
import java.time.Duration;
import java.util.List;
import mindustry.Vars;
import mindustry.ui.dialogs.JoinDialog.Server;
import mindustrytool.models.response.ServerData;
import solim.reactive.Effect;
import solim.reactive.Query;
import solim.reactive.QueryKey;

public class ServerService {

	private static final ServerService instance = new ServerService();

	public static ServerService getInstance() {
		return instance;
	}

	private Query<List<ServerData>> serverQuery;

	private ServerService() {}

	public void init() {
        serverQuery = Query.<List<ServerData>>builder()
                .key(QueryKey.of("servers"))
                .fetch(() -> MindustryTool.getServers(0, 100))
                .refetchInterval(Duration.ofMinutes(15))
                .build();

		Effect.of(() -> {
			List<ServerData> serverDtos = serverQuery.data().get();
			if (serverDtos != null) {
				updateServers(serverDtos);
			}
		});

		if (Vars.ui.join != null) {
			Vars.ui.join.shown(this::fetchServers);
		}
	}

	public void fetchServers() {
		if (serverQuery != null) {
			serverQuery.refetch();
		}
	}

	@SuppressWarnings("unchecked")
	private void updateServers(List<ServerData> serverDtos) {
		try {
			Seq<Server> servers = Core.settings.getJson("servers", Seq.class, Server.class, Seq::new);
			servers.removeAll(server -> server.ip == null || server.ip.contains("mindustry-tool"));

			for (ServerData dto : serverDtos) {
				if (dto.getStatus() != 0 && dto.getStatus() != 1) {
					continue;
				}
				String address = dto.getAddress();
				if (address == null || address.isEmpty()) {
					continue;
				}
				Server server = new Server();
				server.ip = address.replace("http://", "").replace("https://", "");
				server.port = dto.getPort();
				servers.add(server);
			}

			Core.settings.putJson("servers", Server.class, servers);

			if (Vars.ui.join != null) {
				Core.app.post(() -> {
					try {
						ReflectUtil.invokeOrNull(Vars.ui.join, "setupRemote", new Object[]{});
						ReflectUtil.invokeOrNull(Vars.ui.join, "refreshRemote", new Object[]{});
					} catch (Exception e) {
						Log.err("Failed to refresh join dialog", e);
					}
				});
			}
		} catch (Exception e) {
			Log.err("Failed to parse server list", e);
		}
	}
}
