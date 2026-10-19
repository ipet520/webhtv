package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.BuildConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppVersion {

    private static final Pattern BUILD_STAMP = Pattern.compile("(?<!\\d)(\\d{12,14})(?!\\d)");

    private AppVersion() {
    }

    public static String fullName() {
        String tag = BuildConfig.BUILD_TAG == null ? "" : BuildConfig.BUILD_TAG.trim();
        if (tag.isEmpty()) tag = BuildConfig.VERSION_NAME + "-" + BuildConfig.BUILD_TIME;
        return stripPrefix(tag);
    }

    public static boolean isCurrent(String name) {
        return stripPrefix(name).equals(stripPrefix(fullName()));
    }

    /** True only when a release tag sorts strictly after the currently installed tag. */
    public static boolean isNewer(String target) {
        return isNewer(fullName(), target);
    }

    /** Orders same-versionCode manifests by their trailing build timestamps. */
    public static boolean isNewer(String current, String target) {
        String currentTag = stripPrefix(current);
        String targetTag = stripPrefix(target);
        if (targetTag.isEmpty() || targetTag.equals(currentTag)) return false;
        String currentStamp = buildStamp(currentTag);
        String targetStamp = buildStamp(targetTag);
        // Preserve the old different-tag behavior for non-standard/local tags with no timestamp.
        if (currentStamp.isEmpty() || targetStamp.isEmpty()) return true;
        return comparableStamp(targetStamp).compareTo(comparableStamp(currentStamp)) > 0;
    }

    /** Returns the trailing yyyyMMddHHmm[ss] stamp, or an empty string when absent. */
    public static String buildStamp(String value) {
        if (value == null) return "";
        Matcher matcher = BUILD_STAMP.matcher(value);
        String stamp = "";
        while (matcher.find()) stamp = matcher.group(1);
        return stamp;
    }

    private static String comparableStamp(String stamp) {
        return stamp.length() == 12 ? stamp + "00" : stamp;
    }

    public static String stripPrefix(String value) {
        if (value == null) return "";
        value = value.trim();
        return value.startsWith("v") && value.length() > 1 && Character.isDigit(value.charAt(1)) ? value.substring(1) : value;
    }
}
