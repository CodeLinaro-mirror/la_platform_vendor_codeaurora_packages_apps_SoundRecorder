/**
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.soundrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;

public class SettingsActivity extends Activity {
    private Spinner mBitRatesView, mSampleRatesView, mChannelView, mCodecView;
    private Spinner[] mSpinners;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mPrefsStoragePathEditor;
    private int mBitRateIndex, mSampleRatesIndex, mChannelIndex, mOutputFormatIndex, mCodecIndex;
    private CheckBox mCustomConfigView;
    private boolean mUseCustomConfig;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setTitle("Settings");
        initView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initView() {
        mCustomConfigView = findViewById(R.id.custom_settings);
        mBitRatesView = findViewById(R.id.setting_bitrate);
        mSampleRatesView = findViewById(R.id.setting_samplerate);
        mChannelView = findViewById(R.id.setting_channel);
        mCodecView = findViewById(R.id.setting_codec);
        mSpinners = new Spinner[] {mBitRatesView, mSampleRatesView,
                        mChannelView, mCodecView};
        mSharedPreferences = getSharedPreferences("storage_Path", Context.MODE_PRIVATE);
        mPrefsStoragePathEditor = mSharedPreferences.edit();
        loadConfig();
        setListener();
        updateUI();
    }

    private void loadConfig() {
        mUseCustomConfig =
                mSharedPreferences.getBoolean(ConfigUtil.KEY_USE_CUSTOM_CONFIG, false);
        mBitRateIndex = mSharedPreferences.getInt(ConfigUtil.KEY_BITRATE, 0);
        mSampleRatesIndex = mSharedPreferences.getInt(ConfigUtil.KEY_SAMPLE_RATE, 0);
        mChannelIndex = mSharedPreferences.getInt(ConfigUtil.KEY_CHANNEL, 0);
        mOutputFormatIndex = mSharedPreferences.getInt(ConfigUtil.KEY_OUTPUT_FORMAT, 0);
        mCodecIndex = mSharedPreferences.getInt(ConfigUtil.KEY_CODEC, 0);
        mBitRatesView.setSelection(mBitRateIndex);
        mSampleRatesView.setSelection(mSampleRatesIndex);
        mChannelView.setSelection(mChannelIndex);
        mCodecView.setSelection(mCodecIndex);
        mCustomConfigView.setChecked(mUseCustomConfig);
    }

    private void setListener() {
        for (Spinner spinner: mSpinners) {
            if (spinner != null) {
                spinner.setOnItemSelectedListener(mListener);
            }
        }
        mCustomConfigView.setOnCheckedChangeListener((buttonView, checked) -> {
            mUseCustomConfig = checked;
            saveConfig();
            updateUI();
        });
    }

    private void updateUI() {
        for (Spinner spinner: mSpinners) {
            if (spinner != null) {
                spinner.setEnabled(mUseCustomConfig);
            }
        }
    }

    private void saveConfig() {
        mPrefsStoragePathEditor.putInt(ConfigUtil.KEY_BITRATE, mBitRateIndex);
        mPrefsStoragePathEditor.putInt(ConfigUtil.KEY_SAMPLE_RATE, mSampleRatesIndex);
        mPrefsStoragePathEditor.putInt(ConfigUtil.KEY_CHANNEL, mChannelIndex);
        mPrefsStoragePathEditor.putInt(ConfigUtil.KEY_OUTPUT_FORMAT, mOutputFormatIndex);
        mPrefsStoragePathEditor.putInt(ConfigUtil.KEY_CODEC, mCodecIndex);
        mPrefsStoragePathEditor.putBoolean(ConfigUtil.KEY_USE_CUSTOM_CONFIG, mUseCustomConfig);
        mPrefsStoragePathEditor.commit();
    }

    private AdapterView.OnItemSelectedListener mListener =
            new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (parent == mBitRatesView) {
                mBitRateIndex = position;
            } else if (parent == mSampleRatesView) {
                mSampleRatesIndex = position;
            } else if (parent == mChannelView) {
                mChannelIndex = position;
            } else if (parent == mCodecView) {
                mCodecIndex = position;
            }
            saveConfig();
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };
}
