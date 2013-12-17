package com.chatwala.android.dataops;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple sequential operator for data write ops. Generally not for network operations. Definitely not for loading data and
 * doing something with it in the front end.  Think call and forget.
 *
 * User: kgalligan
 * Date: 9/9/13
 * Time: 8:27 PM
 */
public class DataProcessor
{
    static ExecutorService exe = Executors.newSingleThreadExecutor();

    public static void runProcess(Runnable r)
    {
        exe.execute(r);
    }


}
