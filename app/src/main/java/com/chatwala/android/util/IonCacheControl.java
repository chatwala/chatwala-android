package com.chatwala.android.util;

import android.graphics.Bitmap;
import com.koushikdutta.ion.bitmap.Transform;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/27/2014
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class IonCacheControl implements Transform {
    private String cacheKey;

    public IonCacheControl(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    @Override
    public Bitmap transform(Bitmap bitmap) {
        return bitmap;
    }

    @Override
    public String key() {
        return cacheKey;
    }
}
