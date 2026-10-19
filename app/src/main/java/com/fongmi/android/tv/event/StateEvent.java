package com.fongmi.android.tv.event;

import org.greenrobot.eventbus.EventBus;

public record StateEvent(Type type, String message) {

    public StateEvent(Type type) {
        this(type, null);
    }

    public static void empty() {
        EventBus.getDefault().post(new StateEvent(Type.EMPTY, null));
    }

    public static void noConfig() {
        EventBus.getDefault().post(new StateEvent(Type.NO_CONFIG, null));
    }

    public static void progress() {
        EventBus.getDefault().post(new StateEvent(Type.PROGRESS, null));
    }

    public static void content() {
        EventBus.getDefault().post(new StateEvent(Type.CONTENT, null));
    }

    public static void error(String message) {
        EventBus.getDefault().post(new StateEvent(Type.ERROR, message));
    }

    public enum Type {
        EMPTY, PROGRESS, CONTENT, NO_CONFIG, ERROR
    }
}
