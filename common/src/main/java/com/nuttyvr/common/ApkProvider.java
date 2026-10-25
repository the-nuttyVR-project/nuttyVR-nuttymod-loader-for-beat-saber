package com.nuttyvr.common;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class ApkProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    private File file(Uri uri) throws IOException {
        if (!(getContext().getPackageName() + ".apks").equals(uri.getAuthority()) || uri.getPathSegments().size() != 1) throw new FileNotFoundException("Invalid APK URI");
        String name = uri.getLastPathSegment();
        boolean allowed = false;
        String owner = Catalog.role(getContext());
        if ("hub".equals(owner)) allowed = Catalog.filename("loader").equals(name);
        if ("loader".equals(owner)) for (String role : Catalog.DEPENDENCIES) if (Catalog.filename(role).equals(name)) allowed = true;
        if (!allowed) throw new FileNotFoundException("APK not shared");
        File directory = new File(getContext().getFilesDir(), "installers").getCanonicalFile();
        File apk = new File(directory, name).getCanonicalFile();
        if (!directory.equals(apk.getParentFile()) || !apk.isFile()) throw new FileNotFoundException("APK not available");
        return apk;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read access only");
        try { return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY); }
        catch (IOException error) { throw new FileNotFoundException(error.getMessage()); }
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns);
        try {
            File apk = file(uri); Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) row[i] = OpenableColumns.DISPLAY_NAME.equals(columns[i]) ? apk.getName() : OpenableColumns.SIZE.equals(columns[i]) ? apk.length() : null;
            cursor.addRow(row);
        } catch (IOException ignored) { /* Invalid URIs expose no metadata. */ }
        return cursor;
    }
    @Override public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
}
