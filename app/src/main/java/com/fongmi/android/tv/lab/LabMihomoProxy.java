package com.fongmi.android.tv.lab;

import android.util.Log;
import android.util.Base64;

import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.google.gson.JsonObject;

import java.io.File;
import java.net.NetworkInterface;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Lab lifecycle adapter; never changes Setting.shell_proxy or site proxy lists. */
final class LabMihomoProxy {
    private static final String TAG = "MihomoProxy";
    private static ScheduledExecutorService worker;
    private static final MihomoProxyMonitor MONITOR = new MihomoProxyMonitor(proxy ->
            OkHttp.selector().setRuntimeProxy(proxy, proxy == null ? Set.of() : localAddresses()), (key, state) -> {
        Log.i(TAG, state);
        SpiderDebug.log("proxy", "mihomo %s", state);
    });

    private LabMihomoProxy() { }

    static boolean isMihomo(String name) { return "mihomo".equalsIgnoreCase(name); }

    static void saveLaunch(JsonObject entry, String command, File cwd) {
        // Private process recovery state, removed with the tracked process; never App Proxy preferences.
        entry.addProperty("mihomoCommand", command);
        entry.addProperty("mihomoCwd", cwd.getAbsolutePath());
        try {
            var launch = MihomoProxyConfig.Launch.command(command, cwd);
            entry.addProperty("mihomoConfigHash", Base64.encodeToString(launch.initialConfig(), Base64.NO_WRAP));
        } catch (Exception ignored) { }
    }

    static void recover(String key, JsonObject entry) {
        if (!entry.has("mihomoCommand") || !entry.has("mihomoCwd")) {
            if (entry.has("pkg") && isMihomo(entry.get("pkg").getAsString())) {
                Log.w(TAG, "Recovered legacy Mihomo has no launch metadata; restart core to enable automatic routing");
            }
            return;
        }
        try {
            var launch = MihomoProxyConfig.Launch.command(entry.get("mihomoCommand").getAsString(), new File(entry.get("mihomoCwd").getAsString()));
            if (!entry.has("mihomoConfigHash")) { Log.w(TAG, "Recovered Mihomo has no config fingerprint; restart core for automatic routing"); return; }
            launch = new MihomoProxyConfig.Launch(launch.config(), launch.controller(), launch.secret(),
                    Base64.decode(entry.get("mihomoConfigHash").getAsString(), Base64.DEFAULT));
            activate(key, null, launch);
        } catch (Exception e) { Log.w(TAG, "Recovered Mihomo launch cannot be verified; automatic routing inactive"); }
    }

    static synchronized void start(String key, String command, File cwd, Process owner) {
        try {
            if (owner != null && LabRunner.getProcess(key) != owner) return;
            MihomoProxyConfig.Launch launch = MihomoProxyConfig.Launch.command(command, cwd);
            activate(key, owner, launch);
        } catch (Exception e) {
            Log.w(TAG, "Automatic binding unavailable for this launch (" + e.getClass().getSimpleName() + "); existing policy retained");
        }
    }

    private static synchronized void activate(String key, Process owner, MihomoProxyConfig.Launch launch) {
        var detector = new MihomoProxyConfig.Detector(launch);
        MONITOR.start(key, owner, () -> LabRunner.isRunning(key), () -> {
            if (OkHttp.selector().getRuntimeProxy() == null) detector.invalidate();
            return detector.detect();
        });
        if (worker == null) {
            worker = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "mihomo-app-proxy");
                thread.setDaemon(true);
                return thread;
            });
            worker.scheduleWithFixedDelay(() -> {
                try { MONITOR.poll(); }
                catch (Exception e) { Log.w(TAG, "Monitor failure; revoke runtime binding", e); MONITOR.stopAll(); }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    static void stop(String key) { MONITOR.stop(key); }
    static void stopAll() { MONITOR.stopAll(); }
    static void processExited(String key, Process owner) { MONITOR.processExited(key, owner); }

    private static Set<String> localAddresses() {
        Set<String> addresses = new HashSet<>();
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                var values = interfaces.nextElement().getInetAddresses();
                while (values.hasMoreElements()) {
                    String host = values.nextElement().getHostAddress();
                    if (host != null) addresses.add(host.toLowerCase(Locale.ROOT).split("%", 2)[0]);
                }
            }
        } catch (Exception ignored) { }
        return addresses;
    }
}
