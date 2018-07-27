/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.launcher3.pageindicators;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.android.launcher3.R;
import com.android.launcher3.dynamicui.ExtractedColors;

import java.util.ArrayList;

/**
 * Base class for a page indicator.
 */
public abstract class PageIndicator extends LinearLayout { // modify by linmq 2018-6-25 默认FrameLayout会导致指示条重叠
    private CaretDrawable mCaretDrawable;

    protected int mNumPages = 1;

    public PageIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
    }

    public void setScroll(int currentScroll, int totalScroll) {}

    public void setActiveMarker(int activePage) {}

    public void addMarker() {
        mNumPages++;
        onPageCountChanged();
    }

    public void removeMarker() {
        mNumPages--;
        onPageCountChanged();
    }

    public void setMarkersCount(int numMarkers) {
        mNumPages = numMarkers;
        onPageCountChanged();
    }

    public CaretDrawable getCaretDrawable() {
        return mCaretDrawable;
    }

    public void setCaretDrawable(CaretDrawable caretDrawable) {
        if (mCaretDrawable != null) {
            mCaretDrawable.setCallback(null);
        }

        mCaretDrawable = caretDrawable;

        if (mCaretDrawable != null) {
            mCaretDrawable.setCallback(this);
        }
    }

    protected void onPageCountChanged() {}

    public void setShouldAutoHide(boolean shouldAutoHide) {}

    public void updateColor(ExtractedColors extractedColors) {}

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == getCaretDrawable();
    }

    // add by linmq 2018-6-25
    public static class PageMarkerResources {
        int activeId;
        int inactiveId;

        public PageMarkerResources() {
            activeId = R.drawable.snail_page_indicator_selected;
            inactiveId = R.drawable.snail_page_indicator_normal;
        }
        public PageMarkerResources(int aId, int iaId) {
            activeId = aId;
            inactiveId = iaId;
        }
    }

    public void addMarker(int index, PageMarkerResources marker, boolean b) {
    }
    public void updateMarker(int index, PageMarkerResources marker) {
    }
    public void removeMarker(int index, boolean b) {
    }
    public void removeAllMarkers(boolean b) {
    }

    public void addMarkers(ArrayList<PageMarkerResources> markers, boolean b) {
    }
}
