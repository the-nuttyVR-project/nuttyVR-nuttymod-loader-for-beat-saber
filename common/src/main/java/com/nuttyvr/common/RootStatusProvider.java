package com.nuttyvr.common;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Process;

/** Passive device diagnostics. It neither requests nor grants root. */
public final class RootStatusProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (!"/status".equals(uri.getPath())) throw new IllegalArgumentException("Expected /status");
        MatrixCursor cursor = new MatrixCursor(new String[]{"model", "sdk", "uid", "rootGranted"});
        cursor.addRow(new Object[]{Build.MODEL, Build.VERSION.SDK_INT, Process.myUid(), Process.myUid() == 0 ? 1 : 0});
        return cursor;
    }
    @Override public String getType(Uri uri) { return "vnd.android.cursor.item/vnd.nuttyvr.rootstatus"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
}
