package com.chatwala.android.ui.dialog;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import com.chatwala.android.BaseChatWalaActivity;
import com.chatwala.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthewdavis on 1/21/14.
 */
public class SendFeedbackDialog extends ChatwalaBlueDialog
{
    public SendFeedbackDialog(BaseChatWalaActivity chatwalaActivity)
    {
        super(chatwalaActivity);
    }

    @Override
    protected int getTopMessageResource()
    {
        return R.string.sorry_message_text;
    }

    @Override
    protected List<DialogButton> getDialogButtons()
    {
        ArrayList<DialogButton> buttonList = new ArrayList<DialogButton>();

        buttonList.add(new DialogButton(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    hideDialog();

                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);

                    emailIntent.setData(Uri.parse("mailto:"));
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"hello@chatwala.com"});
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Chatwala Android Feedback");

                    chatwalaActivity.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
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
