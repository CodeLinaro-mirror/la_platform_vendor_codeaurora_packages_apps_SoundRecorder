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

package com.android.soundrecorder.filelist.viewholder;

import static com.android.soundrecorder.util.Utils.EXTRA_C2PA_INVALID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.common.Ashmem;
import android.hardware.HardwareBuffer;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.soundrecorder.C2PAActivity;
import com.android.soundrecorder.ConfigUtil;
import com.android.soundrecorder.filelist.listitem.BaseListItem;
import com.android.soundrecorder.filelist.listitem.MediaItem;
import com.android.soundrecorder.filelist.ui.WaveIndicator;
import com.android.soundrecorder.util.FileUtils;
import com.android.soundrecorder.util.Utils;
import com.android.soundrecorder.R;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vendor.qti.hardware.c2pa.C2PADataType;
import vendor.qti.hardware.c2pa.C2PADataTypePair;
import vendor.qti.hardware.c2pa.IC2PA;

public class MediaItemViewHolder extends BaseViewHolder {
    private TextView mDateModifiedView;
    private TextView mDurationView;
    private WaveIndicator mWaveIndicator;
    private SimpleDateFormat mDateFormatter;
    private ImageView mC2paView;
    private boolean mC2paValid = false;
    private FrameLayout mC2paLayout;
    private boolean mIsC2paEnabled;
    private static final String TAG = "MediaItemViewHolder";
    private vendor.qti.hardware.c2pa.IC2PA mFactoryAidl = null;
    private Map<String, ParcelFileDescriptor> mC2paValidations = new HashMap<>();
    private static final int C2PA_RESPONSE_VALID = 0;
    private static final int C2PA_RESPONSE_CORRUPTED = -1;
    private static final int C2PA_SIGNED_VALID = 100;
    private static final int C2PA_SIGNED_BUT_INVALID = 101;
    private static final int C2PA_NOT_SIGNED = -100;

    public MediaItemViewHolder(View itemView, int rootLayoutId) {
        super(itemView, rootLayoutId);
        mDateModifiedView = (TextView)itemView.findViewById(R.id.list_item_date_modified);
        mDurationView = (TextView)itemView.findViewById(R.id.list_item_duration);
        mWaveIndicator = (WaveIndicator) itemView.findViewById(R.id.list_wave_indicator);

        mDateFormatter = new SimpleDateFormat(itemView.getResources().getString(
                R.string.list_item_date_modified_format), java.util.Locale.US);
        mC2paView = (ImageView) itemView.findViewById(R.id.list_cr_image);
        mC2paLayout = (FrameLayout) itemView.findViewById(R.id.list_cr);
        SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences(
                "storage_Path", Context.MODE_PRIVATE);
        mIsC2paEnabled = itemView.getContext().getResources().getBoolean(
                R.bool.c2pa_feature_enabled);
        if (mIsC2paEnabled) {
            Log.d(TAG, "C2PA feature enabled!");
            if (sharedPreferences.getBoolean(ConfigUtil.KEY_C2PA_ENABLED, false)) {
                mFactoryAidl = Utils.getC2paService();
            }
        }
    }

