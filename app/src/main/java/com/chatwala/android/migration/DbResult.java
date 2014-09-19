package com.chatwala.android.migration;

import com.chatwala.android.util.CwResult;
import com.chatwala.android.util.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/23/2014
 * Time: 2:16 PM
 * To change this template use File | Settings | File Templates.
 */
/*package*/ class DbResult<T> extends CwResult<T> {
    /*package*/ void setDbAcquisitionFlag() {
        Logger.e("There was an error acquiring the db somewhere");
        setError("There was an error. Please try again later.");
    }
}
