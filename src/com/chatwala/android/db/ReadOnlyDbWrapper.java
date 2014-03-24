package com.chatwala.android.db;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.CancellationSignal;

public class ReadOnlyDbWrapper {
    private SQLiteDatabase db;

    /*package*/ ReadOnlyDbWrapper(SQLiteDatabase db) {
        this.db = db;
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
        return db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, CancellationSignal cancellationSignal) {
        return db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, cancellationSignal);
    }
}
