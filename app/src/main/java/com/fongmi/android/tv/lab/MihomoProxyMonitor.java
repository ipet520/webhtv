package com.fongmi.android.tv.lab;

import java.net.Proxy;
import java.util.function.BooleanSupplier;

/** One runtime binding; slow I/O never holds the lifecycle lock. Late probes cannot resurrect a session. */
final class MihomoProxyMonitor {
    interface Probe { MihomoProxyConfig.Ready detect() throws Exception; }
    interface Binding { boolean apply(Proxy proxy); }
    interface Events { void record(String key, String state); }

    private record Session(String key, Object owner, BooleanSupplier alive, Probe probe) { }

    private final Binding binding;
    private final Events events;
    private Session current;
    private String lastState = "";

    MihomoProxyMonitor(Binding binding, Events events) {
        this.binding = binding;
        this.events = events;
    }

    synchronized void start(String key, Object owner, BooleanSupplier alive, Probe probe) {
        current = new Session(key, owner, alive, probe);
        lastState = "";
        transition(null, "STARTING; original proxy policy until ready");
    }

    synchronized void stop(String key) {
        if (current == null || !current.key().equals(key)) return;
        transition(null, "STOPPED; restored original proxy policy");
        current = null;
    }

    synchronized void stopAll() {
        if (current == null) return;
        stop(current.key());
    }

    synchronized void processExited(String key, Object owner) {
        if (current == null || !current.key().equals(key) || current.owner() != owner) return;
        // A shell may have daemonized: allow revalidation once ProcManager confirms the surviving group.
        transition(null, "PROCESS_EXIT; binding revoked pending process/group revalidation");
    }

    void poll() {
        Session session;
        synchronized (this) { session = current; }
        if (session == null) return;
        MihomoProxyConfig.Ready ready = null;
        String state;
        try {
            if (!session.alive().getAsBoolean()) state = "CORE_NOT_RUNNING; restored original proxy policy";
            else {
                ready = session.probe().detect();
                state = "READY source=" + ready.source() + " proxy=" + ready.proxy();
            }
        } catch (Exception e) {
            String reason = e instanceof java.io.IOException ? e.getMessage() : e.getClass().getSimpleName();
            state = "UNREADY reason=" + reason + "; restored original proxy policy";
        }
        synchronized (this) {
            if (current != session) return;
            if (ready != null && !session.alive().getAsBoolean()) {
                ready = null;
                state = "CORE_NOT_RUNNING; restored original proxy policy";
            }
            transition(ready == null ? null : ready.proxy(), state);
        }
    }

    private void transition(Proxy proxy, String state) {
        boolean changed = binding.apply(proxy);
        if (changed || !state.equals(lastState)) events.record(current.key(), state);
        lastState = state;
    }
}
