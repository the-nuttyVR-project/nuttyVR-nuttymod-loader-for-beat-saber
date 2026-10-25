package com.nuttyvr.common;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

/** A signature-protected, declarative mod registry. No remote or native code execution. */
public final class RuntimeProvider extends ContentProvider {
    public static final Uri URI = Uri.parse("content://com.nuttyvr.nuttymodruntime.mods/mods");
    private Store store;
    private static final String[] COLUMNS = {"id", "name", "version", "target", "targetVersion", "color", "speed", "enabled", "manifest"};
    private static final class Store extends SQLiteOpenHelper {
        Store(Context context) { super(context, "mods.db", null, 1); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE mods (id TEXT PRIMARY KEY, name TEXT NOT NULL, version TEXT NOT NULL, target TEXT NOT NULL, targetVersion TEXT NOT NULL, color TEXT NOT NULL, speed REAL NOT NULL, enabled INTEGER NOT NULL, manifest TEXT NOT NULL)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("Unsupported database upgrade"); }
    }
    @Override public boolean onCreate() { store = new Store(getContext()); return true; }
    private String id(Uri uri, boolean required) {
        if (!URI.getAuthority().equals(uri.getAuthority()) || uri.getPathSegments().isEmpty() || !"mods".equals(uri.getPathSegments().get(0)) || uri.getPathSegments().size() > 2) throw new IllegalArgumentException("Invalid registry URI");
        if (uri.getPathSegments().size() == 2) return ModRules.id(uri.getLastPathSegment());
        if (required) throw new IllegalArgumentException("Mod ID is required");
        return null;
    }
    @Override public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        String chosen = id(uri, false);
        if (selection != null || args != null) throw new IllegalArgumentException("Selection is unsupported");
        if (projection != null) for (String column : projection) {
            boolean known = false; for (String allowed : COLUMNS) if (allowed.equals(column)) known = true;
            if (!known) throw new IllegalArgumentException("Unknown column");
        }
        Cursor cursor = store.getReadableDatabase().query("mods", projection == null ? COLUMNS : projection, chosen == null ? null : "id=?", chosen == null ? null : new String[]{chosen}, null, null, "id ASC");
        cursor.setNotificationUri(getContext().getContentResolver(), URI);
        return cursor;
    }
    @Override public synchronized Uri insert(Uri uri, ContentValues values) {
        if (id(uri, false) != null) throw new IllegalArgumentException("Import into /mods");
        try {
            ModSpec spec = new ModSpec(values.getAsString("manifest"));
            SQLiteDatabase db = store.getWritableDatabase();
            db.beginTransaction();
            try {
                try (Cursor count = db.rawQuery("SELECT COUNT(*) FROM mods", null); Cursor existing = db.query("mods", new String[]{"id"}, "id=?", new String[]{spec.id}, null, null, null)) {
                    count.moveToFirst();
                    if (count.getInt(0) >= 64 && !existing.moveToFirst()) throw new IllegalArgumentException("Maximum 64 profiles; remove one first");
                }
                ContentValues row = new ContentValues();
                row.put("id", spec.id); row.put("name", spec.name); row.put("version", spec.version);
                row.put("target", spec.target); row.put("targetVersion", spec.targetVersion); row.put("color", spec.color);
                row.put("speed", spec.speed); row.put("enabled", 0); row.put("manifest", spec.json());
                db.insertWithOnConflict("mods", null, row, SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            changed(); return Uri.withAppendedPath(URI, spec.id);
        } catch (Exception error) { throw new IllegalArgumentException(error.getMessage(), error); }
    }
    @Override public synchronized int update(Uri uri, ContentValues values, String selection, String[] args) {
        String chosen = id(uri, true);
        if (selection != null || args != null || values.size() != 1 || !values.containsKey("enabled")) throw new IllegalArgumentException("Only enabled may be changed");
        Integer enabled = values.getAsInteger("enabled");
        if (enabled == null || (enabled != 0 && enabled != 1)) throw new IllegalArgumentException("Invalid enabled value");
        SQLiteDatabase db = store.getWritableDatabase();
        db.beginTransaction();
        int result;
        try {
            // Exactly one enabled profile per game and version makes precedence deterministic.
            if (enabled == 1) try (Cursor target = db.query("mods", new String[]{"target", "targetVersion"}, "id=?", new String[]{chosen}, null, null, null)) {
                if (target.moveToFirst()) {
                    ContentValues off = new ContentValues(); off.put("enabled", 0);
                    db.update("mods", off, "target=? AND targetVersion=?", new String[]{target.getString(0), target.getString(1)});
                }
            }
            result = db.update("mods", values, "id=?", new String[]{chosen});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        changed(); return result;
    }
    @Override public synchronized int delete(Uri uri, String selection, String[] args) {
        String chosen = id(uri, true);
        if (selection != null || args != null) throw new IllegalArgumentException("Selection is unsupported");
        int result = store.getWritableDatabase().delete("mods", "id=?", new String[]{chosen}); changed(); return result;
    }
    private void changed() { getContext().getContentResolver().notifyChange(URI, null); }
    @Override public String getType(Uri uri) { return id(uri, false) == null ? "vnd.android.cursor.dir/vnd.nuttyvr.mod" : "vnd.android.cursor.item/vnd.nuttyvr.mod"; }
}
