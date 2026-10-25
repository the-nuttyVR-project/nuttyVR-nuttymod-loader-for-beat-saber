package com.nuttyvr.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

/** Copy this client, ModRules and ModSpec into an opt-in Android game. */
public final class NuttyModClient {
    private NuttyModClient() {}
    public static final class Entry {
        public final ModSpec spec;
        public final boolean enabled;
        Entry(ModSpec spec, boolean enabled) { this.spec = spec; this.enabled = enabled; }
    }
    public static List<Entry> list(Context context) throws Exception {
        // PackageManager's signature permission is the final authority at the provider boundary.
        List<Entry> entries = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(RuntimeProvider.URI, new String[]{"manifest", "enabled"}, null, null, null)) {
            if (cursor == null) throw new IllegalStateException("Install nuttymodruntime.apk first");
            while (cursor.moveToNext()) entries.add(new Entry(new ModSpec(cursor.getString(0)), cursor.getInt(1) == 1));
        }
        return entries;
    }
    public static ModSpec activeForThisGame(Context context) throws Exception {
        PackageInfo own = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        for (Entry entry : list(context)) if (entry.enabled && ModRules.compatible(entry.spec.target, entry.spec.targetVersion, context.getPackageName(), own.versionName)) return entry.spec;
        return null;
    }
}
