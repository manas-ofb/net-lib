package org.example;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.SettableFuture;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

public class CachedHttpClient {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Cache<String, SettableFuture<String>> responseCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public Future<String> get(final String urlStr) {
        synchronized (getLockForKey(urlStr)) {
            SettableFuture<String> existingFuture = responseCache.getIfPresent(urlStr);
            if (existingFuture != null) {
                System.out.println("fast");
                return existingFuture;
            }

            SettableFuture<String> future = SettableFuture.create();
            responseCache.put(urlStr, future);

            executor.submit(() -> {
                try {
                    String response = sendHttpGet(urlStr);
                    future.set(response);
                } catch (Exception e) {
                    future.setException(e);
                    responseCache.invalidate(urlStr);
                }
            });

            return future;
        }
    }

    private String sendHttpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            response.append(line);
        }

        in.close();
        conn.disconnect();

        return response.toString();
    }

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    private Object getLockForKey(String key) {
        return locks.computeIfAbsent(key, k -> new Object());
    }
}
