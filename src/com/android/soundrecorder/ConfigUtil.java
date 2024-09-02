/**
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.soundrecorder;

import android.content.Context;

import org.codeaurora.wrapper.soundrecorder.util.MediaRecorderWrapper;

public class ConfigUtil {

    private Context mContext;
    private String[] mBitRateArray, mSampleRatesArray, mChannelArray, mCodecArray;

    public ConfigUtil(Context context) {
        mContext = context;
        mBitRateArray = mContext.getResources().getStringArray(R.array.bit_rate_values);
        mSampleRatesArray = mContext.getResources().getStringArray(R.array.sample_rate_values);
        mChannelArray = mContext.getResources().getStringArray(R.array.channel_values);
        mCodecArray = mContext.getResources().getStringArray(R.array.codec_values);
    }

    public static final String KEY_USE_CUSTOM_CONFIG = "use_custom_config";
    public static final String KEY_BITRATE = "bitrate";
    public static final String KEY_SAMPLE_RATE = "samplerate";
    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_OUTPUT_FORMAT = "outputFormat";
    public static final String KEY_CODEC = "codec";
    public static final String KEY_C2PA_ENABLED = "c2pa_enabled";
    private static final String AMR_NB = "AMR_NB";
    private static final String AMR_WB = "AMR_WB";
    private static final String AAC = "AAC_LC";
    private static final String HE_AAC = "HE_AAC_V1";
    private static final String HE_AAC_V2 = "HE_AAC_V2";
    private static final String LPCM = "LPCM";
    private String mExtName;
    private int mOutputFormat = -1;
    private int mCodec = -1;

    public String getExtName() {
        return mExtName == null ? ".amr" : mExtName;
    }

    public int getOutputFormat() {
        return mOutputFormat == -1 ? MediaRecorderWrapper.OutputFormat.RAW_AMR : mOutputFormat;
    }

    public int getCodec() {
        return mCodec == -1 ? MediaRecorderWrapper.AudioEncoder.AMR_NB : mCodec;
    }

    public void updateConfigs(int index) {
        String config = mCodecArray[index];
        if (AMR_NB.equals(config)) {
            mOutputFormat = MediaRecorderWrapper.OutputFormat.RAW_AMR;
            mExtName = ".amr";
            mCodec = MediaRecorderWrapper.AudioEncoder.AMR_NB;
            return;
        }
        if (AMR_WB.equals(config)) {
            mExtName = ".awb";
            mOutputFormat = MediaRecorderWrapper.OutputFormat.AMR_WB;
            mCodec = MediaRecorderWrapper.AudioEncoder.AMR_WB;
            return;
        }
        if (AAC.equals(config)) {
            mExtName = ".aac";
            mOutputFormat = MediaRecorderWrapper.OutputFormat.THREE_GPP;
            mCodec = MediaRecorderWrapper.AudioEncoder.AAC;
            return;
        }
        if (HE_AAC.equals(config)) {
            mExtName = ".aac";
            mOutputFormat = MediaRecorderWrapper.OutputFormat.THREE_GPP;
            mCodec = MediaRecorderWrapper.AudioEncoder.HE_AAC;
            return;
        }
        if (HE_AAC_V2.equals(config)) {
            mExtName = ".aac";
            mOutputFormat = MediaRecorderWrapper.OutputFormat.THREE_GPP;
            mCodec = MediaRecorderWrapper.AudioEncoder.HE_AAC_V2;
            return;
        }
        if (LPCM.equals(config)) {
            mExtName = ".wav";
            mOutputFormat = MediaRecorderWrapper.OutputFormat.WAVE;
            mCodec = MediaRecorderWrapper.AudioEncoder.LPCM;
            return;
        }

        mOutputFormat = MediaRecorderWrapper.OutputFormat.RAW_AMR;
        mExtName = ".amr";
        mCodec = MediaRecorderWrapper.AudioEncoder.AMR_NB;
        return;
    }

    public int getBitRate(int index) {
        String config = mBitRateArray[index];
        String[] s = config.split(" ");
        return Integer.parseInt(s[0]) * 1000;
    }

    public int getChannel(int index) {
            return index + 1;
    }

    public int getSampleRate(int index) {
        String config = mSampleRatesArray[index];
        String[] s = config.split(" ");
        return Integer.parseInt(s[0]);
    }
}
