package com.nuttyvr.common;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayDeque;

/** A persisted queue of user-confirmed Android installations. Cancellation stops the queue. */
public final class Installer {
    public static final int INSTALL = 401, PERMISSION = 402;
    public interface Listener { void status(String message); void changed(); }
    private final Activity activity;
    private final Listener listener;
    private final SharedPreferences state;
    private final ArrayDeque<String> queue = new ArrayDeque<>();
    private String current = "";
    private boolean preparing;
    private boolean requestingPermission;
    public Installer(Activity activity, Listener listener) {
        this.activity = activity; this.listener = listener;
        state = activity.getSharedPreferences("installation", 0);
        String pending = state.getString("queue", "");
        for (String role : pending.split(",")) if (allowed(role)) queue.add(role);
        String active = state.getString("current", "");
        if (allowed(active)) current = active;
    }
    private boolean allowed(String role) {
        if ("hub".equals(Catalog.role(activity))) return "loader".equals(role);
        if ("loader".equals(Catalog.role(activity))) for (String item : Catalog.DEPENDENCIES) if (item.equals(role)) return true;
        return false;
    }
    public boolean pending() { return !current.isEmpty() || !queue.isEmpty(); }
    private void save() { state.edit().putString("queue", String.join(",", queue)).putString("current", current).commit(); }
    public void start(String[] roles) {
        if (preparing || requestingPermission || pending()) { listener.status("An installation is pending. Use Resume installation or Stop queue."); return; }
        for (String role : roles) if (allowed(role) && !Catalog.ready(activity, role)) queue.add(role);
        save(); next();
    }
    public void resume() {
        if (preparing || requestingPermission) return;
        if (!current.isEmpty()) {
            if (!Catalog.ready(activity, current)) queue.addFirst(current);
            current = "";
        }
        save(); next();
    }
    public void stop() {
        if (preparing) { listener.status("APK preparation is running. Stop the queue after it finishes."); return; }
        queue.clear(); current = ""; save(); listener.status("Queue stopped. Completed installations are kept."); listener.changed();
    }
    private void next() {
        while (!queue.isEmpty() && Catalog.ready(activity, queue.peek())) queue.remove();
        save();
        if (queue.isEmpty()) { listener.status("All selected APKs are installed and their signing certificates match."); listener.changed(); return; }
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            listener.status("Allow APK installation for this app, then return here.");
            try {
                requestingPermission = true;
                activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName())), PERMISSION);
            } catch (ActivityNotFoundException | SecurityException error) {
                requestingPermission = false; listener.status("This headset does not expose install permission settings. Install the APKs from a computer instead.");
            }
            return;
        }
        current = queue.remove(); save(); preparing = true;
        final String chosen = current;
        listener.status("Preparing " + Catalog.filename(chosen) + "…");
        new Thread(() -> {
            try {
                File directory = new File(activity.getFilesDir(), "installers");
                if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create APK staging directory");
                File temporary = new File(directory, "pending.tmp"), apk = new File(directory, Catalog.filename(chosen));
                try (InputStream input = activity.getAssets().open("apks/" + Catalog.filename(chosen)); FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[16384]; int read; long size = 0;
                    while ((read = input.read(buffer)) != -1) {
                        size += read; if (size > 64L * 1024 * 1024) throw new IOException("APK exceeds 64 MiB");
                        output.write(buffer, 0, read);
                    }
                    output.getFD().sync();
                }
                PackageInfo info = activity.getPackageManager().getPackageArchiveInfo(temporary.getAbsolutePath(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (info == null || !(Catalog.PREFIX + chosen).equals(info.packageName) || info.getLongVersionCode() != 1 || !Catalog.trusted(activity, info)) throw new IOException("Bundled APK identity, version or certificate does not match");
                if (!temporary.renameTo(apk)) throw new IOException("Cannot stage verified APK");
                activity.runOnUiThread(() -> {
                    preparing = false;
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    Uri uri = new Uri.Builder().scheme("content").authority(activity.getPackageName() + ".apks").appendPath(apk.getName()).build();
                    Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_RETURN_RESULT, true);
                    intent.setClipData(ClipData.newRawUri("Verified APK", uri));
                    try {
                        listener.status("Confirm installation of " + Catalog.label(chosen) + " in Android.");
                        activity.startActivityForResult(intent, INSTALL);
                    } catch (ActivityNotFoundException | SecurityException error) { failed("System installer unavailable. Install this APK from a computer."); }
                });
            } catch (Exception error) { activity.runOnUiThread(() -> { preparing = false; failed("Preparation failed: " + error.getMessage()); }); }
        }, "nuttyvr-apk-preparation").start();
    }
    private void failed(String message) { current = ""; queue.clear(); save(); listener.status(message); listener.changed(); }
    public void result(int request) {
        if (request == PERMISSION) {
            requestingPermission = false;
            if (activity.getPackageManager().canRequestPackageInstalls()) next();
            else failed("Install permission was not granted. Nothing else was installed.");
        } else if (request == INSTALL && !current.isEmpty()) {
            if (Catalog.ready(activity, current)) { current = ""; save(); listener.changed(); next(); }
            else failed("Installation cancelled or failed. Tap Install to try again.");
        }
    }
}
