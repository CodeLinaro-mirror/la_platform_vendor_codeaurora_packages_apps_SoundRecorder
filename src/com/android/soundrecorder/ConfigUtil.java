/**
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.soundrecorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Range;
import android.widget.Toast;

import java.util.ArrayList;

import org.codeaurora.wrapper.soundrecorder.util.MediaRecorderWrapper;

public class ConfigUtil {

    private Context mContext;
    private String[] mBitRateArray, mSampleRatesArray, mChannelArray, mCodecArray;
    private String[] mSupportedBitRateArray, mSupportedSampleRatesArray, mSupportedChannelArray;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mPrefsStoragePathEditor;

    public ConfigUtil(Context context) {
        mContext = context;
        mBitRateArray = mContext.getResources().getStringArray(R.array.bit_rate_values);
        mSampleRatesArray = mContext.getResources().getStringArray(R.array.sample_rate_values);
        mChannelArray = mContext.getResources().getStringArray(R.array.channel_values);
        mCodecArray = mContext.getResources().getStringArray(R.array.codec_values);
        mSharedPreferences = mContext.getSharedPreferences("storage_Path", Context.MODE_PRIVATE);
        mPrefsStoragePathEditor = mSharedPreferences.edit();
        loadConfig(false);
    }

    public static final String KEY_USE_CUSTOM_CONFIG = "use_custom_config";
    public static final String KEY_BITRATE = "bitrate";
    public static final String KEY_SAMPLE_RATE = "samplerate";
    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_OUTPUT_FORMAT = "outputFormat";
    public static final String KEY_CODEC = "codec";

    private static final String AMR_NB = "AMR_NB";
    private static final String AMR_WB = "AMR_WB";
    private static final String AAC = "AAC_LC";
    private static final String HE_AAC = "HE_AAC_V1";
    private static final String HE_AAC_V2 = "HE_AAC_V2";
    private static final String LPCM = "LPCM";
    private String mExtName;
    private int mOutputFormat = -1;
    private int mCodec = -1;

    private static final ArrayList<AudioEncoderCap> ENCODER_LIST = new ArrayList<>();

    static {
        ENCODER_LIST.add(new AudioEncoderCap(AMR_NB, 5525, 12200, 8000, 8000, 1, 1));
        ENCODER_LIST.add(new AudioEncoderCap(AMR_WB, 6600, 23850, 16000, 16000, 1, 1));
        ENCODER_LIST.add(new AudioEncoderCap(AAC, 8000, 96000, 8000, 48000, 1, 2));
        ENCODER_LIST.add(new AudioEncoderCap(HE_AAC, 8000, 384000, 8000, 48000, 1, 2));
        ENCODER_LIST.add(new AudioEncoderCap(HE_AAC_V2, 8000, 384000, 16000, 48000, 2, 2));
        ENCODER_LIST.add(new AudioEncoderCap(LPCM, 768000, 4608000, 8000, 48000, 1, 2));
    }

    private static int sAacLcMonoMinSupportedBitRate = 8000;
    private static int sAacLcStereoMinSupportedBitRate = 16000;
    private static int sHeAacMonoMinSupportedBitRate1 = 10000;
    private static int sHeAacMonoMinSupportedBitRate2 = 12000;
    private static int sHeAacStereoMinSupportedBitRate1 = 18000;
    private static int sHeAacStereoMinSupportedBitRate2 = 24000;
    private static int sHeAacPsStereoMinSupportedBitRate1 = 10000;
    private static int sHeAacPsStereoMinSupportedBitRate2 = 12000;
    private static int sAacLcMonoMaxSupportedBitRate = 192000;
    private static int sAacLcStereoMaxSupportedBitRate = 384000;
    private static int sHeAacMonoMaxSupportedBitRate = 192000;
    private static int sHeAacStereoMaxSupportedBitRate = 192000;
    private static int sHeAacPstereoMaxSupportedBitRate = 192000;

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

    public void loadConfig(boolean update) {
        int bitRateIndex = mSharedPreferences.getInt(KEY_BITRATE, 0);
        int sampleRatesIndex = mSharedPreferences.getInt(KEY_SAMPLE_RATE, 0);
        int channelIndex = mSharedPreferences.getInt(KEY_CHANNEL, 0);
        int codecIndex = mSharedPreferences.getInt(KEY_CODEC, 0);
        updateConfigOptions(codecIndex, sampleRatesIndex, channelIndex);
        if (update) {
            mPrefsStoragePathEditor.putInt(KEY_BITRATE, checkBitRateIndex(bitRateIndex, false));
            mPrefsStoragePathEditor.putInt(KEY_SAMPLE_RATE,
                    checkSampleRateIndex(sampleRatesIndex, false));
            mPrefsStoragePathEditor.putInt(KEY_CHANNEL, checkChannelIndex(channelIndex, false));
            mPrefsStoragePathEditor.commit();
        }
    }

    public int getBitRate(int index) {
        index = checkBitRateIndex(index, false);
        String config;
        if (mSupportedBitRateArray != null && mSupportedBitRateArray.length > 0) {
            config = mSupportedBitRateArray[index];
        } else {
            config = mBitRateArray[index];
        }
        String[] s = config.split(" ");
        return Integer.parseInt(s[0]) * 1000;
    }

    public int getChannel(int index) {
        return index + 1;
    }

    public int getSampleRate(int index) {
        index = checkSampleRateIndex(index, false);
        String config;
        if (mSupportedSampleRatesArray != null && mSupportedSampleRatesArray.length > 0) {
            config = mSupportedSampleRatesArray[index];
        } else {
            config = mSampleRatesArray[index];
        }
        String[] s = config.split(" ");
        return Integer.parseInt(s[0]);
    }

    public void getSupportedRange(String format, int sampleRateIndex, int channelIndex) {
        mSupportedChannelArray = null;
        mSupportedSampleRatesArray = null;
        mSupportedBitRateArray = null;
        if (format.equals(LPCM)) {
            mSupportedBitRateArray = new String[]{"786 Kbps"};
            return;
        }
        AudioEncoderCap encoderCap = getEncoderCap(format);
        int minBitRate = getMinBitRate(format, getChannel(channelIndex),
                getSampleRate(sampleRateIndex), encoderCap.mMinBitRate);
        int maxBitRate = getMaxBitRate(format, getChannel(channelIndex),
                getSampleRate(sampleRateIndex), encoderCap.mMaxBitRate);
        Range<Integer> bitRange = Range.create(minBitRate, maxBitRate);
        ArrayList<String> supportedBitList = new ArrayList<>();
        for (int i = 0; i < mBitRateArray.length; i++) {
            if (bitRange.contains(getBitRate(i))) {
                supportedBitList.add(mBitRateArray[i]);
            }
        }
        if (supportedBitList != null && supportedBitList.size() > 0) {
            mSupportedBitRateArray = new String[supportedBitList.size()];
            mSupportedBitRateArray = supportedBitList.toArray(mSupportedBitRateArray);
        }
        int minSampleRate = encoderCap.mMinSampleRate;
        int maxSampleRate = encoderCap.mMaxSampleRate;
        if ((format.equals(AAC) || format.equals(HE_AAC)) && channelIndex > 0) {
            minSampleRate *= 2;
        }
        Range<Integer> sampleRange = Range.create(minSampleRate, maxSampleRate);
        ArrayList<String> supportedSampleList = new ArrayList<>();
        for (int i = 0; i < mSampleRatesArray.length; i++) {
            if (sampleRange.contains(getSampleRate(i))) {
                supportedSampleList.add(mSampleRatesArray[i]);
            }
        }
        if (supportedSampleList != null && supportedSampleList.size() > 0) {
            mSupportedSampleRatesArray = new String[supportedSampleList.size()];
            mSupportedSampleRatesArray = supportedSampleList.toArray(mSupportedSampleRatesArray);
        }
        if (encoderCap.mMaxChannels == 1) {
            mSupportedChannelArray = new String[1];
            mSupportedChannelArray[0] = mChannelArray[0];
        }
    }

    public void updateConfigOptions(int codecIndex, int sampleRateIndex, int channelIndex) {
        getSupportedRange(mCodecArray[codecIndex], sampleRateIndex, channelIndex);
    }

    public String[] getSupportedSampleArray() {
        if (mSupportedSampleRatesArray != null && mSupportedSampleRatesArray.length > 0) {
            return mSupportedSampleRatesArray;
        }
        return mSampleRatesArray;
    }

    public String[] getSupportedBitRateArray() {
        if (mSupportedBitRateArray != null && mSupportedBitRateArray.length > 0) {
            return mSupportedBitRateArray;
        }
        return mBitRateArray;
    }

    public String[] getSupportedChannelArray() {
        if (mSupportedChannelArray != null && mSupportedChannelArray.length > 0) {
            return mSupportedChannelArray;
        }
        return mChannelArray;
    }

    public int getBitRateIndex(int bitRate) {
        if (mSupportedBitRateArray != null && mSupportedBitRateArray.length > 0) {
            int i = 0;
            while (i < mSupportedBitRateArray.length) {
                if (bitRate == getBitRate(i)) {
                    return i;
                }
                if (i == 0 && bitRate < getBitRate(0)) {
                    return 0;
                }
                i++;
            }
            return mSupportedBitRateArray.length - 1;
        }
        return -1;
    }

    public int getSampleRateIndex(int sampleRate) {
        if (mSupportedSampleRatesArray != null && mSupportedSampleRatesArray.length > 0) {
            int i = 0;
            while (i < mSupportedSampleRatesArray.length) {
                if (sampleRate == getSampleRate(i)) {
                    return i;
                }
                if (i == 0 && sampleRate < getSampleRate(0)) {
                    return 0;
                }
                i++;
            }
            return mSupportedSampleRatesArray.length - 1;
        }
        return -1;
    }

    public int checkBitRateIndex(int bitRateIndex, boolean showToast) {
        if (bitRateIndex >= getSupportedBitRateArray().length) {
            bitRateIndex = getSupportedBitRateArray().length - 1;
            if (showToast) {
                Toast.makeText(mContext, "bitRate index is too large, force to change to "
                        + bitRateIndex, Toast.LENGTH_SHORT).show();
            }
        }
        return bitRateIndex;
    }

    public int checkSampleRateIndex(int sampleIndex, boolean showToast) {
        if (sampleIndex >= getSupportedSampleArray().length) {
            sampleIndex = getSupportedSampleArray().length - 1;
            if (showToast) {
                Toast.makeText(mContext, "sample rate index is too large, force to change to "
                        + sampleIndex, Toast.LENGTH_SHORT).show();
            }
        }
        return sampleIndex;
    }

    public int checkChannelIndex(int channel, boolean showToast) {
        int channelCount = getSupportedChannelArray().length;
        if (channel > channelCount - 1) {
            channel = channelCount - 1;
            if (showToast) {
                Toast.makeText(mContext, "Channel rate index is too large, force to change to "
                        + channel, Toast.LENGTH_SHORT).show();
            }
        }
        return channel;
    }

    private int getMinBitRate(String format, int channelCount, int sampleRate, int bitRate) {
        if (format.equals(AAC)) {
            if (channelCount == 1) {
                return sAacLcMonoMinSupportedBitRate;
            } else {
                return sAacLcStereoMinSupportedBitRate;
            }
        } else if (format.equals(HE_AAC)) {
            if (channelCount == 1) {
                return (sampleRate == 24000 || sampleRate == 32000) ?
                        sHeAacMonoMinSupportedBitRate1
                        : sHeAacMonoMinSupportedBitRate2;
            } else {
                return (sampleRate == 24000 || sampleRate == 32000) ?
                        sHeAacStereoMinSupportedBitRate1
                        : sHeAacStereoMinSupportedBitRate2;
            }
        } else if (format.equals(HE_AAC_V2)) {
            return (sampleRate == 24000 || sampleRate == 32000) ?
                    sHeAacPsStereoMinSupportedBitRate1
                    : sHeAacPsStereoMinSupportedBitRate2;
        } else {
            return bitRate;
        }
    }

    private int getMaxBitRate(String format, int channelCount, int sampleRate, int bitRate) {
        if (format.equals(AAC)) {
            if (channelCount == 1) {
                return Math.min(sAacLcMonoMaxSupportedBitRate, 6 * sampleRate);
            } else {
                return Math.min(sAacLcStereoMaxSupportedBitRate, 12 * sampleRate);
            }
        } else if (format.equals(HE_AAC)) {
            if (channelCount == 1) {
                return Math.min(sHeAacMonoMaxSupportedBitRate, 6 * sampleRate);
            } else {
                return Math.min(sHeAacStereoMaxSupportedBitRate, 12 * sampleRate);
            }
        } else if (format.equals(HE_AAC_V2)) {
            return Math.min(sHeAacPstereoMaxSupportedBitRate, 6 * sampleRate);
        } else {
            return bitRate;
        }
    }

    private AudioEncoderCap getEncoderCap(String codec) {
        for (AudioEncoderCap cap : ENCODER_LIST) {
            if (cap.mCodec.equals(codec)) {
                return cap;
            }
        }
        return null;
    }

    private static class AudioEncoderCap {
        public final String mCodec;
        public final int mMinChannels, mMaxChannels;     // min and max number of channels
        public final int mMinSampleRate, mMaxSampleRate; // min and max sample rate (hz)
        public final int mMinBitRate, mMaxBitRate;       // min and max bit rate (bps)

        private AudioEncoderCap(String codec,
                                int minBitRate, int maxBitRate,
                                int minSampleRate, int maxSampleRate,
                                int minChannels, int maxChannels) {
            mCodec = codec;
            mMinBitRate = minBitRate;
            mMaxBitRate = maxBitRate;
            mMinSampleRate = minSampleRate;
            mMaxSampleRate = maxSampleRate;
            mMinChannels = minChannels;
            mMaxChannels = maxChannels;
        }
    }
}
