package cache_proxy;

import database.DatabaseHelper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dbarelop on 18/04/15.
 */
public class ResourceManager {
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());
    public static final String CACHE_DIR = "cache/";
    private static final String TEMP_DIR = "temp/";
    private static final Random rnd = new Random();

    public static void updateResource(Resource r) {
        try {
            // Decodes the resource URL from the key
            URL url = new URL(new String(Hex.decodeHex(r.get_id().toCharArray()), "UTF-8"));
            File tmpFile = new File(TEMP_DIR + Long.toString(rnd.nextLong()));
            try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                 FileOutputStream fos = new FileOutputStream(tmpFile)) {
                // Reads remote file into tmpFile
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                // Stores tmpFile in CACHE_DIR with SHA-256 hash as filename
                String hash = getFileHash("SHA-256", tmpFile);
                File storedFile = new File(CACHE_DIR + hash);
                Files.move(tmpFile.toPath(), storedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Updates the resource in the database
                Resource updatedResource = new Resource(url.toString(), r.get_rev(), hash);
                DatabaseHelper.updateResource(updatedResource);

                // Deletes outdated cached file
                if (!updatedResource.getFilename().equals(r.getFilename())) {
                    Files.delete(new File(CACHE_DIR + r.getFilename()).toPath());
                }
            }
        } catch (NoSuchAlgorithmException | DecoderException | IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }

    public static Resource storeResource(URL url, File tmpFile) {
        try {
            // Stores tmpFile in CACHE_DIR with SHA-256 hash as filename
            String hash = getFileHash("SHA-256", tmpFile);
            File storedFile = new File(CACHE_DIR + hash);
            Files.move(tmpFile.toPath(), storedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Stores the pair (url, hash) in the database
            Resource r = new Resource(url.toString(), hash);
            DatabaseHelper.storeResource(r);

            return r;
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
            return null;
        }
    }

    private static String getFileHash(String algorithm, File f) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        FileInputStream fis = new FileInputStream(f);
        byte[] dataBytes = new byte[1024];
        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0 ,nread);
        }
        byte[] mdbytes = md.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toHexString(0xFF & mdbytes[i]));
        }
        return sb.toString();
    }
}
