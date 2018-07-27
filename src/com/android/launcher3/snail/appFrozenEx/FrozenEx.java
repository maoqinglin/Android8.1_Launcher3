package com.android.launcher3.snail.appFrozenEx;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Administrator on 2017/11/23.
 */

public class FrozenEx {

    private static final String AUTHORITY = "com.ireadygo.providers.recentappsprovider";
    private static final String CONTENT_URI_UPDATE = "content://" + AUTHORITY + "/update";
    private static Set<String> sLauncherPkgCache = new HashSet<>();
    private static Intent sHomeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);

    public static boolean isAppEnableSetting(Context context, String pkgName) {
        if (!TextUtils.isEmpty(pkgName)) {
            try {
                int state = context.getPackageManager().getApplicationEnabledSetting(pkgName);
                if (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT || state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return true;
                } else if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    return false;
                }
            } catch (IllegalArgumentException e) {
                Log.e("lmq", e.getMessage());
            }
        }
        return false;
    }

    public static void setAppState(Context context, final String pkgName) {
        if (!TextUtils.isEmpty(pkgName)) {
            // modify by linmq 2018-1-9 不判断禁用状态，直接解冻应用
            try {
                // add by linmq 2018-1-8 修复异常
                context.getPackageManager().setApplicationEnabledSetting(pkgName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
            } catch (IllegalArgumentException e) {
                Log.e("lmq", e.getMessage());
            }
        }
    }

    public static void enableShortcut(Context context, Intent shortcutIntent) {
        Intent newIntent = new Intent(shortcutIntent);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> availableActivities = pm.queryIntentActivities(newIntent, PackageManager.MATCH_DISABLED_COMPONENTS);
        for (ResolveInfo ri : availableActivities) {
            if (ri.activityInfo != null && !TextUtils.isEmpty(ri.activityInfo.packageName)) {
                setAppState(context, ri.activityInfo.packageName);
            }
        }
    }

    public static boolean isLauncherApp(Context context, Intent intent) {
        if (intent != null && intent.getComponent() != null) {
            PackageManager pm = context.getPackageManager();
            //先从缓存中取
            for (String pkgFromCache : sLauncherPkgCache) {
                if (pkgFromCache.equals(intent.getComponent().getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    // add by linmq 2018-1-9 360应用特殊处理
    public static boolean isQiHooApp(Intent intent) {
        boolean result = false;
        if (intent != null && intent.getComponent() != null) {
            result = intent.getComponent().flattenToString().toLowerCase().contains("qihoo");
        }
        return result;
    }

    public static void loadLauncherApp(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(sHomeIntent, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.MATCH_DISABLED_COMPONENTS);
        for (ResolveInfo info : infos) {
            String pkgFromInfo = info.activityInfo.applicationInfo.packageName;
            if (!TextUtils.isEmpty(pkgFromInfo)) {
                sLauncherPkgCache.add(pkgFromInfo);
            }
        }
    }

    public static void reLoadedLauncherApp(Context context) {
        loadLauncherApp(context);
    }

    public static void updateLauncherApp(Context context) {
        ContentResolver cr = context.getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put("pkg", "com.ireadygo.app.recentapps");
        cv.put("isDebug", false);
        cr.update(Uri.parse(CONTENT_URI_UPDATE + "/is_debug"), cv, null, null);
    }
}
