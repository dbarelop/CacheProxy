package database;

import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Hex;
import org.lightcouch.CouchDbClient;

/**
 * Created by dbarelop on 16/04/15.
 */
public class DatabaseHelper {
    private static CouchDbClient db = new CouchDbClient("couchdb.properties");
    private static final long CACHE_EXPIRAL_TIME = 172800000;    // 1 day

    public static void storeFile(String url, String f) {
        JsonObject document = new JsonObject();
        document.addProperty("_id", Hex.encodeHexString(url.getBytes()));
        document.addProperty("filename", f);
        document.addProperty("timestamp", System.currentTimeMillis());
        db.save(document);
    }

    public static String getFilename(String url) {
        JsonObject obj = db.find(JsonObject.class, Hex.encodeHexString(url.getBytes()));
        return obj.get("filename").getAsString();
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
}
