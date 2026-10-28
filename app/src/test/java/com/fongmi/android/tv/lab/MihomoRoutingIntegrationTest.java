package com.fongmi.android.tv.lab;

import com.fongmi.android.tv.App;

import com.fongmi.android.tv.setting.ProxySetting;
import com.fongmi.android.tv.web.WebCall;
import com.github.catvod.Init;
import com.github.catvod.bean.Proxy;
import com.github.catvod.net.OkHttp;
import com.github.catvod.net.OkProxySelector;
import com.github.catvod.utils.Prefers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE, application = MihomoRoutingIntegrationTest.FixtureApp.class)
public class MihomoRoutingIntegrationTest {
    public static class FixtureApp extends App {
        @Override public void onCreate() { Init.set(this); }
    }
    private final List<MockWebServer> servers = new ArrayList<>();

    @Before public void initialize() {
        Init.set(RuntimeEnvironment.getApplication());
        Prefers.getPrefers().edit().clear().commit();
    }
    @After public void close() throws Exception {
        OkHttp.selector().setRuntimeProxy(null, Set.of());
        OkHttp.selector().clear();
        for (MockWebServer server : servers) server.close();
    }

    private MockWebServer server() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();
        servers.add(server);
        return server;
    }

    private static java.net.Proxy endpoint(int port) {
        return new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", port));
    }

    private static java.net.Proxy selected(OkProxySelector selector, String url) {
        return selector.select(URI.create(url)).get(0);
    }

    private static String get(OkHttpClient client, String url) throws Exception {
        try (var response = client.newCall(new Request.Builder().url(url).build()).execute()) {
            return response.body().string();
        }
    }

    private static List<Proxy> rules(String json) {
        List<Proxy> rules = Proxy.arrayFrom(JsonParser.parseString(json));
        if (rules.isEmpty()) new Gson().fromJson(json, Proxy[].class); // Expose a JVM fixture failure hidden by production fallback.
        assertFalse("proxy fixture must contain a real explicit rule", rules.isEmpty());
        return rules;
    }

    @Test public void inactiveSelectorAndStopRestoreOriginalPolicyExactly() {
        OkProxySelector selector = new OkProxySelector();
        URI remote = URI.create("https://unmatched.example.test/data");
        List<java.net.Proxy> before = selector.select(remote);
        selector.setRuntimeProxy(endpoint(12345), Set.of());
        assertEquals(endpoint(12345), selected(selector, remote.toString()));
        selector.setRuntimeProxy(null, Set.of());
        assertEquals(before, selector.select(remote));
    }

    @Test public void appAndSiteExplicitRulesRetainPriorityAndPreferencesRemainUnchanged() {
        Prefers.getPrefers().edit().putBoolean("shell_proxy", true)
                .putString("shell_proxy_url", "http://127.0.0.1:21001")
                .putString("shell_proxy_rules", "app.example.test").commit();
        ProxySetting.apply();
        var selector = OkHttp.selector();
        selector.addAll(rules("[{\"name\":\"site\",\"hosts\":[\"site.example.test\"],\"urls\":[\"http://127.0.0.1:21002\"]}]"));
        Map<String, ?> preferences = Map.copyOf(Prefers.getPrefers().getAll());
        List<Proxy> rules = List.copyOf(selector.getProxy());
        var app = selected(selector, "https://app.example.test/");
        var site = selected(selector, "https://site.example.test/");
        assertEquals(java.net.Proxy.Type.HTTP, app.type());
        assertEquals(java.net.Proxy.Type.HTTP, site.type());
        selector.setRuntimeProxy(endpoint(21003), Set.of());
        assertEquals(app, selected(selector, "https://app.example.test/"));
        assertEquals(site, selected(selector, "https://site.example.test/"));
        assertEquals(endpoint(21003), selected(selector, "https://other.example.test/"));
        ProxySetting.apply(); // Reloading persisted App rules must not remove the runtime default.
        assertEquals(endpoint(21003), selected(selector, "https://other.example.test/"));
        selector.setRuntimeProxy(null, Set.of());
        assertEquals(app, selected(selector, "https://app.example.test/"));
        assertEquals(site, selected(selector, "https://site.example.test/"));
        assertEquals(preferences, Prefers.getPrefers().getAll());
        assertEquals(rules.stream().map(Proxy::getUrls).collect(java.util.stream.Collectors.toSet()),
                selector.getProxy().stream().map(Proxy::getUrls).collect(java.util.stream.Collectors.toSet()));
    }

    @Test public void explicitWildcardStillWinsOverAutomaticDefault() {
        var selector = new OkProxySelector();
        selector.addAll(rules("[{\"name\":\"site\",\"hosts\":[\"*\"],\"urls\":[\"http://127.0.0.1:21001\"]}]"));
        var before = selected(selector, "http://any.example.test/");
        selector.setRuntimeProxy(endpoint(21003), Set.of());
        assertEquals(before, selected(selector, "http://any.example.test/"));
    }

    @Test public void loopbackControllerAndLocalInterfaceNeverLoopThroughProxyOrWildcard() {
        var selector = new OkProxySelector();
        selector.addAll(rules("[{\"hosts\":[\"*\"],\"urls\":[\"http://127.0.0.1:21001\"]}]"));
        selector.setRuntimeProxy(endpoint(21003), Set.of("192.168.1.10"));
        for (String host : List.of("127.0.0.1", "127.1.2.3", "localhost", "localhost.", "test.localhost", "[::1]",
                "[::ffff:127.0.0.1]", "[::ffff:7f00:1]", "0.0.0.0", "192.168.1.10")) {
            assertEquals(host, java.net.Proxy.NO_PROXY, selected(selector, "http://" + host + ":23456/configs"));
        }
        assertThrows(IllegalArgumentException.class, () -> selector.setRuntimeProxy(
                new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("192.0.2.1", 1234)), Set.of()));
    }

    @Test public void actualSharedClientPlayerAndNativeBridgeDeliverRequestsToSimulatedRuleCore() throws Exception {
        MockWebServer core = server();
        // The simulated core labels its own DIRECT/node decision. App routing is identical in both cases.
        core.enqueue(new MockResponse.Builder().body("DIRECT-from-core").build());
        core.enqueue(new MockResponse.Builder().body("NODE-from-core").build());
        core.enqueue(new MockResponse.Builder().body("BRIDGE-from-core").build());
        OkHttpClient clientBeforeStart = OkHttp.client();
        OkHttpClient playerBeforeStart = OkHttp.player();
        assertSame(OkHttp.selector(), clientBeforeStart.proxySelector());
        assertSame(OkHttp.selector(), playerBeforeStart.proxySelector());
        OkHttp.selector().setRuntimeProxy(endpoint(core.getPort()), Set.of());
        assertEquals("DIRECT-from-core", get(clientBeforeStart, "http://direct.example.test/api"));
        assertEquals("NODE-from-core", get(playerBeforeStart, "http://node.example.test/segment.ts"));
        JsonObject payload = new JsonObject();
        payload.addProperty("url", "http://bridge.example.test/api");
        String bridgeResult = WebCall.request(payload);
        assertTrue(bridgeResult, bridgeResult.contains("BRIDGE-from-core"));
        for (String host : List.of("direct.example.test", "node.example.test", "bridge.example.test")) {
            var request = core.takeRequest(3, TimeUnit.SECONDS);
            assertNotNull(request);
            assertTrue(request.getRequestLine(), request.getRequestLine().contains("http://" + host + "/"));
        }
    }

    @Test public void preexistingIdleConnectionIsEvictedOnBindingAndStopWithSameClient() throws Exception {
        MockWebServer origin = server(), core = server();
        origin.enqueue(new MockResponse.Builder().body("before").build());
        origin.enqueue(new MockResponse.Builder().body("restored").build());
        core.enqueue(new MockResponse.Builder().body("inside-core").build());
        OkProxySelector selector = new OkProxySelector();
        ConnectionPool pool = selector.registerRuntimePool(new ConnectionPool());
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(selector.runtimeCleanupInterceptor()).proxySelector(selector).connectionPool(pool)
                .dns(host -> List.of(InetAddress.getByName("127.0.0.1"))).build();
        String url = origin.url("/data").newBuilder().host("origin.example.test").build().toString();
        assertEquals("before", get(client, url));
        assertEquals(1, pool.idleConnectionCount());
        selector.setRuntimeProxy(endpoint(core.getPort()), Set.of());
        long cleanupDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (pool.idleConnectionCount() != 0 && System.nanoTime() < cleanupDeadline) Thread.sleep(10);
        assertEquals(0, pool.idleConnectionCount());
        assertEquals("inside-core", get(client, url));
        selector.setRuntimeProxy(null, Set.of());
        assertEquals("restored", get(client, url));
        assertEquals(1, core.getRequestCount());
        assertEquals(2, origin.getRequestCount());
        pool.evictAll();
    }

    @Test public void bindingChangeDoesNotInterruptActiveResponse() throws Exception {
        MockWebServer origin = server();
        String body = "x".repeat(262144);
        origin.enqueue(new MockResponse.Builder().body(body).build());
        var selector = new OkProxySelector();
        ConnectionPool pool = selector.registerRuntimePool(new ConnectionPool());
        var client = new OkHttpClient.Builder().addInterceptor(selector.runtimeCleanupInterceptor()).proxySelector(selector).connectionPool(pool).build();
        try (var response = client.newCall(new Request.Builder().url(origin.url("/playing")).build()).execute()) {
            assertEquals('x', response.body().byteStream().read());
            assertEquals(0, pool.idleConnectionCount());
            selector.setRuntimeProxy(endpoint(21003), Set.of());
            assertEquals(1, pool.connectionCount());
            assertEquals(body.length() - 1, response.body().byteStream().readAllBytes().length);
        }
        pool.evictAll();
    }

    @Test public void failedRuntimeEndpointIsRevokedImmediatelyAndNeedsReadinessAgain() {
        var selector = new OkProxySelector();
        URI uri = URI.create("http://remote.example.test/");
        var before = selector.select(uri);
        var proxy = endpoint(21003);
        selector.setRuntimeProxy(proxy, Set.of());
        selector.connectFailed(uri, proxy.address(), new IOException("refused"));
        assertNull(selector.getRuntimeProxy());
        assertEquals(before, selector.select(uri));
    }

    @Test public void socksTargetFailureDoesNotMisclassifyHealthyCoreOrBypassItsRules() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(4000);
            var peer = CompletableFuture.runAsync(() -> {
                try {
                    // Java SOCKS connect: accept no-auth, then reject the target while keeping the listener alive.
                    try (var socket = listener.accept()) {
                        socket.setSoTimeout(3000);
                        var input = socket.getInputStream();
                        assertEquals(5, input.read());
                        int methods = input.read();
                        assertEquals(methods, input.readNBytes(methods).length);
                        socket.getOutputStream().write(new byte[]{5, 0});
                        byte[] header = input.readNBytes(4);
                        assertEquals(5, header[0]);
                        assertEquals(1, header[1]);
                        int addressBytes = header[3] == 3 ? input.read() : header[3] == 1 ? 4 : 16;
                        assertEquals(addressBytes + 2, input.readNBytes(addressBytes + 2).length);
                        socket.getOutputStream().write(new byte[]{5, 4, 0, 1, 0, 0, 0, 0, 0, 0});
                    }
                    // Independent readiness check succeeds even though the target request failed.
                    try (var socket = listener.accept()) {
                        socket.setSoTimeout(3000);
                        assertArrayEquals(new byte[]{5, 1, 0}, socket.getInputStream().readNBytes(3));
                        socket.getOutputStream().write(new byte[]{5, 0});
                    }
                } catch (IOException e) { throw new RuntimeException(e); }
            });
            var selector = new OkProxySelector();
            var proxy = new java.net.Proxy(java.net.Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", listener.getLocalPort()));
            selector.setRuntimeProxy(proxy, Set.of());
            var client = new OkHttpClient.Builder().proxySelector(selector).retryOnConnectionFailure(false).build();
            assertThrows(IOException.class, () -> get(client, "http://unreachable.example.test/media"));
            assertEquals(proxy, selector.getRuntimeProxy());
            assertEquals(proxy, selected(selector, "http://another.example.test/"));
            MihomoProxyConfig.handshake(proxy);
            peer.get(4, TimeUnit.SECONDS);
        }
    }
}
