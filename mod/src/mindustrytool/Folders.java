package mindustrytool;

import arc.files.Fi;
import mindustry.Vars;

public class Folders {
	public static Fi baseDir = Vars.dataDirectory.child("mindustry-tool");
	public static Fi imageDir = baseDir.child("caches");
	public static Fi mapsDir = baseDir.child("maps");
	public static Fi schematicDir = baseDir.child("schematics");
	public static Fi backgroundsDir = baseDir.child("backgrounds");
	public static Fi musicsDir = baseDir.child("musics");

	static {
		imageDir.mkdirs();
		mapsDir.mkdirs();
		schematicDir.mkdirs();
		backgroundsDir.mkdirs();
		musicsDir.mkdirs();
	}
}
