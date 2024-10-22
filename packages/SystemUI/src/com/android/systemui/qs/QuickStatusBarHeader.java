/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.systemui.R;
import com.android.systemui.util.LargeScreenUtils;

/**
 * View that contains the top-most bits of the QS panel (primarily the status bar with date, time,
 * battery, carrier info and privacy icons) and also contains the {@link QuickQSPanel}.
 */
public class QuickStatusBarHeader extends FrameLayout {

    private int colorSecondaryLabelActive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorSecondaryInverse);
    private int colorSecondaryLabelInactive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorSecondary);

    private boolean mExpanded;
    private boolean mQsDisabled;
    
    private final TelephonyManager mTelephonyManager;
    private final int mSubscriptionId = -1; 
    
    private ViewGroup mQSHyperHeader;
    private View mHyperWifiButton, mHyperDataButton;
    private ImageView mHyperWifiIcon, mHyperDataIcon;
    private TextView mHyperWifiTitle, mHyperDataTitle;
    private TextView mHyperWifiSummary, mHyperDataSummary;
    
    private Runnable mUpdateRunnableWifi, mUpdateRunnableData;

    protected QuickQSPanel mHeaderQsPanel;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeaderQsPanel = findViewById(R.id.quick_qs_panel);
        
        mQSHyperHeader = findViewById(R.id.qs_hyper_container);
        
        mHyperWifiButton = findViewById(R.id.qs_hyper_wifi_button);
        mHyperWifiIcon = findViewById(R.id.qs_hyper_wifi_icon);
        mHyperWifiTitle = findViewById(R.id.qs_hyper_wifi_title);
        mHyperWifiSummary = findViewById(R.id.qs_hyper_wifi_summary);
        mHyperDataButton = findViewById(R.id.qs_hyper_data_button);
        mHyperDataIcon = findViewById(R.id.qs_hyper_data_icon);
        mHyperDataTitle = findViewById(R.id.qs_hyper_data_title);
        mHyperDataSummary = findViewById(R.id.qs_hyper_data_summary);
        
        mHyperWifiButton.setOnClickListener(this);
        mHyperDataButton.setOnClickListener(this);
        mHyperWifiButton.setOnLongClickListener(this);
        mHyperDataButton.setOnLongClickListener(this);

        updateResources();
        
        startUpdateWifiButtonStateAsync();
        startUpdateDataButtonStateAsync();
    }
    
    @Override
    public void onClick(View v) {
    
        } else if (v == mHyperWifiButton) {
			updateWifiButton();
        } else if (v == mHyperDataButton) {
			updateDataButton();
		
    }
     
    @Override
    public boolean onLongClick(View v) {
    
        } else if (v == mHyperWifiButton) {
            mActivityStarter.postStartActivityDismissingKeyguard(new Intent(
                Settings.ACTION_WIFI_SETTINGS), 0);
            return true;   
        } else if (v == mHyperDataButton) {
        	mActivityStarter.postStartActivityDismissingKeyguard(new Intent(
                Settings.ACTION_NETWORK_OPERATOR_SETTINGS), 0);
            return true;
        
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Only react to touches inside QuickQSPanel
        if (event.getY() > mHeaderQsPanel.getTop()) {
            return super.onTouchEvent(event);
        } else {
            return false;
        }
    }
    
    private TelephonyManager getTelephonyManager() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubscriptionId)) {
            mSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        }
        if (!SubscriptionManager.isValidSubscriptionId(mSubscriptionId)) {
            int[] activeSubscriptionIdList = SubscriptionManager.from(mContext).getActiveSubscriptionIdList();
            if (!ArrayUtils.isEmpty(activeSubscriptionIdList)) {
                mSubscriptionId = activeSubscriptionIdList[0];
            }
        }
        return ((TelephonyManager) mContext.getSystemService(TelephonyManager.class)).createForSubscriptionId(mSubscriptionId);
    }
    
    private void updateQSCustomHeaderStyle() {
        if (mQSCustomHeaderEnabled) {    
            if (mQSCustomHeaderStyle == 0) {
                mQSDefaultHeader.setVisibility(View.GONE);
                mQSAnimHeader.setVisibility(View.VISIBLE);
                mQSHyperHeader.setVisibility(View.GONE);
                } else if (mQSCustomHeaderStyle == 1) {
                mQSDefaultHeader.setVisibility(View.GONE);
                mQSAnimHeader.setVisibility(View.GONE);
                mQSHyperHeader.setVisibility(View.VISIBLE);
                }           
            } else {
            mQSDefaultHeader.setVisibility(View.VISIBLE);
            mQSAnimHeader.setVisibility(View.GONE);
            mQSHyperHeader.setVisibility(View.GONE);
        }   
    }

    void updateResources() {
        Resources resources = mContext.getResources();
        boolean largeScreenHeaderActive =
                LargeScreenUtils.shouldUseLargeScreenShadeHeader(resources);

        ViewGroup.LayoutParams lp = getLayoutParams();
        if (mQsDisabled) {
            lp.height = 0;
        } else {
            lp.height = WRAP_CONTENT;
        }
        setLayoutParams(lp);

        MarginLayoutParams qqsLP = (MarginLayoutParams) mHeaderQsPanel.getLayoutParams();
        if (largeScreenHeaderActive) {
            qqsLP.topMargin = mContext.getResources()
                    .getDimensionPixelSize(R.dimen.qqs_layout_margin_top);
        } else {
            qqsLP.topMargin = mContext.getResources()
                    .getDimensionPixelSize(R.dimen.large_screen_shade_header_min_height);
        }
        mHeaderQsPanel.setLayoutParams(qqsLP);
    }
    
    private boolean isWifiEnabled() {
        try {
            return mWifiManager.isWifiEnabled();
        } catch (Exception unused) {
            return false;
        }
    }
    
    private void setWifiEnabled(boolean enable) {
        try {
            mWifiManager.setWifiEnabled(enable);
        } catch (Exception unused) {
        }
    }
    
    public void updateWifiButton() {
        synchronized (this) {
            setWifiEnabled(!isWifiEnabled());
        }
        startUpdateWifiButtonStateAsync();
    }
    
    public void updateWifiButtonState() {
        if (mHyperWifiButton == null 
        || mHyperWifiIcon == null 
        || mHyperWifiTitle == null 
        || mHyperWifiSummary == null) return;
        Drawable background = mHyperWifiButton.getBackground();
        if (isWifiEnabled()) {
            background.setTint(colorActive);
            mHyperWifiIcon.setColorFilter(colorLabelActive);
            mHyperWifiTitle.setTextColor(colorLabelActive);
            mHyperWifiSummary.setText(getWifiSsid());
            mHyperWifiSummary.setTextColor(colorSecondaryLabelActive);
        } else {
            background.setTint(colorInactive);
            mHyperWifiIcon.setColorFilter(colorLabelInactive);
            mHyperWifiTitle.setTextColor(colorLabelInactive);
            mHyperWifiSummary.setText("Off");
            mHyperWifiSummary.setTextColor(colorSecondaryLabelInactive);
        }
    }
    
    public final void startUpdateWifiButtonStateAsync() {
        AsyncTask.execute(
                new Runnable() {
                    public void run() {
                        startUpdateWifiButtonState();
                    }
                });
    }
    
    public void startUpdateWifiButtonState() {
        Runnable runnable = mUpdateRunnableWifi;
        if (runnable == null) {
            mUpdateRunnableWifi =
                    new Runnable() {
                        public void run() {
                            updateWifiButtonState();
                            scheduleWifiButtonUpdate();
                        }
                    };
        } else {
            mHandler.removeCallbacks(runnable);
        }
        scheduleWifiButtonUpdate();
    }
    
    public final void scheduleWifiButtonUpdate() {
        Runnable runnable;
        if ((runnable = mUpdateRunnableWifi) != null) {
            mHandler.postDelayed(runnable, 250);
        }
    }
    
    public void updateDataButton() {
        synchronized (this) {
            getTelephonyManager().setDataEnabled(!getTelephonyManager().isDataEnabled());
        }
        startUpdateDataButtonStateAsync();
    }
    
    
    public void updateDataButtonState() {
        if (mHyperDataButton == null 
        || mHyperDataIcon == null 
        || mHyperDataTitle == null 
        || mHyperDataSummary == null) return;
        Drawable background = mHyperDataButton.getBackground();
        if (getTelephonyManager().isDataEnabled()) {
            background.setTint(colorActive);
            mHyperDataIcon.setColorFilter(colorLabelActive);
            mHyperDataTitle.setTextColor(colorLabelActive);
            mHyperDataSummary.setText(getSlotCarrierName());
            mHyperDataSummary.setTextColor(colorSecondaryLabelActive);
        } else {
            background.setTint(colorInactive);
            mHyperDataIcon.setColorFilter(colorLabelInactive);
            mHyperDataTitle.setTextColor(colorLabelInactive);
            mHyperDataSummary.setText("Off");
            mHyperDataSummary.setTextColor(colorSecondaryLabelInactive);
        }
    }
    
    public void startUpdateDataButtonStateAsync() {
        AsyncTask.execute(
                new Runnable() {
                    public void run() {
                        startUpdateDataButtonState();
                    }
                });
    }
    
    public void startUpdateDataButtonState() {
        Runnable runnable = mUpdateRunnableData;
        if (runnable == null) {
            mUpdateRunnableData =
                    new Runnable() {
                        public void run() {
                            updateDataButtonState();
                            scheduleDataButtonUpdate();
                        }
                    };
        } else {
            mHandler.removeCallbacks(runnable);
        }
        scheduleDataButtonUpdate();
    }
    
    public void scheduleDataButtonUpdate() {
        Runnable runnable;
        if ((runnable = mUpdateRunnableData) != null) {
            mHandler.postDelayed(runnable, 250);
        }
    }

    public void setExpanded(boolean expanded, QuickQSPanelController quickQSPanelController) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        quickQSPanelController.setExpanded(expanded);
    }

    public void disable(int state1, int state2, boolean animate) {
        final boolean disabled = (state2 & DISABLE2_QUICK_SETTINGS) != 0;
        if (disabled == mQsDisabled) return;
        mQsDisabled = disabled;
        mHeaderQsPanel.setDisabledByPolicy(disabled);
        updateResources();
    }

    private void setContentMargins(View view, int marginStart, int marginEnd) {
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        lp.setMarginStart(marginStart);
        lp.setMarginEnd(marginEnd);
        view.setLayoutParams(lp);
    }
    
}
