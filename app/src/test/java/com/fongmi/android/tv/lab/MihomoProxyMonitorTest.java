package com.fongmi.android.tv.lab;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class MihomoProxyMonitorTest {
    private final AtomicReference<Proxy> selected = new AtomicReference<>();
    private final List<String> events = new ArrayList<>();
    private final MihomoProxyMonitor monitor = new MihomoProxyMonitor(proxy -> !java.util.Objects.equals(selected.getAndSet(proxy), proxy),
            (key, state) -> events.add(state));
    private static Proxy endpoint(int port) { return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", port)); }
    private static MihomoProxyConfig.Ready ready(int port) { return new MihomoProxyConfig.Ready(endpoint(port), "test"); }

    @Test public void onlyReadinessActivatesAndNormalStopRestores() {
        AtomicBoolean ready = new AtomicBoolean();
        monitor.poll();
        assertNull(selected.get());
        monitor.start("mihomo/run_config", new Object(), () -> true, () -> {
            if (!ready.get()) throw new IOException("not ready");
            return ready(12345);
        });
        monitor.poll();
        assertNull(selected.get());
        ready.set(true);
        monitor.poll();
        assertEquals(endpoint(12345), selected.get());
        int eventCount = events.size();
        monitor.poll();
        assertEquals(eventCount, events.size());
        monitor.stop("other/command");
        assertNotNull(selected.get());
        monitor.stop("mihomo/run_config");
        monitor.poll();
        assertNull(selected.get());
    }

    @Test public void crashRestartPortChangeAndUnavailableListenerUpdateBinding() {
        AtomicBoolean alive = new AtomicBoolean(true);
        AtomicReference<MihomoProxyConfig.Ready> result = new AtomicReference<>(ready(12345));
        monitor.start("m/run", new Object(), alive::get, () -> {
            if (result.get() == null) throw new IOException("closed");
            return result.get();
        });
        monitor.poll();
        assertEquals(endpoint(12345), selected.get());
        result.set(ready(23456));
        monitor.poll();
        assertEquals(endpoint(23456), selected.get());
        result.set(null);
        monitor.poll();
        assertNull(selected.get());
        result.set(ready(34567));
        monitor.poll();
        alive.set(false);
        monitor.poll();
        assertNull(selected.get());
        assertTrue(events.get(events.size() - 1).contains("CORE_NOT_RUNNING"));
        monitor.start("m/run", new Object(), () -> true, () -> ready(45678));
        monitor.poll();
        assertEquals(endpoint(45678), selected.get());
    }

    @Test public void stoppedOrReplacedSessionRejectsLateProbeAndOldExit() throws Exception {
        CountDownLatch probing = new CountDownLatch(1), complete = new CountDownLatch(1);
        Object oldOwner = new Object(), newOwner = new Object();
        monitor.start("m/run", oldOwner, () -> true, () -> {
            probing.countDown();
            assertTrue(complete.await(3, TimeUnit.SECONDS));
            return ready(12345);
        });
        Thread oldPoll = new Thread(monitor::poll);
        oldPoll.start();
        assertTrue(probing.await(3, TimeUnit.SECONDS));
        monitor.stop("m/run");
        monitor.start("m/run", newOwner, () -> true, () -> ready(23456));
        monitor.poll();
        complete.countDown();
        oldPoll.join(3000);
        assertFalse(oldPoll.isAlive());
        assertEquals(endpoint(23456), selected.get());
        monitor.processExited("m/run", oldOwner);
        assertEquals(endpoint(23456), selected.get());
        monitor.processExited("m/run", newOwner);
        assertNull(selected.get());
        monitor.stopAll();
    }

    @Test public void coreDiesDuringProbeCannotActivate() {
        AtomicBoolean alive = new AtomicBoolean(true);
        monitor.start("m/run", new Object(), alive::get, () -> { alive.set(false); return ready(12345); });
        monitor.poll();
        assertNull(selected.get());
    }
}
