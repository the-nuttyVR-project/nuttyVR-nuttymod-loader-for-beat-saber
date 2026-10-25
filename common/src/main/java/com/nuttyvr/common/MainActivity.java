package com.nuttyvr.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity implements Installer.Listener {
    private static final int IMPORT = 403;
    private String role, tab = "home", statusMessage = "Ready.", rootResult = "Root access has not been checked.";
    private Installer installer;
    private TextView statusView;
    private LinearLayout page;
    private boolean rootChecking;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); role = Catalog.role(this);
        if (state != null) { tab = state.getString("tab", "home"); statusMessage = state.getString("status", "Ready."); }
        installer = new Installer(this, this);
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); state.putString("tab", tab); state.putString("status", statusMessage); }
    @Override public void status(String message) { statusMessage = message; if (statusView != null) statusView.setText(message); }
    @Override public void changed() { if (!isFinishing() && !isDestroyed()) render(); }
    private void show(String next) { tab = next; render(); }
    private void render() {
        page = Ui.page(this, Catalog.label(role), "Your headset. Your setup. Your mods.");
        statusView = Ui.text(page, statusMessage, 15, Ui.MINT); statusView.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        if ("loader".equals(role)) {
            LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); page.addView(nav);
            for (String name : new String[]{"home", "dependencies", "games", "mods", "guide"}) {
                android.widget.Button button = new android.widget.Button(this); button.setAllCaps(false); button.setText(name.substring(0, 1).toUpperCase() + name.substring(1)); button.setTextSize(13);
                nav.addView(button, new LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1)); button.setOnClickListener(v -> show(name));
            }
        }
        if ("guide".equals(tab)) guide();
        else switch (role) {
            case "hub": hub(); break;
            case "loader":
                if ("dependencies".equals(tab)) dependencies(); else if ("games".equals(tab)) games(); else if ("mods".equals(tab)) mods(); else home();
                break;
            case "nuttymod": case "nuttymodruntime": case "vnedruntime": mods(); break;
            case "rootmode": case "rootmodelib": root(); break;
            default: status("Unknown module");
        }
        if (installer.pending()) {
            LinearLayout pending = Ui.panel(page); Ui.text(pending, "Installation queue saved", 20, Ui.WHITE);
            Ui.text(pending, "After returning from Android’s installer, resume if installation did not continue. A cancelled APK can be retried.", 16, Ui.MUTED);
            Ui.button(pending, "Resume installation", true, installer::resume);
            Ui.button(pending, "Stop queue", false, installer::stop);
        }
        Ui.text(page, "nuttyVR   /   nuttymod loader   /   0.1.0 development build", 13, Ui.MUTED);
    }
    private void hub() {
        boolean connected = getPreferences(0).getBoolean("localDevice", false);
        LinearLayout device = Ui.panel(page);
        Ui.text(device, "01 / Connect your headset", 23, Ui.WHITE);
        Ui.text(device, "Run this Hub on the headset you want to set up. Connect this headset creates a local session on this device.", 17, Ui.MUTED);
        Ui.text(device, Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE + (connected ? " · Local session connected" : " · Not connected"), 18, Ui.MINT);
        Ui.button(device, connected ? "Reconnect this headset" : "Connect this headset", true, () -> {
            getPreferences(0).edit().putBoolean("localDevice", true).apply(); status("Connected to this Android device: " + Build.MODEL + ". Remote headset pairing is not implemented."); render();
        });
        LinearLayout loader = Ui.panel(page); Ui.text(loader, "02 / Install nuttymod Loader", 23, Ui.WHITE);
        Ui.text(loader, "The loader APK is included in the Hub. Confirm the Android installation and wait until it finishes.", 17, Ui.MUTED);
        Ui.button(loader, Catalog.ready(this, "loader") ? "Launch nuttymod Loader" : "Install nuttymod Loader for VR", true, () -> {
            if (!getPreferences(0).getBoolean("localDevice", false)) { status("Connect this headset first."); return; }
            if (Catalog.ready(this, "loader")) launch("loader"); else installer.start(new String[]{"loader"});
        });
        Ui.button(loader, "Read the installation guide", false, () -> show("guide"));
    }
    private void home() {
        int count = 0; for (String item : Catalog.DEPENDENCIES) if (Catalog.ready(this, item)) count++;
        LinearLayout summary = Ui.panel(page); Ui.text(summary, "Make room for more play.", 28, Ui.WHITE);
        Ui.text(summary, count + " / 5 dependencies installed", 22, Ui.MINT);
        Ui.text(summary, "Install the companion apps, add a profile, then open a compatible game. All five APKs are bundled for offline installation.", 17, Ui.MUTED);
        Ui.button(summary, "Install all dependencies", true, () -> installer.start(Catalog.DEPENDENCIES));
        Ui.button(summary, "Manage dependencies", false, () -> show("dependencies"));
        LinearLayout compatibility = Ui.panel(page); Ui.text(compatibility, "Game compatibility", 23, Ui.WHITE);
        Ui.text(compatibility, "Rhythm Demo: working NuttyMod integration included in the project.\nBeat Saber: game-specific adapter required; this build does not patch or inject into it.", 17, Ui.MUTED);
        Ui.button(compatibility, "View games & compatibility", false, () -> show("games"));
        Ui.button(compatibility, "Manage mod profiles", false, () -> show("mods"));
    }
    private void dependencies() {
        LinearLayout install = Ui.panel(page); Ui.text(install, "Install all dependencies", 23, Ui.WHITE);
        Ui.text(install, "Android asks you to confirm each APK. Cancelling stops the queue. Root Mode checks existing root access; installing it does not root a headset.", 17, Ui.MUTED);
        Ui.button(install, "Install all dependencies", true, () -> installer.start(Catalog.DEPENDENCIES));
        for (String item : Catalog.DEPENDENCIES) {
            PackageInfo info = Catalog.info(this, item); boolean ready = Catalog.ready(this, item);
            LinearLayout card = Ui.panel(page); Ui.text(card, Catalog.filename(item), 22, Ui.WHITE);
            String message = info == null ? "Missing" : ready ? "Installed · " + info.versionName : "Installed package has an incompatible certificate or version";
            Ui.text(card, message + "\n" + purpose(item), 16, ready ? Ui.MINT : Ui.MUTED);
            Ui.button(card, ready ? "Open " + Catalog.label(item) : "Install " + Catalog.label(item), false, () -> { if (Catalog.ready(this, item)) launch(item); else installer.start(new String[]{item}); });
        }
    }
    private String purpose(String item) {
        switch (item) {
            case "rootmode": return "Check an existing su installation on an already rooted device.";
            case "rootmodelib": return "Read device diagnostics through a signed companion API.";
            case "nuttymod": return "Import, enable, disable and remove game profiles.";
            case "vnedruntime": return "Validate declarative nuttymod/1 profiles before adding them.";
            case "nuttymodruntime": return "Store profiles and supply them to integrated games.";
            default: return "";
        }
    }
    private void games() {
        LinearLayout demo = Ui.panel(page); Ui.text(demo, "nuttyVR Rhythm Demo", 23, Ui.WHITE);
        Ui.text(demo, "Compatible · 0.1.0\nTap the falling beats. Enable Mint Rush to make them faster and mint green. Install nuttyVR demo game.apk from the project download first.", 17, Ui.MUTED);
        Ui.button(demo, "Open Rhythm Demo", true, () -> launch("demo"));
        Ui.button(demo, "Add Mint Rush profile", false, () -> review(ModSpec.example()));
        LinearLayout saber = Ui.panel(page); Ui.text(saber, "Beat Saber", 23, Ui.WHITE);
        String version = "Not installed";
        try { version = "Detected version " + getPackageManager().getPackageInfo("com.beatgames.beatsaber", 0).versionName; } catch (PackageManager.NameNotFoundException ignored) { }
        Ui.text(saber, version + " · Adapter required", 18, Ui.MINT);
        Ui.text(saber, "Installing these companions does not inject into Beat Saber. An adapter must match the game version and its loader. Root access alone does not establish compatibility.", 17, Ui.MUTED);
        Ui.button(saber, "Open Beat Saber", false, () -> openPackage("com.beatgames.beatsaber"));
        Ui.button(saber, "Open Beat Saber modding guide", false, () -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bsmg.wiki/quest-modding.html"))); }
            catch (ActivityNotFoundException error) { status("No browser is installed."); }
        });
    }
    private void mods() {
        LinearLayout intro = Ui.panel(page);
        Ui.text(intro, "vnedruntime".equals(role) ? "Validate & import a profile" : "Your mod profiles", 24, Ui.WHITE);
        Ui.text(intro, "Declarative profiles use the nuttymod/1 format. Integrated games read their color and speed settings. Native .qmod files and arbitrary scripts require a separate adapter.", 17, Ui.MUTED);
        if (!Catalog.ready(this, "nuttymodruntime")) { Ui.text(intro, "Install nuttymodruntime.apk from the Loader first.", 18, Ui.MINT); return; }
        Ui.button(intro, "Import .nuttymod / JSON profile", true, () -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            try { startActivityForResult(pick, IMPORT); } catch (ActivityNotFoundException error) { status("No document picker is available. Use the built-in example profile."); }
        });
        Ui.button(intro, "Add Mint Rush example", false, () -> review(ModSpec.example()));
        try {
            java.util.List<NuttyModClient.Entry> entries = NuttyModClient.list(this);
            if (entries.isEmpty()) Ui.text(intro, "No profiles yet. Add the example to try the demo.", 17, Ui.MUTED);
            for (NuttyModClient.Entry entry : entries) {
                ModSpec spec = entry.spec;
                LinearLayout card = Ui.panel(page); Ui.text(card, spec.name, 22, Ui.WHITE);
                Ui.text(card, (entry.enabled ? "Enabled" : "Disabled") + " · v" + spec.version + "\n" + spec.target + " · " + spec.targetVersion + "\nColor " + spec.color + " · Speed " + spec.speed + "×", 16, entry.enabled ? Ui.MINT : Ui.MUTED);
                Ui.button(card, entry.enabled ? "Disable profile" : "Enable profile", !entry.enabled, () -> {
                    try {
                        ContentValues values = new ContentValues(); values.put("enabled", entry.enabled ? 0 : 1);
                        getContentResolver().update(Uri.withAppendedPath(RuntimeProvider.URI, spec.id), values, null, null);
                        status(entry.enabled ? "Profile disabled. Reopen the game to apply." : "Profile enabled for its exact target game version. Reopen the game to apply."); render();
                    } catch (Exception error) { status("Could not update profile: " + error.getMessage()); }
                });
                Ui.button(card, "Remove profile", false, () -> new AlertDialog.Builder(this).setTitle("Remove " + spec.name + "?")
                    .setMessage("The imported profile will be removed from the runtime.").setNegativeButton("Keep", null).setPositiveButton("Remove", (dialog, which) -> {
                        try { getContentResolver().delete(Uri.withAppendedPath(RuntimeProvider.URI, spec.id), null, null); status("Profile removed."); render(); }
                        catch (Exception error) { status("Could not remove profile: " + error.getMessage()); }
                    }).show());
            }
        } catch (Exception error) { Ui.text(intro, "Runtime unavailable: " + error.getMessage(), 17, Ui.MUTED); }
    }
    private void review(String data) {
        try {
            ModSpec spec = new ModSpec(data);
            if (!Catalog.ready(this, "nuttymodruntime")) { status("Install nuttymodruntime.apk first."); return; }
            new AlertDialog.Builder(this).setTitle("Import " + spec.name + "?")
                .setMessage("Game: " + spec.target + "\nExact game version: " + spec.targetVersion + "\nColor: " + spec.color + "\nSpeed: " + spec.speed + "×\n\nImporting an existing ID replaces it and disables it until you choose Enable.")
                .setNegativeButton("Cancel", null).setPositiveButton("Import", (dialog, which) -> {
                    try {
                        ContentValues values = new ContentValues(); values.put("manifest", spec.json());
                        getContentResolver().insert(RuntimeProvider.URI, values);
                        status("Imported " + spec.name + ". Enable it in Mod profiles."); tab = "mods"; render();
                    } catch (Exception error) { status("Import failed: " + error.getMessage()); }
                }).show();
        } catch (Exception error) { status("Invalid profile: " + error.getMessage()); }
    }
    private void root() {
        LinearLayout diagnostics = Ui.panel(page); Ui.text(diagnostics, "Device diagnostics", 24, Ui.WHITE);
        Ui.text(diagnostics, Build.MANUFACTURER + " " + Build.MODEL + "\nAndroid " + Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT + "\nApp UID: " + android.os.Process.myUid(), 18, Ui.MUTED);
        Ui.text(diagnostics, "These APKs do not grant root or alter the boot image. Root Mode can test an existing su installation after you request it.", 17, Ui.MUTED);
        if ("rootmode".equals(role)) {
            Ui.text(diagnostics, rootResult, 17, Ui.MINT);
            Ui.button(diagnostics, rootChecking ? "Checking…" : "Check existing root access", true, this::checkRoot);
            Ui.button(diagnostics, "Read Root Mode Library status", false, () -> {
                try (Cursor cursor = getContentResolver().query(Uri.parse("content://com.nuttyvr.rootmodelib.status/status"), null, null, null, null)) {
                    if (cursor == null || !cursor.moveToFirst()) throw new IllegalStateException("Install rootmodelib.apk first");
                    status("Library: " + cursor.getString(0) + " · API " + cursor.getInt(1) + " · provider UID " + cursor.getInt(2) + " · provider runs as " + (cursor.getInt(3) == 1 ? "root" : "ordinary app"));
                } catch (Exception error) { status("Library unavailable: " + error.getMessage()); }
            });
        } else Ui.text(diagnostics, "The signed read-only API is available at content://com.nuttyvr.rootmodelib.status/status.", 16, Ui.MINT);
    }
    private void checkRoot() {
        if (rootChecking) return;
        rootChecking = true; rootResult = "Waiting for existing su authorization (up to 10 seconds)…"; render();
        new Thread(() -> {
            java.lang.Process process = null;
            String result;
            try {
                process = new ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start();
                if (!process.waitFor(10, TimeUnit.SECONDS)) { process.destroyForcibly(); result = "su did not respond. Root access was not confirmed."; }
                else {
                    byte[] bytes = new byte[4096]; int count;
                    try (InputStream input = process.getInputStream()) { count = input.read(bytes); }
                    String output = count < 0 ? "" : new String(bytes, 0, count, StandardCharsets.UTF_8);
                    result = process.exitValue() == 0 && output.startsWith("uid=0(") ? "Existing su ran id with UID 0. This app still runs in its normal sandbox." : "Root access was not granted. " + output.trim();
                }
            } catch (Exception error) { result = "No usable existing root access: " + error.getMessage(); }
            finally { if (process != null) process.destroy(); }
            final String message = result;
            runOnUiThread(() -> { rootChecking = false; rootResult = message; if (!isFinishing() && !isDestroyed()) render(); });
        }, "nuttyvr-root-check").start();
    }
    private void guide() {
        LinearLayout card = Ui.panel(page); Ui.text(card, "Get set up in five steps", 25, Ui.WHITE);
        Ui.text(card, "1. Download nuttyVR hub.apk and sideload it onto your headset.\n\n2. Open nuttyVR Hub on that headset and choose Connect this headset.\n\n3. Select Install nuttymod Loader for VR and approve Android’s install prompt.\n\n4. Wait for installation to finish, then launch nuttymod Loader.\n\n5. Select Install all dependencies. Approve rootmode.apk, rootmodelib.apk, nuttymod.apk, vnedruntime.apk and nuttymodruntime.apk in order.", 18, Ui.WHITE);
        Ui.text(card, "Try it: install nuttyVR demo game.apk, import the Mint Rush example, enable it, and open the demo. Beat Saber still requires a matching game adapter. This Hub connects to the device it runs on; phone-to-headset pairing is not implemented.", 17, Ui.MUTED);
        Ui.button(card, "Back to " + Catalog.label(role), true, () -> show("home"));
    }
    private void launch(String target) {
        if (!Catalog.ready(this, target)) { status("Install the matching " + Catalog.filename(target) + " first."); return; }
        openPackage(Catalog.PREFIX + target);
    }
    private void openPackage(String target) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(target);
        if (intent == null) intent = getPackageManager().getLeanbackLaunchIntentForPackage(target);
        if (intent == null) {
            Intent vr = new Intent(Intent.ACTION_MAIN).addCategory("com.oculus.intent.category.VR").setPackage(target);
            if (vr.resolveActivity(getPackageManager()) != null) intent = vr;
        }
        try { if (intent != null) startActivity(intent); else status("App not installed or no launcher is available: " + target); }
        catch (ActivityNotFoundException | SecurityException error) { status("Could not open app: " + error.getMessage()); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != IMPORT) { installer.result(request); return; }
        if (result != RESULT_OK || data == null || data.getData() == null) { status("Profile import cancelled."); return; }
        final Uri uri = data.getData(); status("Reading profile…");
        new Thread(() -> {
            try (InputStream input = getContentResolver().openInputStream(uri); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (input == null) throw new IllegalArgumentException("Cannot open selected document");
                byte[] buffer = new byte[4096]; int read;
                while ((read = input.read(buffer)) != -1) { if (output.size() + read > ModSpec.MAX_BYTES) throw new IllegalArgumentException("Profile exceeds 64 KiB"); output.write(buffer, 0, read); }
                String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(output.toByteArray())).toString();
                new ModSpec(json);
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) review(json); });
            } catch (Exception error) { runOnUiThread(() -> status("Cannot import profile: " + error.getMessage())); }
        }, "nuttyvr-profile-import").start();
    }
}
