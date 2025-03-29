package idespring.lab5.config;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CacheConfig<K, V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final long maxAgeInMillis;
    private final int maxSize;

    private final LinkedList<K> accessOrder = new LinkedList<>();

    private final Object lock = new Object();

    public CacheConfig(@Value("${cache.maxAge}") long maxAgeInMillis,
                       @Value("${cache.maxSize}") int maxSize) {
        this.maxAgeInMillis = maxAgeInMillis;
        this.maxSize = maxSize;

        executor.scheduleAtFixedRate(this::cleanExpiredEntries,
                maxAgeInMillis / 2,
                maxAgeInMillis / 2,
                TimeUnit.MILLISECONDS);
    }

    public CacheConfig() {
        this.maxAgeInMillis = 600000000;
        this.maxSize = 100;

        executor.scheduleAtFixedRate(this::cleanExpiredEntries,
                maxAgeInMillis / 2,
                maxAgeInMillis / 2,
                TimeUnit.MILLISECONDS);
    }

    public void put(K key, V value) {
        CacheEntry<V> entry = new CacheEntry<>(value);

        synchronized (lock) {
            if (cache.containsKey(key)) {
                accessOrder.remove(key);
            } else if (cache.size() >= maxSize) {
                K oldestKey = accessOrder.poll();
                if (oldestKey != null) {
                    cache.remove(oldestKey);
                }
            }

            accessOrder.addLast(key);
            cache.put(key, entry);
        }

        executor.schedule(() -> remove(key), maxAgeInMillis, TimeUnit.MILLISECONDS);
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        entry.updateAccessTime();
        synchronized (lock) {
            accessOrder.remove(key);
            accessOrder.addLast(key);
        }

        return entry.getValue();
    }

    public void remove(K key) {
        synchronized (lock) {
            cache.remove(key);
            accessOrder.remove(key);
        }
    }

    public int size() {
        return cache.size();
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        List<K> expiredKeys = new ArrayList<>();

        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            if (now - entry.getValue().getCreationTime() > maxAgeInMillis) {
                expiredKeys.add(entry.getKey());
            }
        }

        for (K key : expiredKeys) {
            remove(key);
        }
    }

    private static class CacheEntry<V> {
        private final V value;
        private final long creationTime;
        private long lastAccessTime;

        public CacheEntry(V value) {
            this.value = value;
            this.creationTime = System.currentTimeMillis();
            this.lastAccessTime = this.creationTime;
        }

        public V getValue() {
            return value;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}