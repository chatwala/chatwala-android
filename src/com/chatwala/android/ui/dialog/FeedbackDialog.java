package com.chatwala.android.ui.dialog;

import android.view.View;
import com.chatwala.android.activity.BaseChatWalaActivity;
import com.chatwala.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthewdavis on 1/21/14.
 */
public class FeedbackDialog extends ChatwalaBlueDialog
{
    public FeedbackDialog(BaseChatWalaActivity chatwalaActivity)
    {
        super(chatwalaActivity);
    }

    @Override
    protected int getTopMessageResource()
    {
        return R.string.are_you_happy_text;
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
                    new RateAppDialog(chatwalaActivity).showDialog();
                    hideDialog();
                }
            }, R.string.yes)
        );

        buttonList.add(new DialogButton(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    new SendFeedbackDialog(chatwalaActivity).showDialog();
                    hideDialog();
                }
            }, R.string.no)
        );

        return buttonList;
    }

    @Override
    protected boolean showHeaderStuff()
    {
        return false;
    }
}
