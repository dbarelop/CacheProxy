package cache_proxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import database.DatabaseHelper;
import org.apache.http.protocol.HTTP;

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

    public static void main(String[] args) {
        try {
            HttpServer server;
            if (args.length > 0) {
                int port = Integer.parseInt(args[0]);
                server = HttpServer.create(new InetSocketAddress(port), 0);
            } else {
                server = HttpServer.create(new InetSocketAddress(LISTEN_PORT), 0);
            }
            server.createContext("/", new ProxyResourceHandler());
            server.setExecutor(null);
            server.start();
            logger.log(Level.INFO, "Listening on port " + LISTEN_PORT);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }
}
