package cache_proxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.DatabaseHelper;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dbarelop on 18/04/15.
 */
public class ProxyResourceHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(ProxyResourceHandler.class.getName());
    private static final String CACHE_DIR = "cache/";
    private static final String TEMP_DIR = "temp/";
    private static final String HOSTS_FILE = "allowed_hosts.txt";
    private static final Random rnd = new Random();
    static {
        new File(CACHE_DIR).mkdir();
        new File(TEMP_DIR).mkdir();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        URL url = new URL(httpExchange.getRequestURI().toString().substring(1));
        if (isAllowed(url.getHost())) {
            File responseFile = null;
            if (DatabaseHelper.contains(url.toString())) {
                logger.log(Level.INFO, "File " + url.toString() + " FOUND in cache!");
                Resource r = DatabaseHelper.getResource(url.toString());
                responseFile = new File(CACHE_DIR + r.getFilename());
            } else {
                logger.log(Level.INFO, "File " + url.toString() + " NOT found in cache. Fetching...");
                File tmpFile = new File(TEMP_DIR + Long.toString(rnd.nextLong()));
                try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                     FileOutputStream fos = new FileOutputStream(tmpFile)) {
                    // Reads remote file into tmpFile
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                    // Stores the fetched resource
                    Resource storedResource = ResourceManager.storeResource(url, tmpFile);
                    responseFile = new File(ResourceManager.CACHE_DIR + storedResource.getFilename());
                }
            }

            // Sends the file back to the client
            httpExchange.sendResponseHeaders(200, responseFile.length());
            try (WritableByteChannel wbc = Channels.newChannel(httpExchange.getResponseBody());
                 FileInputStream fis = new FileInputStream(responseFile)) {
                fis.getChannel().transferTo(0, Long.MAX_VALUE, wbc);
                logger.log(Level.FINE, "File " + responseFile.toString() + " sent succesfully");
            }
        } else {
            httpExchange.sendResponseHeaders(403, -1);
            logger.log(Level.INFO, "Access to " + url.toString() + " forbidden");
        }
    }

    private String getFileHash(String algorithm, File f) throws NoSuchAlgorithmException, IOException {
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

    private boolean isAllowed(String host) {
        try (Scanner sc = new Scanner(new File(HOSTS_FILE))) {
            boolean allowed = false;
            while (sc.hasNextLine() && !allowed) {
                String h = sc.nextLine();
                if (!h.equals("")) {
                    allowed = host.contains(h);
                }
            }
            return allowed;
        } catch (FileNotFoundException e) {
            return true;
        }
    }
}
