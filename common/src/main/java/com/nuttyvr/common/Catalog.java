package com.nuttyvr.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.util.Arrays;

public final class Catalog {
    public static final String PREFIX = "com.nuttyvr.";
    public static final String[] DEPENDENCIES = {"rootmode", "rootmodelib", "nuttymod", "vnedruntime", "nuttymodruntime"};
    public static final String[] ALL = {"hub", "loader", "rootmode", "rootmodelib", "nuttymod", "vnedruntime", "nuttymodruntime", "demo"};
    private Catalog() {}
    public static String role(Context context) { return context.getPackageName().substring(PREFIX.length()); }
    public static String label(String role) {
        switch (role) {
            case "hub": return "nuttyVR Hub";
            case "loader": return "nuttymod Loader for VR";
            case "rootmode": return "Root Mode";
            case "rootmodelib": return "Root Mode Library";
            case "nuttymod": return "nuttymod";
            case "vnedruntime": return "VNED Runtime";
            case "nuttymodruntime": return "nuttymod Runtime";
            case "demo": return "nuttyVR Rhythm Demo";
            default: throw new IllegalArgumentException("Unknown module");
        }
    }
    public static String filename(String role) {
        if ("hub".equals(role)) return "nuttyVR hub.apk";
        if ("loader".equals(role)) return "nuttymod loader for __vr.apk";
        if ("demo".equals(role)) return "nuttyVR demo game.apk";
        for (String item : DEPENDENCIES) if (item.equals(role)) return role + ".apk";
        throw new IllegalArgumentException("Unknown module");
    }
    public static PackageInfo info(Context context, String role) {
        try { return context.getPackageManager().getPackageInfo(PREFIX + role, PackageManager.GET_SIGNING_CERTIFICATES); }
        catch (PackageManager.NameNotFoundException ignored) { return null; }
    }
    public static boolean trusted(Context context, PackageInfo info) {
        if (info == null || info.signingInfo == null) return false;
        try {
            PackageInfo own = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            Signature[] wanted = own.signingInfo.getApkContentsSigners();
            Signature[] actual = info.signingInfo.getApkContentsSigners();
            return wanted.length == 1 && actual.length == 1 && Arrays.equals(wanted[0].toByteArray(), actual[0].toByteArray());
        } catch (PackageManager.NameNotFoundException ignored) { return false; }
    }
    public static boolean ready(Context context, String role) {
        PackageInfo info = info(context, role);
        return info != null && info.getLongVersionCode() >= 1 && trusted(context, info);
    }
    public static boolean dependenciesReady(Context context) {
        for (String role : DEPENDENCIES) if (!ready(context, role)) return false;
        return true;
    }
}
