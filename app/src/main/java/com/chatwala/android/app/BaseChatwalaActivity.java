package com.chatwala.android.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.chatwala.android.util.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/8/2014
 * Time: 1:28 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseChatwalaActivity extends FragmentActivity {
    private boolean wasBackButtonPressed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        wasBackButtonPressed = false;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... v) {
                if(BaseChatwalaActivity.this != null) {
                    Logger.i("About to send Facebook activateApp event");
                    com.facebook.AppEventsLogger.activateApp(BaseChatwalaActivity.this, EnvironmentVariables.get().getFacebookAppId());
                }
                else {
                    Logger.e("Couldn't send Facebook activateApp event because Activity was null");
                }
                return null;
            }
        }.execute();
    }

    /**
     * This method is only valid in onBackPressed() or later.
     * @return whether the back button was pressed
     */
    public boolean wasBackButtonPressed() {
        return wasBackButtonPressed;
    }

    protected void setWasBackButtonPressed(boolean wasBackButtonPressed) {
        this.wasBackButtonPressed = wasBackButtonPressed;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        wasBackButtonPressed = true;
    }
}
