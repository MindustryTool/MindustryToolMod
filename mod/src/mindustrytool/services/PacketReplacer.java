package mindustrytool.services;

import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.net.Packet;

public class PacketReplacer {
	private static ObjectMap<Class<?>, Prov<? extends Packet>> packetReplacements = new ObjectMap<>();

	public static void register(Class<?> clazz, Prov<? extends Packet> prov) {
		packetReplacements.put(clazz, prov);
	}

	public static void replace() {
		Seq<Prov<? extends Packet>> packetProvs = Reflect.get(Vars.net, "packetProvs");

		packetProvs.replace(packet -> {
			Class<?> clazz = packet.get().getClass();
			if (packetReplacements.containsKey(clazz)) {
				Log.info(
						"Replace packet @ to @",
						clazz.getSimpleName(),
						packetReplacements.get(clazz).get().getClass().getSimpleName());
				return packetReplacements.remove(clazz);
			}

			return packet;
		});

		for (Class<?> clazz : packetReplacements.keys()) {
			Log.info("Packet @ not found", clazz.getSimpleName());
		}
	}
}
