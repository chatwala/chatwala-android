package com.chatwala.android.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.chatwala.android.R;

/**
 * Created with IntelliJ IDEA.
 * User: eygraber
 * Date: 5/19/2014
 * Time: 4:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackActivity extends BaseChatwalaActivity {
    private boolean doFeedbackAction = false;

    private TextView topText;
    private TextView yesText;
    private TextView noText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_activity);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        topText = (TextView) findViewById(R.id.feedback_text);
        yesText = (TextView) findViewById(R.id.feedback_yes_text);
        noText = (TextView) findViewById(R.id.feedback_no_text);

        findViewById(R.id.feedback_yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(doFeedbackAction) {
                    onPositiveFeedback();
                }
                else {
                    topText.setText(R.string.feedback_positive_text);
                    yesText.setText(R.string.feedback_sure);
                    findViewById(R.id.feedback_no).setVisibility(View.GONE);
                    doFeedbackAction = true;
                }
            }
        });

        findViewById(R.id.feedback_no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(doFeedbackAction) {
                    onNegativeFeedback();
                }
                else {
                    topText.setText(R.string.feedback_negative_text);
                    noText.setText(R.string.feedback_sure);
                    findViewById(R.id.feedback_yes).setVisibility(View.GONE);
                    doFeedbackAction = true;
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
        finish();
    }

    private void onPositiveFeedback() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.chatwala.chatwala")));
        finish();
    }

    private void onNegativeFeedback() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);

        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"hello@chatwala.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Chatwala Android Feedback");

        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        finish();
    }
}
