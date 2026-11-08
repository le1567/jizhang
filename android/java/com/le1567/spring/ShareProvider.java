package com.le1567.spring;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

public class ShareProvider extends ContentProvider {

    public static final String AUTHORITY = "com.le1567.spring.share";

    public static Uri uriFor(File f) {
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(f.getName())
                .build();
    }

    @Override
    public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        File f = new File(new File(getContext().getCacheDir(), "share"), name);
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) { return "image/png"; }

    @Override
    public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }

    @Override
    public Uri insert(Uri uri, ContentValues v) { return null; }

    @Override
    public int delete(Uri uri, String s, String[] a) { return 0; }

    @Override
    public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
