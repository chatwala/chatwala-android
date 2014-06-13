package com.chatwala.android.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/28/13
 * Time: 11:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class AndroidUtils {
    public static Intent getChooserIntentExcludingPackage(Context context, Intent fromIntent, String packagePrefixToExclude) {
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        if (context == null || context.getPackageManager() == null) {
            return null;
        }
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(fromIntent, 0);
        if (resInfo != null && !resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo == null || resolveInfo.activityInfo == null) {
                    continue;
                }
                String packageName = resolveInfo.activityInfo.packageName;
                if (packageName != null && packageName.startsWith(packagePrefixToExclude)) {
                    continue;
                }
                Intent targetedShareIntent = new Intent(fromIntent.getAction());
                if (fromIntent.getCategories() != null && !fromIntent.getCategories().isEmpty()) {
                    for (String cat : fromIntent.getCategories()) {
                        targetedShareIntent.addCategory(cat);
                    }
                }

                targetedShareIntent.setData(fromIntent.getData());
                targetedShareIntent.setPackage(packageName);
                targetedShareIntents.add(targetedShareIntent);
            }

            if(targetedShareIntents.isEmpty()) {
                return null;
            }
            if(targetedShareIntents.size() == 1) {
                return Intent.createChooser(targetedShareIntents.remove(0), null);
            }
            else {
                Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(targetedShareIntents.size() - 1), null);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
                return chooserIntent;
            }
        }
        return null;
    }
}
