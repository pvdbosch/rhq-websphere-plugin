package be.fgov.kszbcss.rhq.websphere.config.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class DelayedRefreshCache<K,V> {
    private static final Log log = LogFactory.getLog(DelayedRefreshCache.class);
    
    private final Ehcache underlyingCache;
    private final ExecutorService executorService;
    private final DelayedRefreshCacheEntryFactory<K,V> entryFactory;
    private final AtomicInteger currentRequestId = new AtomicInteger();
    private final Map<K,RefreshRequest<K,V>> pendingRequests = new HashMap<K,RefreshRequest<K,V>>();
    
    public DelayedRefreshCache(Ehcache underlyingCache, ExecutorService executorService, DelayedRefreshCacheEntryFactory<K,V> entryFactory) {
        this.underlyingCache = underlyingCache;
        this.executorService = executorService;
        this.entryFactory = entryFactory;
    }
    
    public V get(K key, boolean immediate) throws InterruptedException, CacheRefreshException {
        V value;
        RefreshRequest<K,V> refreshRequest;
        synchronized (this) {
            Element element = underlyingCache.get(key);
            value = element == null ? null : (V)element.getObjectValue();
            if (value == null || entryFactory.isStale(key, value)) {
                refreshRequest = pendingRequests.get(key);
                if (refreshRequest == null) {
                    refreshRequest = new RefreshRequest<K,V>(this, currentRequestId.incrementAndGet(), key, immediate);
                    if (log.isDebugEnabled()) {
                        log.debug("Scheduling refresh request " + refreshRequest.getId()
                                + " (cache=" + underlyingCache.getName()
                                + "; immediate=" + refreshRequest.isImmediate() + ")");
                    }
                    pendingRequests.put(key, refreshRequest);
                    executorService.submit(refreshRequest);
                    if (log.isDebugEnabled()) {
                        log.debug("Total number of pending requests: " + pendingRequests.size());
                    }
                } else if (immediate) {
                    refreshRequest.setImmediate();
                }
            } else {
                // The cache is up-to-date
                return value;
            }
        }
        if (value == null || immediate) {
            if (log.isDebugEnabled()) {
                log.debug("Waiting for completion of refresh request " + refreshRequest.getId()
                        + " (cache=" + underlyingCache.getName() + ")");
            }
            return refreshRequest.getValue();
        } else {
            // Return the stale value
            return value;
        }
    }
    
    V execute(RefreshRequest<K,V> refreshRequest) throws CacheRefreshException {
        if (log.isDebugEnabled()) {
            long delay = System.currentTimeMillis() - refreshRequest.getTime();
            log.debug("Executing refresh request " + refreshRequest.getId()
                    + " (cache=" + underlyingCache.getName()
                    + "; delay=" + delay
                    + "; immediate=" + refreshRequest.isImmediate() + ")");
        }
        K key = refreshRequest.getKey();
        V value = null;
        CacheRefreshException exception = null;
        try {
            value = entryFactory.createEntry(key);
        } catch (CacheRefreshException ex) {
            exception = ex;
        } catch (Throwable ex) {
            exception = new CacheRefreshException("Unexpected exception", ex);
        }
        if (log.isDebugEnabled()) {
            if (exception == null) {
                log.debug("Refresh request " + refreshRequest.getId() + " (cache=" + underlyingCache.getName() + ") successfully executed");
            } else {
                log.debug("Refresh request " + refreshRequest.getId() + " (cache=" + underlyingCache.getName() + ") failed with exception", exception);
            }
        }
        synchronized (this) {
            if (exception == null) {
                underlyingCache.put(new Element(key, value));
            }
            pendingRequests.remove(key);
            if (log.isDebugEnabled()) {
                log.debug("Total number of pending requests: " + pendingRequests.size());
            }
        }
        if (exception == null) {
            return value;
        } else {
            throw exception;
        }
    }
}
