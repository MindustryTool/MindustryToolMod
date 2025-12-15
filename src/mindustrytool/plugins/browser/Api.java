package mindustrytool.plugins.browser;

import arc.func.*;

public class Api {
    public static void downloadSchematic(String id, ConsT<byte[], Exception> c) { 
        AuthHttp.get(Config.API_URL + "schematics/" + id + "/data").submit(r -> c.get(r.getResult())); 
    }
    
    public static void downloadMap(String id, ConsT<byte[], Exception> c) { 
        AuthHttp.get(Config.API_URL + "maps/" + id + "/data").submit(r -> c.get(r.getResult())); 
    }
    
    public static void findSchematicById(String id, Cons<SchematicDetailData> c) { 
        ApiRequest.get(Config.API_URL + "schematics/" + id, SchematicDetailData.class, c); 
    }
    
    public static void findMapById(String id, Cons<MapDetailData> c) { 
        ApiRequest.get(Config.API_URL + "maps/" + id, MapDetailData.class, c); 
    }
    
    public static void findUserById(String id, Cons<UserData> c) { 
        ApiRequest.get(Config.API_URL + "users/" + id, UserData.class, c); 
    }
}
