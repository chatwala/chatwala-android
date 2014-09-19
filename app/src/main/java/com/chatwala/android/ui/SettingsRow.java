package com.chatwala.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.chatwala.android.R;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/15/2014
 * Time: 6:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsRow extends LinearLayout {
    private Spinner optionalSelections;

    public SettingsRow(Context context, String text, CharSequence[] selections) {
        super(context);
        init(context, text, selections);
    }

    public SettingsRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingsRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if(attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingsRow);
            if(a != null) {
                String text = a.getString(R.styleable.SettingsRow_settings_row_text);
                CharSequence[] selections = a.getTextArray(R.styleable.SettingsRow_settings_row_selections);
                init(context, text, selections);
                return;
            }
        }
        throw new InflateException("Missing attribute settings_row_text");
    }

    private void init(Context context, String text, CharSequence[] selections) {
        LayoutInflater.from(context).inflate(R.layout.settings_row_layout, this);
        if(text == null) {
            throw new InflateException("Missing attribute settings_row_text");
        }
        else {
            ((TextView) findViewById(R.id.settings_row_text)).setText(text);
        }
        if(selections != null) {
            optionalSelections = (Spinner) findViewById(R.id.settings_row_selection);
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item, selections);
            adapter.setDropDownViewResource(R.layout.settings_spinner_dropdown_item);
            optionalSelections.setAdapter(adapter);
            optionalSelections.setVisibility(VISIBLE);
        }
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
        if(optionalSelections != null) {
            optionalSelections.setOnItemSelectedListener(listener);
        }
    }

    public void setSelection(int position) {
        if(optionalSelections != null) {
            optionalSelections.setSelection(position);
        }
    }
}
