package com.chatwala.android.db;

import com.chatwala.android.CWResult;
import com.chatwala.android.util.Logger;

/**
 * Created by Eliezer on 3/20/2014.
 */
public class DbResult<T> extends CWResult<T> {
    /*package*/ void setDbAcquisitionFlag() {
        Logger.e("There was an error acquiring the db somewhere");
        setError("There was an error. Please try again later.");
    }
}
