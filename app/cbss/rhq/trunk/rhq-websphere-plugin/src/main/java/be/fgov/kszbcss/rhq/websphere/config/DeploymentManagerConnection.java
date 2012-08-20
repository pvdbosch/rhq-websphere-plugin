package be.fgov.kszbcss.rhq.websphere.config;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fgov.kszbcss.rhq.websphere.DeploymentManager;
import be.fgov.kszbcss.rhq.websphere.config.cache.CacheRefreshException;
import be.fgov.kszbcss.rhq.websphere.config.cache.DelayedRefreshCache;
import be.fgov.kszbcss.rhq.websphere.config.cache.MutablePriorityQueue;
import be.fgov.kszbcss.rhq.websphere.proxy.AppManagement;
import be.fgov.kszbcss.rhq.websphere.proxy.ConfigService;

import com.ibm.websphere.management.repository.ConfigEpoch;

/**
 * Manages the communication with the deployment manager of a given WebSphere cell. There will be
 * one instance of this class for each cell for which there is at least one monitored WebSphere
 * instance.
 */
class DeploymentManagerConnection implements Runnable {
    private static final Log log = LogFactory.getLog(DeploymentManagerConnection.class);

    private final ConfigQueryServiceFactory factory;
    private final CacheManager cacheManager;
    private final ConfigRepository configRepository;
    private final CellConfiguration config;
    private final ScheduledFuture<?> future;
    private final String cell;
    private final ExecutorService queryExecutorService;
    private final DelayedRefreshCache<ConfigQuery<?>,ConfigQueryResult> queryCache;
    private final ScheduledExecutorService epochPollExecutorService;
    private ConfigEpoch epoch;
    private int refCount;
    private boolean polled;
    private boolean waitForConnection = true;
    
    DeploymentManagerConnection(ConfigQueryServiceFactory factory, CacheManager cacheManager, DeploymentManager dm, String cell) {
        this.factory = factory;
        this.cacheManager = cacheManager;
        this.cell = cell;
        configRepository = dm.getMBeanClient("WebSphere:type=ConfigRepository,*").getProxy(ConfigRepository.class);
        config = new CellConfiguration(
                dm.getMBeanClient("WebSphere:type=ConfigService,*").getProxy(ConfigService.class),
                configRepository,
                dm.getMBeanClient("WebSphere:type=AppManagement,*").getProxy(AppManagement.class));
        cacheManager.addCache(cell);
        // TODO: set a thread factory to give meaningful names to threads
        queryExecutorService = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new MutablePriorityQueue<Runnable>());
        queryCache = new DelayedRefreshCache<ConfigQuery<?>,ConfigQueryResult>(cacheManager.getEhcache(cell), queryExecutorService, new ConfigQueryResultFactory(this));
        epochPollExecutorService = Executors.newScheduledThreadPool(1);
        future = epochPollExecutorService.scheduleWithFixedDelay(this, 0, 30, TimeUnit.SECONDS);
    }
    
    public void run() {
        ConfigEpoch epoch = null;
        Exception exception = null;
        try {
            epoch = configRepository.getRepositoryEpoch();
        } catch (Exception ex) {
            exception = ex;
        }
        synchronized (this) {
            if (this.epoch != null && exception != null) {
                log.error("Lost connection to the deployment manager for cell " + cell, exception);
            } else if (!polled && exception != null) {
                log.error("Connection to deployment manager unavailable for cell " + cell, exception);
            } else if (this.epoch == null && exception == null) {
                if (polled) {
                    log.info("Connection to deployment manager reestablished for cell " + cell);
                } else {
                    log.info("Connection to deployment manager established for cell " + cell);
                }
            } else if (this.epoch != null && epoch != null && !this.epoch.equals(epoch)) {
                if (log.isDebugEnabled()) {
                    log.debug("Epoch change detected for cell " + cell + "; old epoch: " + this.epoch + "; new epoch: " + epoch);
                }
            }
            if (epoch != null && !epoch.equals(this.epoch)) {
                // The ConfigService actually creates a workspace on the deployment manager. This workspace
                // contains copies of the configuration documents. If they change, then we need to refresh
                // the workspace. Otherwise we will not see the changes.
                config.refresh();
            }
            this.epoch = epoch;
            if (!polled) {
                polled = true;
                notifyAll();
            }
        }
    }
    
    synchronized ConfigEpoch getEpoch() {
        if (!polled && waitForConnection) {
            log.debug("Waiting for connection to deployment manager for cell " + cell);
            try {
                do {
                    wait();
                } while (!polled);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                // We only wait once. If we got interrupted, then this means that there is an issue
                // with the deployment manager and we should not wait the next time getEpoch is called.
                waitForConnection = false;
            }
        }
        return epoch;
    }
    
    String getCell() {
        return cell;
    }

    synchronized CellConfiguration getCellConfiguration() {
        return config;
    }

    @SuppressWarnings("unchecked")
    <T extends Serializable> T query(ConfigQuery<T> query, boolean immediate) throws InterruptedException {
        // If the current thread is already interrupted, then don't query the cache at all
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        T result;
        try {
            result = (T)queryCache.get(query, immediate).object;
        } catch (CacheRefreshException ex) {
            // TODO: handle this properly
            throw new RuntimeException(ex);
        }
        // TODO: this is probably no longer applicable
        // The interrupt flag may have been set by ConfigQueryResultFactory
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return result;
    }
    
    synchronized void incrementRefCount() {
        refCount++;
        if (log.isDebugEnabled()) {
            log.debug("New ref count is " + refCount);
        }
    }

    synchronized void decrementRefCount() {
        refCount--;
        if (log.isDebugEnabled()) {
            log.debug("New ref count is " + refCount);
        }
        if (refCount == 0) {
            log.debug("Destroying DeploymentManagerConnection");
            config.destroy();
            future.cancel(false);
            epochPollExecutorService.shutdownNow();
            queryExecutorService.shutdownNow();
            cacheManager.removeCache(cell);
            factory.removeDeploymentManagerConnection(this);
        }
    }
}
