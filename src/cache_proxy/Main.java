package cache_proxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import database.DatabaseHelper;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final int LISTEN_PORT = 8080;
    private static final String CACHE_DIR = "cache/";
    private static final String TEMP_DIR = "temp/";
    private static final Random rnd = new Random();
    static {
        new File(CACHE_DIR).mkdir();
        new File(TEMP_DIR).mkdir();
    }

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(LISTEN_PORT), 0);
            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange httpExchange) throws IOException {
                    URL url = new URL(httpExchange.getRequestURI().toString().substring(1));
                    // Filter only openweathermap.org and *.nasa.gov petitions
                    if (url.toString().contains("nasa") || url.toString().contains("openweathermap")) {
                        File responseFile = null;
                        if (DatabaseHelper.contains(url.toString())) {
                            logger.log(Level.INFO, "File " + url.toString() + " FOUND in cache!");
                            String filename = DatabaseHelper.getFilename(url.toString());
                            responseFile = new File(CACHE_DIR + filename);
                        } else {
                            logger.log(Level.INFO, "File " + url.toString() + " NOT found in cache. Fetching...");
                            File tmpFile = new File(TEMP_DIR + Long.toString(rnd.nextLong()));
                            try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                                 FileOutputStream fos = new FileOutputStream(tmpFile)) {
                                // Reads remote file into tmpFile
                                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                                // Stores tmpFile in CACHE_DIR with SHA-256 hash as filename
                                String hash = getFileHash("SHA-256", tmpFile);
                                responseFile = new File(CACHE_DIR + hash);
                                Files.move(tmpFile.toPath(), responseFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                // Stores the pair (url, hash) in the database
                                DatabaseHelper.storeFile(url.toString(), hash);
                            } catch (NoSuchAlgorithmException e) {
                                logger.log(Level.SEVERE, e.toString(), e);
                            }
                        }

                        // Sends the file back to the client
                        httpExchange.sendResponseHeaders(200, responseFile.length());
                        try (WritableByteChannel wbc = Channels.newChannel(httpExchange.getResponseBody());
                             FileInputStream fis = new FileInputStream(responseFile)) {
                            fis.getChannel().transferTo(0, Long.MAX_VALUE, wbc);
                            logger.log(Level.INFO, "File " + responseFile.toString() + " sent succesfully");
                        }
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
            });
            server.setExecutor(null);
            server.start();
            logger.log(Level.INFO, "Listening on port " + LISTEN_PORT);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }
}
