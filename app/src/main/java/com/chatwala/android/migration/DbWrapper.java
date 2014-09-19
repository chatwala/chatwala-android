package com.chatwala.android.migration;

import android.database.sqlite.SQLiteDatabase;
import com.chatwala.android.util.Logger;

import java.util.concurrent.Semaphore;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/23/2014
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
/*package*/ class DbWrapper {
    private SQLiteDatabase db;
    private Semaphore mutex;

    /*package*/ DbWrapper(SQLiteDatabase db, Semaphore mutex) {
        this.db = db;
        this.mutex = mutex;
    }

    public SQLiteDatabase get() {
        return db;
    }

    public void release() {
        if(mutex != null) {
            while(mutex.availablePermits() <= 0) {
                mutex.release();
                Logger.d("Released db");
            }
        }
        db = null;
        mutex = null;
    }
}