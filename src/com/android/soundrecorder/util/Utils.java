/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.soundrecorder.util;

import android.app.Activity;
import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.soundrecorder.R;
import com.google.gson.Gson;
import com.truepic.lensverify.data.c2padata.C2PAData;

import java.util.ArrayList;
import java.util.List;

import vendor.qti.hardware.c2pa.C2PADataType;
import vendor.qti.hardware.c2pa.C2PADataTypePair;
import vendor.qti.hardware.c2pa.IC2PA;

public class Utils {
    static final int SECOND_PER_MINUTES = 60;
    static final int SECOND_PER_HOUR = 60 * SECOND_PER_MINUTES;
    static vendor.qti.hardware.c2pa.IC2PA mFactoryAidl = null;
    static IBinder mBinder;
    static final String TAG = Utils.class.getSimpleName();
    public static final String EXTRA_C2PA_INVALID = "EXTRA_C2PA_INVALID";

    public static String timeToString(Context context, long time) {
        long hour = time / SECOND_PER_HOUR;
        long minutes = time / SECOND_PER_MINUTES - hour * SECOND_PER_MINUTES;
        long second = time % SECOND_PER_MINUTES;

        String timerFormat;
        if (hour > 0) {
            timerFormat = context.getResources().getString(R.string.timer_format_hour);
            return String.format(timerFormat, hour, minutes, second);
        } else {
            timerFormat = context.getResources().getString(R.string.timer_format);
            return String.format(timerFormat, minutes, second);
        }
    }

    public static void setUpEdgeToEdge(@NonNull Activity activity) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                    int statusBarHeight = activity.getWindow().getDecorView().getRootWindowInsets()
                            .getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    int actionBarHeight = 0;
                    TypedValue tv = new TypedValue();
                    if (activity.getTheme().resolveAttribute(
                            android.R.attr.actionBarSize, tv, true))
                    {
                        actionBarHeight = TypedValue.complexToDimensionPixelSize(
                                tv.data,activity.getResources().getDisplayMetrics());
                    }
                    v.setPadding(insets.left, statusBarHeight + actionBarHeight,
                            insets.right, insets.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
    }

    public static void connectC2PAService() {
        try {
            Log.d(TAG, "Call C2PA getService");
            String ISERVICE_INTERFACE = "vendor.qti.hardware.c2pa.IC2PA/default";
            if (ServiceManager.isDeclared(ISERVICE_INTERFACE)){
                mBinder = ServiceManager.waitForService(ISERVICE_INTERFACE);
                mFactoryAidl = IC2PA.Stub.asInterface(mBinder);
            }
        } catch (Exception e) {
            Log.e(TAG, "C2PA not supported: " + e);
        }
        if ( mFactoryAidl == null) {
            Log.e(TAG, "C2PA returned null");
            return;
        }
    }

    public static vendor.qti.hardware.c2pa.IC2PA getC2paService() {
        if (mFactoryAidl == null) {
            connectC2PAService();
        }
        return mFactoryAidl;
    }

    public static List<C2PADataTypePair> getConfigParams(int type) {
        List<C2PADataTypePair> configParams = new ArrayList<C2PADataTypePair>();
        C2PADataTypePair outPair = new C2PADataTypePair();
        C2PADataType c2PADataType;
        outPair.key = "MEDIA_TYPE";
        c2PADataType = new C2PADataType();
        c2PADataType.setIntValue(type);
        outPair.value = c2PADataType;
        configParams.add(outPair);
        return configParams;
    }

    public static C2PAData jsonToC2PAData(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, C2PAData.class);
    }
}
