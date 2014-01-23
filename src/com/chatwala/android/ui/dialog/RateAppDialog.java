package com.chatwala.android.ui.dialog;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;
import com.chatwala.android.AppPrefs;
import com.chatwala.android.BaseChatWalaActivity;
import com.chatwala.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthewdavis on 1/21/14.
 */
public class RateAppDialog extends ChatwalaBlueDialog
{
    public RateAppDialog(BaseChatWalaActivity chatwalaActivity)
    {
        super(chatwalaActivity);
    }

    @Override
    protected int getTopMessageResource()
    {
        return R.string.rate_us_text;
    }

    @Override
    protected List<ChatwalaBlueDialog.DialogButton> getDialogButtons()
    {
        ArrayList<DialogButton> buttonList = new ArrayList<DialogButton>();

        buttonList.add(new DialogButton(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    AppPrefs.getInstance(chatwalaActivity).setPrefFeedbackShown(true);
                    chatwalaActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.chatwala.chatwala")));
                    hideDialog();
                }
            }, R.string.sure)
        );

        return buttonList;
    }

    @Override
    protected boolean showHeaderStuff()
    {
        return false;
    }
}
