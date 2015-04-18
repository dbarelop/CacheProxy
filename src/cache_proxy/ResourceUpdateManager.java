package cache_proxy;

import database.DatabaseHelper;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dbarelop on 18/04/15.
 */
public class ResourceUpdateManager implements Runnable {
    private static final Logger logger = Logger.getLogger(ResourceUpdateManager.class.getName());
    private static final long CACHE_UPDATE_INTERVAL = 12*60*60*1000;  // 12 hours
    private static final int MAX_THREADS = 8;
    private static Queue<Resource> outdatedResources = new ArrayDeque<>();
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

    @Override
    public void run() {
        for (Resource r : DatabaseHelper.getResources()) {
            if (System.currentTimeMillis() - r.getTimestamp() > CACHE_UPDATE_INTERVAL) {
                logger.log(Level.INFO, "Resource " + r.getURL() + " is OUTDATED. Updating...");
                outdatedResources.add(r);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        Resource r = getOutdatedResource();
                        ResourceManager.updateResource(r);
                    }
                });
            }
        }
    }

    public static synchronized Resource getOutdatedResource() {
        return outdatedResources.poll();
    }
}
