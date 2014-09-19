package com.chatwala.android.util;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Created by Eliezer on 5/7/2014.
 */
public class Typefaces {
    private static final String TYPEFACE_MD = "fonts/ITCAvantGardeStd-Md.otf";
    private static final String TYPEFACE_DEMI = "fonts/ITCAvantGardeStd-Demi.otf";

    private static Typeface md;
    private static Typeface demi;

    public static Typeface getMdTypeface(Context c) {
        if(md == null) {
            md = Typeface.createFromAsset(c.getAssets(), TYPEFACE_MD);
        }
        return md;
    }

    public static Typeface getDemiTypeface(Context c) {
        if(demi == null) {
            demi = Typeface.createFromAsset(c.getAssets(), TYPEFACE_DEMI);
        }
        return demi;
    }
}
