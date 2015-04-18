package cache_proxy;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final int LISTEN_PORT = 8080;
    private static final int MAX_THREADS = 8;
    private static final long UPDATER_FREQ = 60*60*1000;    // 1 hour

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
            server.setExecutor(Executors.newFixedThreadPool(MAX_THREADS));
            server.start();
            logger.log(Level.INFO, "Listening on port " + LISTEN_PORT);
            ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutor.scheduleAtFixedRate(new ResourceUpdateManager(), 0, UPDATER_FREQ, TimeUnit.MILLISECONDS);
            logger.log(Level.INFO, "Updater scheduled to run every " + UPDATER_FREQ/60/1000 + " minutes");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }
}
