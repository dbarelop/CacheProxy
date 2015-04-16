package database;

import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Hex;
import org.lightcouch.CouchDbClient;
import org.lightcouch.Response;

/**
 * Created by dbarelop on 16/04/15.
 */
public class DatabaseHelper {
    private static CouchDbClient db = new CouchDbClient("couchdb.properties");

    public static void storeFile(String url, String f) {
        JsonObject document = new JsonObject();
        document.addProperty("_id", Hex.encodeHexString(url.getBytes()));
        document.addProperty("filename", f);
        db.save(document);
    }

    public static String getFilename(String url) {
        JsonObject obj = db.find(JsonObject.class, Hex.encodeHexString(url.getBytes()));
        return obj.get("filename").getAsString();
    }

    public static boolean contains(String url) {
        return db.contains(Hex.encodeHexString(url.getBytes()));
    }
}
