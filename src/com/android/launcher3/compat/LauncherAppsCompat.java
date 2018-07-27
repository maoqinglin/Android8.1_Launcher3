/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.compat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.launcher3.Utilities;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.snail.appFrozenEx.FrozenEx;
import com.android.launcher3.util.PackageUserKey;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public abstract class LauncherAppsCompat {

    public interface OnAppsChangedCallbackCompat {
        void onPackageRemoved(String packageName, UserHandle user);
        void onPackageAdded(String packageName, UserHandle user);
        void onPackageChanged(String packageName, UserHandle user);
        void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing);
        void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing);
        void onPackagesSuspended(String[] packageNames, UserHandle user);
        void onPackagesUnsuspended(String[] packageNames, UserHandle user);
        void onShortcutsChanged(String packageName, List<ShortcutInfoCompat> shortcuts,
                UserHandle user);
    }

    protected LauncherAppsCompat() {
    }

    private static LauncherAppsCompat sInstance;
    private static final Object sInstanceLock = new Object();

    public static LauncherAppsCompat getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                if (Utilities.ATLEAST_OREO) {
                    sInstance = new LauncherAppsCompatVO(context.getApplicationContext());
                } else {
                    sInstance = new LauncherAppsCompatVL(context.getApplicationContext());
                }
            }
            return sInstance;
        }
    }

    public abstract List<LauncherActivityInfo> getActivityList(String packageName,
            UserHandle user);
    public abstract LauncherActivityInfo resolveActivity(Intent intent,
            UserHandle user);
    public abstract void startActivityForProfile(ComponentName component, UserHandle user,
            Rect sourceBounds, Bundle opts);
    public abstract ApplicationInfo getApplicationInfo(
            String packageName, int flags, UserHandle user);
    public abstract void showAppDetailsForProfile(ComponentName component, UserHandle user,
            Rect sourceBounds, Bundle opts);
    public abstract void addOnAppsChangedCallback(OnAppsChangedCallbackCompat listener);
    public abstract void removeOnAppsChangedCallback(OnAppsChangedCallbackCompat listener);
    public abstract boolean isPackageEnabledForProfile(String packageName, UserHandle user);
    public abstract boolean isActivityEnabledForProfile(ComponentName component,
            UserHandle user);
    public abstract List<ShortcutConfigActivityInfo> getCustomShortcutActivityList(
            @Nullable PackageUserKey packageUser);

    public void showAppDetailsForProfile(ComponentName component, UserHandle user) {
        showAppDetailsForProfile(component, user, null, null);
    }

    //add by linmq 2018-7-23
    public List<LauncherActivityInfo> getDisableAppList(Intent intent,Context context,UserHandle user){
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> availableActivities = pm.queryIntentActivities(intent, PackageManager.MATCH_DISABLED_COMPONENTS);
        List<LauncherActivityInfo> lais = new ArrayList<>(availableActivities.size());
        for (ResolveInfo ri : availableActivities) {
            if (ri.activityInfo != null && ri.activityInfo.enabled) { // modify by linmq 2018-7-23  防止使用activity-alias 设置多个启动activity(但一般只设置一个enabled)
                if (!FrozenEx.isAppEnableSetting(context, ri.activityInfo.packageName)) {
                    LauncherActivityInfo lai = generateInstance(context, ri.activityInfo, user);
                    if (lai != null) {
                        lais.add(lai);
                    } else {
                        Log.e("lmq", ri.activityInfo.packageName + " is generateInstance fail!");
                    }
                }
            }
        }
        return lais;
    }

    public LauncherActivityInfo getLauncherActivityInfo(Context context, Intent intent, UserHandle user) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> availableActivities = pm.queryIntentActivities(intent, PackageManager.MATCH_DISABLED_COMPONENTS);
        for (ResolveInfo ri : availableActivities) {
            if (ri.activityInfo != null && ri.activityInfo.enabled) { // modify by linmq 2018-7-23  防止使用activity-alias 设置多个启动activity(但一般只设置一个enabled)
                if (!FrozenEx.isAppEnableSetting(context, ri.activityInfo.packageName)) {
                    LauncherActivityInfo lai = generateInstance(context, ri.activityInfo, user);
                    if (lai != null) {
                        return lai;
                    }
                }
            }
        }
        return null;
    }

    public List<LauncherActivityInfo> getAllShortcutList(List<LauncherActivityInfo> installList, Context context, UserHandle user) {
        List<LauncherActivityInfo> allShortcutList = new ArrayList<>(installList);
        List<LauncherActivityInfo> disableShortList = getDisableShortcutList(context, user);
        for (LauncherActivityInfo disableShortcut : disableShortList) {
            boolean exist = false;
            for (LauncherActivityInfo installShortcut : installList) {
                if (installShortcut.getComponentName().equals(disableShortcut.getComponentName())) {
                    exist = true;
                    break;
                }
            }
            if (exist) {
                continue;
            } else {
                allShortcutList.add(disableShortcut);
            }
        }

        return allShortcutList;
    }

    private List<LauncherActivityInfo> getDisableShortcutList(Context context, UserHandle user) {
        Intent shortIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT, null);
//        shortIntent.addCategory(Intent.CATEGORY_DEFAULT); // 有些应用没有default category
        return getDisableAppList(shortIntent, context, user);
    }

    protected LauncherActivityInfo generateInstance(Context context, ActivityInfo info, UserHandle user) {

        try {
            Class<?> cls = Class.forName("android.content.pm.LauncherActivityInfo");
            Class<?>[] param = new Class[]{Context.class, ActivityInfo.class, UserHandle.class};
            Constructor<?> con = cls.getDeclaredConstructor(param);
            con.setAccessible(true);
            Object[] objects = new Object[]{context, info, user};
            return (LauncherActivityInfo) con.newInstance(objects);
        } catch (ClassNotFoundException e) {
            Log.e("lmq","ClassNotFoundException "+e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.e("lmq","NoSuchMethodException "+e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e("lmq","IllegalAccessException "+e.getMessage());
            e.printStackTrace();
        } catch (InstantiationException e) {
            Log.e("lmq","InstantiationException "+e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e("lmq","InvocationTargetException "+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
