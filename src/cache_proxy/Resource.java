package cache_proxy;

import com.google.gson.JsonObject;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by dbarelop on 18/04/15.
 */
public class Resource {
    private String _id;
    private String _rev;
    private String filename;
    private long timestamp;

    public Resource(String _id, String filename) {
        this(_id, null, filename);
    }

    public Resource(String _id, String _rev, String filename) {
        this._id = Hex.encodeHexString(_id.getBytes());
        this._rev = _rev;
        this.filename = filename;
        this.timestamp = System.currentTimeMillis();
    }

    public Resource(JsonObject obj) {
        _id = obj.get("_id").getAsString();
        _rev = obj.get("_rev").getAsString();
        filename = obj.get("filename").getAsString();
        timestamp = obj.get("timestamp").getAsLong();
    }

    public String get_id() {
        return _id;
    }

    public String get_rev() {
        return _rev;
    }

    public String getFilename() {
        return filename;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getURL() {
        try {
            return new String(Hex.decodeHex(_id.toCharArray()), "UTF-8");
        } catch (DecoderException | UnsupportedEncodingException e) {
            return null;
        }
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("_id", _id);
        obj.addProperty("filename", filename);
        obj.addProperty("timestamp", timestamp);
        if (_rev != null) {
            obj.addProperty("_rev", _rev);
        }
        return obj;
    }

    @Override
    public String toString() {
        return getURL();
    }
}
