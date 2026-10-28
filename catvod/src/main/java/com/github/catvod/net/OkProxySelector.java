package com.github.catvod.net;

import com.github.catvod.bean.Proxy;
import com.github.catvod.crawler.DebugEventLimiter;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Util;

import java.io.IOException;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.lang.ref.WeakReference;

import okhttp3.ConnectionPool;

public class OkProxySelector extends ProxySelector {

    private static final long DEBUG_LOG_INTERVAL_MS = TimeUnit.SECONDS.toMillis(30);
    private static final java.util.concurrent.ExecutorService POOL_CLEANER =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "mihomo-pool-cleaner");
                thread.setDaemon(true);
                return thread;
            });

    private final List<Proxy> proxy;
    private final ProxySelector system;
    private final DebugEventLimiter debugLogLimiter;
    private boolean authSet;
    // Separate from persisted App/site rules. Never installed as the JVM default selector.
    private volatile RuntimeProxy runtimeProxy;
    private volatile java.util.concurrent.Future<?> pendingCleanup;
    private final List<WeakReference<ConnectionPool>> runtimePools = new CopyOnWriteArrayList<>();

    public ConnectionPool registerRuntimePool(ConnectionPool pool) {
        runtimePools.removeIf(reference -> reference.get() == null);
        runtimePools.add(new WeakReference<>(pool));
        return pool;
    }

    private void evictRuntimeIdleConnections() {
        // Capture pools before enqueueing; the task never changes proxy/session state.
        List<ConnectionPool> pools = new java.util.ArrayList<>();
        for (WeakReference<ConnectionPool> reference : runtimePools) {
            ConnectionPool pool = reference.get();
            if (pool != null) pools.add(pool);
        }
        runtimePools.removeIf(reference -> reference.get() == null);
        pendingCleanup = POOL_CLEANER.submit(() -> {
            for (ConnectionPool pool : pools) {
                try {
                    pool.evictAll(); // OkHttp retains active exchanges.
                } catch (RuntimeException e) {
                    android.util.Log.e("MihomoProxy", "Background pool cleanup failed", e);
                }
            }
        });
    }

    /** Runs on the request execution thread, never in the lifecycle callback. */
    public okhttp3.Interceptor runtimeCleanupInterceptor() {
        return chain -> {
            java.util.concurrent.Future<?> cleanup;
            do {
                cleanup = pendingCleanup;
                if (cleanup == null) break;
                try {
                    cleanup.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.io.InterruptedIOException("Interrupted waiting for proxy pool cleanup");
                } catch (java.util.concurrent.ExecutionException e) {
                    throw new IOException("Proxy pool cleanup failed", e.getCause());
                }
            } while (cleanup != pendingCleanup);
            return chain.proceed(chain.request());
        };
    }

    private record RuntimeProxy(java.net.Proxy proxy, Set<String> localHosts) { }

    public synchronized boolean setRuntimeProxy(java.net.Proxy value, Set<String> localHosts) {
        RuntimeProxy next = null;
        if (value != null) {
            if (!(value.address() instanceof InetSocketAddress address)
                    || !isLoopbackHost(address.getHostString()) || address.getPort() <= 0
                    || value.type() == java.net.Proxy.Type.DIRECT) {
                throw new IllegalArgumentException("Runtime proxy must be a loopback HTTP/SOCKS endpoint");
            }
            next = new RuntimeProxy(value, localHosts.stream().map(OkProxySelector::normalizeHost).collect(java.util.stream.Collectors.toUnmodifiableSet()));
        }
        if (java.util.Objects.equals(runtimeProxy, next)) return false;
        runtimeProxy = next;
        evictRuntimeIdleConnections();
        return true;
    }

    public java.net.Proxy getRuntimeProxy() {
        RuntimeProxy current = runtimeProxy;
        return current == null ? null : current.proxy();
    }

    private static boolean isLoopbackHost(String host) {
        String value = normalizeHost(host);
        return value.equals("localhost") || value.endsWith(".localhost")
                || value.equals("::1") || value.equals("0:0:0:0:0:0:0:1")
                || value.matches("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }

    private static boolean localTarget(String host, RuntimeProxy current) {
        String value = normalizeHost(host);
        return isLoopbackHost(value) || value.equals("0.0.0.0") || value.equals("::")
                || value.equals("0:0:0:0:0:0:0:0") || current.localHosts().contains(value);
    }

    private static String normalizeHost(String host) {
        String value = host.toLowerCase(Locale.ROOT).replace("[", "").replace("]", "").split("%", 2)[0];
        if (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        if (value.contains(":") && value.matches("[0-9a-f:.]+")) {
            try { return InetAddress.getByName(value).getHostAddress(); } // Numeric literal only; no DNS lookup.
            catch (IOException ignored) { }
        }
        return value;
    }

    public OkProxySelector() {
        proxy = new CopyOnWriteArrayList<>();
        system = ProxySelector.getDefault();
        debugLogLimiter = new DebugEventLimiter(64);
        Authenticator.setDefault(new ProxyAuthenticator(this));
    }

    public synchronized void addAll(List<Proxy> items) {
        if (items.isEmpty()) return;
        Authenticator.setDefault(new ProxyAuthenticator(this));
        items.forEach(Proxy::init);
        proxy.addAll(items);
        proxy.sort(null);
        SpiderDebug.log("proxy", "selector add rules=%s total=%s", items.size(), proxy.size());
    }

    public synchronized void remove(String name) {
        int before = proxy.size();
        proxy.removeIf(item -> item.getName().equals(name));
        int removed = before - proxy.size();
        if (removed > 0) SpiderDebug.log("proxy", "selector remove name=%s removed=%s total=%s", name, removed, proxy.size());
    }

    public synchronized void clear() {
        Authenticator.setDefault(null);
        proxy.clear();
        SpiderDebug.log("proxy", "selector clear");
    }

    public List<Proxy> getProxy() {
        return proxy;
    }

    private List<java.net.Proxy> fallback(URI uri) {
        return system != null ? system.select(uri) : List.of(java.net.Proxy.NO_PROXY);
    }

    @Override
    public List<java.net.Proxy> select(URI uri) {
        String host = uri.getHost();
        RuntimeProxy current = runtimeProxy;
        if (current != null && host != null && localTarget(host, current)) return List.of(java.net.Proxy.NO_PROXY);
        if (host == null) return fallback(uri, "no-host");
        if ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)) return fallback(uri, "local-target");
        for (Proxy item : proxy) {
            for (String rule : item.getHosts()) {
                if (!matches(host, rule)) continue;
                List<java.net.Proxy> selected = item.getProxies().isEmpty() ? fallback(uri, "empty-proxy") : item.getProxies();
                logSelection(uri, "hit", host, rule, item.getName(), selected.size());
                return selected;
            }
        }
        String scheme = uri.getScheme();
        if (current != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                || "ws".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme))) {
            logSelection(uri, "runtime", host, "", "mihomo-runtime", 1);
            return List.of(current.proxy());
        }
        return fallback(uri, proxy.isEmpty() ? "no-rule" : "no-match");
    }

    private List<java.net.Proxy> fallback(URI uri, String reason) {
        List<java.net.Proxy> selected = fallback(uri);
        logFallback(uri, reason, selected.size());
        return selected;
    }

    private boolean matches(String host, String rule) {
        return "*".equals(rule) || Util.containOrMatch(host, rule);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress socketAddress, IOException e) {
        synchronized (this) {
            RuntimeProxy current = runtimeProxy;
            // SOCKS Socket.connect includes the target dial: a SOCKS rejection is not a dead local core.
            // Its independent greeting/liveness monitor decides when to revoke the binding.
            if (current != null && current.proxy().type() == java.net.Proxy.Type.HTTP
                    && current.proxy().address().equals(socketAddress)) {
                runtimeProxy = null;
                evictRuntimeIdleConnections();
                android.util.Log.w("MihomoProxy", "Local proxy connect failed; runtime binding revoked; future requests restore original proxy policy");
            }
        }
        logConnectFailed(uri, socketAddress, e);
        if (system != null) system.connectFailed(uri, socketAddress, e);
    }

    private void logSelection(URI uri, String reason, String host, String rule, String name, int count) {
        if (!SpiderDebug.isEnabled()) return;
        DebugEventLimiter.Decision decision = acquire("select", reason, uri, null);
        if (!decision.allowed()) return;
        SpiderDebug.log("proxy", "select hit uri=%s host=%s ruleHash=%s nameHash=%s proxyCount=%s suppressed=%s", OkHttpLogPolicy.redactUri(uri), OkHttpLogPolicy.redactHost(host), OkHttpLogPolicy.redactHost(rule), OkHttpLogPolicy.redactHost(name), count, decision.suppressedCount());
    }

    private void logFallback(URI uri, String reason, int count) {
        if (!SpiderDebug.isEnabled()) return;
        DebugEventLimiter.Decision decision = acquire("fallback", reason, uri, null);
        if (!decision.allowed()) return;
        SpiderDebug.log("proxy", "select fallback reason=%s uri=%s proxyCount=%s suppressed=%s", reason, OkHttpLogPolicy.redactUri(uri), count, decision.suppressedCount());
    }

    private void logConnectFailed(URI uri, SocketAddress address, IOException error) {
        if (!SpiderDebug.isEnabled()) return;
        String addressType = address == null ? "none" : address.getClass().getSimpleName();
        DebugEventLimiter.Decision decision = acquire("failure", addressType, uri, error);
        if (!decision.allowed()) return;
        SpiderDebug.log("proxy", "connectFailed uri=%s addressType=%s error=%s suppressed=%s", OkHttpLogPolicy.redactUri(uri), addressType, OkHttpLogPolicy.errorChain(error), decision.suppressedCount());
    }

    private DebugEventLimiter.Decision acquire(String event, String detail, URI uri, Throwable error) {
        String scheme = uri == null || uri.getScheme() == null ? "" : uri.getScheme();
        String host = uri == null || uri.getHost() == null ? "" : uri.getHost();
        String errorType = error == null ? "" : error.getClass().getName();
        String key = event + '|' + detail + '|' + scheme + '|' + host + '|' + errorType;
        long nowMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        return debugLogLimiter.acquire(key, nowMs, DEBUG_LOG_INTERVAL_MS);
    }
}
