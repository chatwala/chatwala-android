package com.chatwala.android.ui.dialog;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.chatwala.android.BaseChatWalaActivity;
import com.chatwala.android.ChatwalaApplication;
import com.chatwala.android.R;

import java.util.List;

/**
 * Created by matthewdavis on 1/21/14.
 */
public abstract class ChatwalaBlueDialog
{
    protected BaseChatWalaActivity chatwalaActivity;
    private ViewGroup blueMessageDialog;

    public ChatwalaBlueDialog(BaseChatWalaActivity chatwalaActivity)
    {
        this.chatwalaActivity = chatwalaActivity;
    }

    public void showDialog()
    {
        hideDialog();

        blueMessageDialog = (ViewGroup) chatwalaActivity.getLayoutInflater().inflate(R.layout.message_dialag, null);

        blueMessageDialog.findViewById(R.id.messageClose).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                hideDialog();
            }
        });
        Typeface fontMd = ((ChatwalaApplication)chatwalaActivity.getApplication()).fontMd;
        ((TextView) blueMessageDialog.findViewById(R.id.feedbackTitle)).setTypeface(fontMd);
        TextView messageText = (TextView) blueMessageDialog.findViewById(R.id.message_dialag_text);
        TextView messageFiller = (TextView) blueMessageDialog.findViewById(R.id.message_dialag_filler);

        messageText.setTypeface(fontMd);
        messageText.setText(getTopMessageResource());

        messageFiller.setTypeface(fontMd);
        messageFiller.setText(getTopMessageResource());

        ViewGroup buttonLayout = (ViewGroup) blueMessageDialog.findViewById(R.id.messageDialagButtonContainer);
        for (DialogButton button : getDialogButtons())
        {
            buttonLayout.addView(makeDialogButton(button));
        }

        chatwalaActivity.findViewRoot().addView(blueMessageDialog);
    }

    public void hideDialog()
    {
        if (blueMessageDialog != null)
        {
            chatwalaActivity.findViewRoot().removeView(blueMessageDialog);
            blueMessageDialog = null;
        }
    }

    private View makeDialogButton(DialogButton buttonDef)
    {
        View button = chatwalaActivity.getLayoutInflater().inflate(R.layout.message_dialag_button, null);

        Button buttonText = (Button) button.findViewById(R.id.buttonText);
        buttonText.setTypeface(((ChatwalaApplication) chatwalaActivity.getApplication()).fontDemi);
        buttonText.setText(buttonDef.stringRes);
        buttonText.setOnClickListener(buttonDef.listener);

        return button;
    }

    protected abstract int getTopMessageResource();
    protected abstract List<DialogButton> getDialogButtons();

    class DialogButton
    {
        View.OnClickListener listener;
        int stringRes;

        DialogButton(View.OnClickListener listener, int stringRes)
        {
            this.listener = listener;
            this.stringRes = stringRes;
        }
    }
}
