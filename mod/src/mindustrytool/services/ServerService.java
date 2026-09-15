package mindustrytool.services;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.ui.dialogs.JoinDialog.Server;
import mindustrytool.models.response.ServerData;

public class ServerService {

	private static final ServerService instance = new ServerService();

	public static ServerService getInstance() {
		return instance;
	}

	private ServerService() {}

	public void init() {
		fetchServers();

		if (Vars.ui.join != null) {
			Vars.ui.join.shown(this::fetchServers);
		}

		Timer.schedule(this::fetchServers, 15 * 60, 15 * 60);
	}

	@SuppressWarnings("unchecked")
	public void fetchServers() {
		try {
			Seq<Server> servers = Core.settings.getJson("servers", Seq.class, Server.class, Seq::new);

			MindustryTool.getServers(0, 100)
					.thenAccept(serverDtos -> {
						try {
							if (serverDtos == null) {
								return;
							}

							servers.removeAll(server -> server.ip == null || server.ip.contains("mindustry-tool"));

							for (ServerData dto : serverDtos) {
								if (dto.getStatus() != 0 && dto.getStatus() != 1) {
									continue;
								}
								String address = dto.getAddress();
								if (address == null || address.isEmpty()) {
									continue;
								}
								var server = new Server();
								server.ip = address.replace("http://", "").replace("https://", "");
								server.port = dto.getPort();
								servers.add(server);
							}

							Core.settings.putJson("servers", Server.class, servers);

							if (Vars.ui.join != null) {
								Core.app.post(() -> {
									try {
										Reflect.invoke(Vars.ui.join, "setupRemote");
										Reflect.invoke(Vars.ui.join, "refreshRemote");
									} catch (Exception e) {
										Log.err("Failed to refresh join dialog", e);
									}
								});
							}
						} catch (Exception e) {
							Log.err("Failed to parse server list", e);
						}
					})
					.exceptionally(err -> {
						Log.err("Failed to fetch server list", err);
						return null;
					});
		} catch (Exception e) {
			Log.err("Failed to fetch server list", e);
		}
	}
}
