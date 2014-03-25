package com.chatwala.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.chatwala.android.util.Logger;

import java.util.concurrent.Semaphore;

/**
 * Created by Eliezer on 3/20/2014.
 */
public class DBHelper {
    private DBHelper() {}

    private static class Singleton {
        private static DBHelper instance = new DBHelper();
    }

    private static class DbAcquisitionException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = -1281117613671957582L;
    }

    public static final int DB_VERSION = 1;

    private CwSQLiteOpenHelper myOpenHelperInstance = null;
    private final Semaphore mutex = new Semaphore(0, true);

    public static void initInstance(Context context) {
        if(Singleton.instance.myOpenHelperInstance == null) {
            Singleton.instance.myOpenHelperInstance = new CwSQLiteOpenHelper(Singleton.instance, context);
            getInstance().myOpenHelperInstance.getWritableDatabase();
        }
    }

    private static DBHelper getInstance() {
        return Singleton.instance;
    }

    private static class CwSQLiteOpenHelper extends SQLiteOpenHelper {
        private Context context;
        private DBHelper dbHelper;

        public CwSQLiteOpenHelper(DBHelper dbHelper, Context context) {
            super(context, "chatwala20.db", null, DB_VERSION);
            this.context = context;
            this.dbHelper = dbHelper;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            dbHelper.mutex.release();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            for(int i = oldVersion + 1; i <= newVersion; i++) {
                DBVersionUpdates.doVersionUpdate(context, db, i);
            }
        }

    }

    /*package*/ static final DBWrapper getDbWrapper(DBResult<?> result) {
        try {
            return acquireDatabase();
        }
        catch(DbAcquisitionException e) {
            result.setDbAcquisitionFlag();
            return null;
        }
    }

    /*package*/ static final ReadOnlyDBWrapper getReadOnlyDbWrapper(DBResult<?> result) {
        try {
            return acquireReadOnlyDatabase();
        }
        catch(DbAcquisitionException e) {
            result.setDbAcquisitionFlag();
            return null;
        }
    }

    private static final DBWrapper acquireDatabase() throws DbAcquisitionException {
        try {
            getInstance().mutex.acquire();
            Logger.d("Acquired db");
            return new DBWrapper(getInstance().myOpenHelperInstance.getWritableDatabase(), getInstance().mutex);
        }
        catch (InterruptedException e) {
            Logger.e("The thread waiting for a db was interrupted", e);
            throw new DbAcquisitionException();
        }
        catch(Exception e) {
            Logger.e("There was an error getting the db", e);
            throw new DbAcquisitionException();
        }
    }

    private static final ReadOnlyDBWrapper acquireReadOnlyDatabase() throws DbAcquisitionException {
        try {
            return new ReadOnlyDBWrapper(getInstance().myOpenHelperInstance.getReadableDatabase());
        }
        catch(Exception e) {
            Logger.e("There was an error getting the db", e);
            throw new DbAcquisitionException();
        }
    }
}
