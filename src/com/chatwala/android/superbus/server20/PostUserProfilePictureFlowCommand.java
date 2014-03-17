package com.chatwala.android.superbus.server20;

import android.content.Context;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.PermanentException;
import co.touchlab.android.superbus.TransientException;
import co.touchlab.android.superbus.provider.sqlite.SqliteCommand;

/**
 * Created by samirahman on 3/16/14.
 */
public class PostUserProfilePictureFlowCommand extends SqliteCommand {

    String thumbPath;
    boolean sasURLFetched=false;
    boolean imagePosted=false;

    public PostUserProfilePictureFlowCommand(String thumbPath) {
        this.thumbPath=thumbPath;
    }

    @Override
    public String logSummary() {
        return null;
    }

    @Override
    public boolean same(Command command) {
        return false;
    }

    @Override
    public void callCommand(Context context) throws TransientException, PermanentException {

    }
}
