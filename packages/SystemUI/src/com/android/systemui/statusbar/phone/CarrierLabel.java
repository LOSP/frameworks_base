/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.os.UserHandle;
import android.os.Handler;
import android.content.ContentResolver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.widget.TextView;

import com.android.internal.telephony.TelephonyIntents;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.android.internal.R;

/**
 * This widget display an analogic clock with two hands for hours and
 * minutes.
 */
public class CarrierLabel extends TextView {
    private boolean mAttached;
    protected int mCarrierColor = com.android.internal.R.color.holo_blue_light;
    Handler mHandler;
    String mLastCarrier;
    private Context mContext;

    public CarrierLabel(Context context) {
        this(context, null);
    }

    public CarrierLabel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarrierLabel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        updateNetworkName(false, null, false, null);
        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
        }

        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                mLastCarrier = intent.getStringExtra(TelephonyIntents.EXTRA_SPN);
                updateNetworkName(intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                        intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));
            }
        }
    };

    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        if (false) {
            Slog.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn + " spn=" + spn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }
        String str = "";
        // match logic in KeyguardStatusViewManager
        final boolean plmnValid = showPlmn && !TextUtils.isEmpty(plmn);
        final boolean spnValid = showSpn && !TextUtils.isEmpty(spn);
        if (plmnValid && spnValid) {
            str = spn;
        } else if (plmnValid) {
            str = plmn;
        } else if (spnValid) {
            str = spn;
        } else {
            str = "";
        }
        String customLabel = Settings.System.getStringForUser(getContext().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL, UserHandle.USER_CURRENT);
        if(!TextUtils.isEmpty(customLabel))
            str = customLabel;
        else if (TextUtils.isEmpty(str.trim()))
            str = mLastCarrier;
        setText(operatorCheck(mContext, str));
    }

    public static String operatorCheck(Context context, String CarrierLabelText) {
        if (CarrierLabelText != null) {
            String str1 = CarrierLabelText.trim().toLowerCase();
            String ids[] = context.getResources().getStringArray(com.android.internal.R.array.operator_translate_ids);
            String names[] = context.getResources().getStringArray(com.android.internal.R.array.operator_translate_names);
            for (int i = 0; i < ids.length; i++) {
                if (str1.equals(ids[i])) {
                    return names[i];
                }
            }
            return str1;
        } else {
            return "";
        }
    }

    private void updateSettings() {
        boolean showCarrier = (Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_CARRIER, 1) == 1);
        if (showCarrier) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
        // set the color
        int defaultColor = getResources().getColor(
                com.android.internal.R.color.holo_blue_light);
        mCarrierColor = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_CARRIER_COLOR, -2);
        if (mCarrierColor == Integer.MIN_VALUE || mCarrierColor == -2) {
            mCarrierColor = defaultColor;
        }
        setTextColor(mCarrierColor);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getContext().getContentResolver();
            resolver.registerContentObserver(Settings.System
                .getUriFor(Settings.System.CUSTOM_CARRIER_LABEL), false, this);
            resolver.registerContentObserver(Settings.System
                .getUriFor(Settings.System.STATUS_BAR_SHOW_CARRIER), false, this);
            resolver.registerContentObserver(Settings.System
                .getUriFor(Settings.System.STATUS_BAR_CARRIER_COLOR), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
            updateNetworkName(true, Settings.System.getStringForUser(getContext().getContentResolver(),
                  Settings.System.CUSTOM_CARRIER_LABEL, UserHandle.USER_CURRENT), false, null);
        }
    }
}


