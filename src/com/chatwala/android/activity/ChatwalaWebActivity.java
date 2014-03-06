package com.chatwala.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ChatwalaWebActivity extends Activity {
    public static final String CHATWALA_WEB_TITLE_EXTRA = "CHATWALA_WEB_TITLE";
    public static final String CHATWALA_WEB_URL_EXTRA = "CHATWALA_WEB_URL";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView web = new WebView(this);
        if(!getIntent().hasExtra(CHATWALA_WEB_TITLE_EXTRA) || !getIntent().hasExtra(CHATWALA_WEB_URL_EXTRA)) {
            finish();
            return;
        }

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        setTitle(getIntent().getStringExtra(CHATWALA_WEB_TITLE_EXTRA));

        setContentView(web);

        web.getSettings().setJavaScriptEnabled(false);
        web.loadUrl(getIntent().getStringExtra(CHATWALA_WEB_URL_EXTRA));
    }
}
