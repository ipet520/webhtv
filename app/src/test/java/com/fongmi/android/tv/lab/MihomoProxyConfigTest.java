package com.fongmi.android.tv.lab;

import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

import static org.junit.Assert.*;

public class MihomoProxyConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final List<MockWebServer> servers = new ArrayList<>();

    @After public void close() throws Exception { for (MockWebServer server : servers) server.close(); }

    private MockWebServer server() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();
        servers.add(server);
        return server;
    }

    private MihomoProxyConfig.Launch launch(String yaml, String flags) throws Exception {
        File config = temporary.newFile();
        Files.writeString(config.toPath(), yaml);
        return MihomoProxyConfig.Launch.command("mihomo -f '" + config + "' " + flags, temporary.getRoot());
    }

    @Test public void liveMixedPortOverridesStaleYamlAndUsesAuthenticatedDirectController() throws Exception {
        MockWebServer control = server(), inbound = server();
        control.enqueue(new MockResponse.Builder().body("{\"mixed-port\":" + inbound.getPort() + ",\"port\":0,\"mode\":\"rule\"}").build());
        inbound.enqueue(new MockResponse.Builder().code(200).build());
        var ready = MihomoProxyConfig.detect(launch("mixed-port: 1\n", "-ext-ctl 0.0.0.0:" + control.getPort() + " -secret 'test secret'"));
        assertEquals("controller", ready.source());
        assertEquals(Proxy.Type.HTTP, ready.proxy().type());
        assertEquals(inbound.getPort(), ((InetSocketAddress) ready.proxy().address()).getPort());
        var request = control.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/configs", request.getTarget());
        assertEquals("Bearer test secret", request.getHeaders().get("Authorization"));
        assertTrue(inbound.takeRequest(2, TimeUnit.SECONDS).getRequestLine().startsWith("CONNECT 127.0.0.1:1 "));
    }

    @Test public void httpYamlFallbackRequiresProtocolAcceptance() throws Exception {
        MockWebServer inbound = server();
        inbound.enqueue(new MockResponse.Builder().code(200).build());
        var ready = MihomoProxyConfig.detect(launch("port: '" + inbound.getPort() + "' # local\nmode: rule\n", ""));
        assertEquals("yaml+protocol", ready.source());
        assertEquals(Proxy.Type.HTTP, ready.proxy().type());
        inbound.enqueue(new MockResponse.Builder().code(407).build());
        assertThrows(IOException.class, () -> MihomoProxyConfig.detect(launch("port: " + inbound.getPort(), "")));
    }

    @Test public void socksOnlyUsesSocks5NoAuthHandshake() throws Exception {
        try (ServerSocket inbound = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            inbound.setSoTimeout(3000);
            CompletableFuture<Void> peer = CompletableFuture.runAsync(() -> {
                try (var socket = inbound.accept()) {
                    socket.setSoTimeout(2000);
                    assertArrayEquals(new byte[]{5, 1, 0}, socket.getInputStream().readNBytes(3));
                    socket.getOutputStream().write(new byte[]{5, 0});
                } catch (IOException e) { throw new RuntimeException(e); }
            });
            var ready = MihomoProxyConfig.detect(launch("socks-port: " + inbound.getLocalPort(), ""));
            assertEquals(Proxy.Type.SOCKS, ready.proxy().type());
            peer.get(3, TimeUnit.SECONDS);
        }
    }

    @Test public void controllerFailureNeverFallsBackToPossiblyStaleYaml() throws Exception {
        MockWebServer control = server(), inbound = server();
        control.enqueue(new MockResponse.Builder().code(401).build());
        var launch = launch("port: " + inbound.getPort(), "-ext-ctl 127.0.0.1:" + control.getPort());
        assertThrows(IOException.class, () -> MihomoProxyConfig.detect(launch));
        assertEquals(0, inbound.getRequestCount());
    }

    @Test public void configChangesWithoutControllerRequireRestartAndDoNotGuessNewPort() throws Exception {
        var launch = launch("mixed-port: 12345\n", "");
        Files.writeString(launch.config().toPath(), "mixed-port: 23456\n");
        IOException error = assertThrows(IOException.class, () -> MihomoProxyConfig.detect(launch));
        assertTrue(error.getMessage().contains("restart"));
    }

    @Test public void closedPortInvalidPortsAndWrongProtocolNeverBecomeReady() throws Exception {
        int closed;
        try (ServerSocket socket = new ServerSocket(0)) { closed = socket.getLocalPort(); }
        assertThrows(IOException.class, () -> MihomoProxyConfig.detect(launch("port: " + closed, "")));
        assertThrows(IOException.class, () -> MihomoProxyConfig.detect(launch("socks-port: " + closed, "")));
        for (String port : List.of("-1", "65536", "*alias", "null", "[123]")) {
            assertThrows(IOException.class, () -> MihomoProxyConfig.detect(launch("mixed-port: " + port, "")));
        }
        MockWebServer notProxy = server();
        notProxy.enqueue(new MockResponse.Builder().code(404).build());
        assertThrows(IOException.class, () -> MihomoProxyConfig.detect(launch("port: " + notProxy.getPort(), "")));
    }

    @Test public void controllerPortAndNonLoopbackBindingCannotBecomeProxyEndpoint() throws Exception {
        var config = JsonParser.parseString("{\"mixed-port\":12345,\"socks-port\":23456}").getAsJsonObject();
        var candidates = MihomoProxyConfig.candidates(config, new InetSocketAddress("127.0.0.1", 12345));
        assertEquals(1, candidates.size());
        assertEquals(Proxy.Type.SOCKS, candidates.get(0).type());
        config.addProperty("allow-lan", true);
        config.addProperty("bind-address", "192.0.2.1");
        assertThrows(IOException.class, () -> MihomoProxyConfig.candidates(config, null));
        assertEquals("127.0.0.1", MihomoProxyConfig.controller("0.0.0.0:12345").getHostString());
        assertThrows(IOException.class, () -> MihomoProxyConfig.controller("example.com:12345"));
    }

    @Test public void commandArgumentsPreserveSpacesEmptySecretAndExplicitConfigHome() throws Exception {
        File home = temporary.newFolder("config space");
        Files.writeString(new File(home, "config.yaml").toPath(), "port: 0");
        var launch = MihomoProxyConfig.Launch.command("exec /data/bin/mihomo -d '" + home + "' -secret ''", temporary.getRoot());
        assertEquals(new File(home, "config.yaml"), launch.config());
        assertEquals("", launch.secret());
        assertThrows(IOException.class, () -> MihomoProxyConfig.Launch.command("mihomo -v", home));
        assertThrows(IOException.class, () -> MihomoProxyConfig.Launch.command("mihomo -f config.yaml && other", home));
        assertThrows(IOException.class, () -> MihomoProxyConfig.Launch.command("mihomo -f 'unfinished", home));
    }

    @Test public void duplicateYamlPortsAreNotGuessed() throws Exception {
        assertThrows(IOException.class, () -> MihomoProxyConfig.detect(launch("port: 123\nport: 456\n", "")));
    }

    @Test public void livePortChangeRechecksProtocolAndHttpHealthChecksDoNotDialEveryTick() throws Exception {
        MockWebServer control = server(), first = server(), second = server();
        var launch = launch("mode: rule\n", "-ext-ctl 127.0.0.1:" + control.getPort());
        var detector = new MihomoProxyConfig.Detector(launch);
        String one = "{\"mixed-port\":" + first.getPort() + ",\"mode\":\"rule\"}";
        control.enqueue(new MockResponse.Builder().body(one).build());
        control.enqueue(new MockResponse.Builder().body(one).build());
        control.enqueue(new MockResponse.Builder().body("{\"mixed-port\":" + second.getPort() + "}").build());
        first.enqueue(new MockResponse.Builder().code(200).build());
        second.enqueue(new MockResponse.Builder().code(200).build());
        assertEquals(first.getPort(), ((InetSocketAddress) detector.detect().proxy().address()).getPort());
        assertNotNull(first.takeRequest(2, TimeUnit.SECONDS));
        detector.detect(); // TCP liveness check only after a verified HTTP protocol check.
        assertEquals(1, first.getRequestCount());
        assertEquals(second.getPort(), ((InetSocketAddress) detector.detect().proxy().address()).getPort());
        assertNotNull(second.takeRequest(2, TimeUnit.SECONDS));
    }
}
