package com.chatwala.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import com.chatwala.android.ui.dialog.FeedbackDialog;

/**
 * Created by matthewdavis on 1/23/14.
 */
public class FeedbackActivity extends BaseChatWalaActivity
{
    private boolean isAfterReply;

    private static final String IS_AFTER_REPLY = "IS_AFTER_REPLY";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        isAfterReply = getIntent().getBooleanExtra(IS_AFTER_REPLY, false);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        new FeedbackDialog(FeedbackActivity.this).showDialog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed()
    {
        if(isAfterReply)
        {
            NewCameraActivity.startMe(FeedbackActivity.this);
            finish();
        }
        else
        {
            super.onBackPressed();
        }
    }

    public static void startMe(Context context, boolean isAfterReply)
    {
        Intent intent = new Intent(context, FeedbackActivity.class);
        intent.putExtra(IS_AFTER_REPLY, isAfterReply);
        context.startActivity(intent);
    }
}