    @Override
    public void setItem(BaseListItem item) {
        super.setItem(item);
        if (item instanceof MediaItem) {
            long dateModified = ((MediaItem)item).getDateModified();
            Date date = new Date(dateModified);
            mDateModifiedView.setText(mDateFormatter.format(date));
            mC2paLayout.setVisibility(View.GONE);
            long duration = ((MediaItem)item).getDuration() / 1000; // millisecond to second
            mDurationView.setText(Utils.timeToString(mDurationView.getContext(), duration));

            updateWaveIndicator(((MediaItem) item).getPlayStatus());
            if (mIsC2paEnabled) {
                if (!mItem.getPath().contains(".m4a")) {
                    mC2paLayout.setVisibility(View.GONE);
                    Log.d(TAG, "item ext not supported C2PA: " + mItem.getPath());
                    return;
                }
                ValidationTask validationTask = new ValidationTask();
                validationTask.execute(new String[] {mItem.getPath()});
                mC2paView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Context context = mC2paView.getContext();
                        if (context != null && mItem != null) {
                            if (mC2paValid) {
                                tryC2PA(mItem.getPath());
                            } else {
                                startInvalidReport();
                            }
                        }
                    }
                });
            }
        }
    }

    private void updateWaveIndicator(MediaItem.PlayStatus status) {
        if (status == MediaItem.PlayStatus.PLAYING) {
            mWaveIndicator.setVisibility(View.VISIBLE);
            mWaveIndicator.animate(true);
        } else if (status == MediaItem.PlayStatus.PAUSE) {
            mWaveIndicator.setVisibility(View.VISIBLE);
            mWaveIndicator.animate(false);
        } else {
            mWaveIndicator.setVisibility(View.GONE);
            mWaveIndicator.animate(false);
        }
    }

    private void tryC2PA(String path) {
        ParcelFileDescriptor pfd = mC2paValidations.get(path);
        if (pfd == null || !pfd.getFileDescriptor().valid()) {
            Log.e(TAG, "tryC2PA PFD is null or PFD is invalid!! filePath = " + path);
            Toast.makeText(mRootView.getContext(), "Please click later, validation is not ready",
                    Toast.LENGTH_SHORT).show();
            mC2paLayout.setVisibility(View.GONE);
            ValidationTask validationTask = new ValidationTask();
            validationTask.execute(new String[] {path});
            return;
        }
        Intent intent = new Intent();
        intent.setClass(mRootView.getContext(), C2PAActivity.class);
        intent.putExtra(C2PAActivity.C2PA_REPORT, pfd.detachFd());
        mRootView.getContext().startActivity(intent);
    }

    private int isC2PASigned(String filepath) {
        Log.d(TAG, "validateC2PA isC2PASigned filepath = " + filepath);
        File file = new File(filepath);
        if (!file.exists()) {
            Log.e(TAG, "validateC2PA file not found");
            return C2PA_NOT_SIGNED;
        }
        try {
            Ashmem ashmem = new Ashmem();

            int[] values = FileUtils.getHardwareBufferFd(filepath);
            if (values == null) {
                Log.e(TAG, "values == null");
                return C2PA_NOT_SIGNED;
            }
            try {
                ashmem.fd = ParcelFileDescriptor.fromFd(values[0]);
                ashmem.size = values[1];
            } catch (IOException e) {
                Log.e(TAG, "ERROR: Failed to get file descriptor : ", e);
                return C2PA_NOT_SIGNED;
            }
            List<C2PADataTypePair> configParams = Utils.getConfigParams(0);
            List<C2PADataTypePair> outputParams = new ArrayList<C2PADataTypePair>();
            int response =
                    mFactoryAidl.validateMedia(ashmem, configParams, outputParams);

            if (response != C2PA_RESPONSE_VALID && response != C2PA_RESPONSE_CORRUPTED) {
                Log.d(TAG, "validateC2PA response = " + response);
                return C2PA_NOT_SIGNED;
            }
            if (response == C2PA_RESPONSE_VALID) {
                mC2paView.setImageResource(R.drawable.ic_c2pa_new);
                if (outputParams != null && outputParams.size() > 0) {
                    Log.d(TAG, "validateC2PA outputParams = " + outputParams);
                    for (C2PADataTypePair pair : outputParams) {
                        if ("VALIDATION_REPORT".equals(pair.key)) {
                            C2PADataType value = pair.value;
                            Ashmem report = value.getFdValue();
                            if (report.size > 0) {
                                mC2paValidations.put(filepath, report.fd);
                                mC2paValid = true;
                                FileUtils.freeFd(values[0]);
                                return C2PA_SIGNED_VALID;
                            }
                        }
                    }
                }
            } else if(response == C2PA_RESPONSE_CORRUPTED) {
                mC2paView.setImageResource(R.drawable.ic_c2pa_invalid_new);
                return C2PA_SIGNED_BUT_INVALID;
            }
        } catch (Exception e) {
            Log.e(TAG, "validateC2PA failed " + e);
            e.printStackTrace();
        }
        return C2PA_NOT_SIGNED;
    }

    private void startInvalidReport() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_C2PA_INVALID, true);
        intent.setClass(mRootView.getContext(), C2PAActivity.class);
        mRootView.getContext().startActivity(intent);
    }

    private class ValidationTask extends AsyncTask<String, Void, Integer> {
        String mFilePath;
        @Override
        protected Integer doInBackground(String... strings) {
            mFilePath = strings[0];
            int c2paResult = isC2PASigned(mFilePath);
            Log.e(TAG, "isC2PASigned c2paResult " + c2paResult);
            if (c2paResult >= C2PA_SIGNED_VALID) {
                return 0;
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (0 == result.intValue()) {
                // success
                mC2paLayout.setVisibility(View.VISIBLE);
            } else {
                mC2paLayout.setVisibility(View.GONE);
            }
        }
    }
}
