package com.chatwala.android.db;

/**
 * Created by Eliezer on 3/20/2014.
 */
import java.util.concurrent.Semaphore;

import android.database.sqlite.SQLiteDatabase;
import com.chatwala.android.util.Logger;

public class DBWrapper {
    private SQLiteDatabase db;
    private Semaphore mutex;

    /*package*/ DBWrapper(SQLiteDatabase db, Semaphore mutex) {
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