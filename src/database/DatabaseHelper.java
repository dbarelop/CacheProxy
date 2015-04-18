package database;

import cache_proxy.Resource;
import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Hex;
import org.lightcouch.CouchDbClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dbarelop on 16/04/15.
 */
public class DatabaseHelper {
    private static CouchDbClient db = new CouchDbClient("couchdb.properties");
    private static final long CACHE_EXPIRAL_TIME = 7*24*60*60*1000;    // 1 week

    public static void storeResource(Resource r) {
        db.save(r.toJson());
    }

    public static void updateResource(Resource r) {
        JsonObject obj = r.toJson();
        db.update(r.toJson());
    }

    public static Resource getResource(String url) {
        JsonObject obj = db.find(JsonObject.class, Hex.encodeHexString(url.getBytes()));
        return new Resource(obj);
    }

    public static boolean contains(String url) {
        if (db.contains(Hex.encodeHexString(url.getBytes()))) {
            JsonObject obj = db.find(JsonObject.class, Hex.encodeHexString(url.getBytes()));
            if (System.currentTimeMillis() - obj.get("timestamp").getAsLong() < CACHE_EXPIRAL_TIME) {
                return true;
            } else {
                // Cached file has expired
                db.remove(obj);
                return false;
            }
        } else {
            return false;
        }
    }

    public static List<Resource> getResources() {
        List<Resource> result = new ArrayList<>();
        for (JsonObject obj : db.view("_all_docs").includeDocs(true).query(JsonObject.class)) {
            Resource r = new Resource(obj);
            result.add(r);
        }
        return result;
    }
}
