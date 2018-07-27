/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3.config;

/**
 * Defines a set of flags used to control various launcher behaviors
 */
public final class FeatureFlags extends BaseFlags {

    private FeatureFlags() {}

    public static final boolean DISABLE_ALL_APPS = true;

    public static final boolean SCROLL_LOOP = true;

    public static final boolean SNAIL_PAGE_INDICATOR = true;

    public static final boolean ADD_OR_DELETE_CELLLAYOUT = false;

    public static final int DEFAULT_ICON_SIZE_DP = 53;

    public static final boolean ADD_CUSTOM_PAGE = true;

    public static final boolean HIDE_QSB = true;

    public static final boolean CUSTOM_FOLDER = true;

    public static final boolean KEY_EXTEND = true;

    public static final int KEYCODE_BUTTON_L1 = 102;
    public static final int KEYCODE_BUTTON_R1 = 103;

    public static final boolean LANDSCAPE_EXTEND = true;

    public static final boolean CUSTOM_APP_ICON = true;
    public static final boolean ADAPTER_ICON_FORCE = true;
    public static final boolean SHOW_DISABLE_ICON = true;

    public static final boolean OVERVIEW_FREE_SCROLL = false; // 缩略图不自由滚动，平滑滚动到页面

    public static final boolean SUPPORT_DELETE_BUTTON = true;

    public static final boolean NOT_SUPPORT_INFO_BUTTON = true;
}
