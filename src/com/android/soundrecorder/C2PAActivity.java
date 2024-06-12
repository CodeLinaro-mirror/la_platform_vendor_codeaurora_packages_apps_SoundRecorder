/**
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.soundrecorder;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.common.Ashmem;
import android.os.Bundle;
import android.view.MenuItem;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.soundrecorder.c2pa.C2PAAdapter;
import com.android.soundrecorder.util.FileUtils;
import com.android.soundrecorder.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import vendor.qti.hardware.c2pa.C2PADataType;
import vendor.qti.hardware.c2pa.C2PADataTypePair;

public class C2PAActivity extends Activity {

    public static final String C2PA_REPORT = "c2pa_report";
    private static final String TAG = C2PAActivity.class.getSimpleName();
    private ArrayList<Item> mItemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c2pa);
        Utils.setUpEdgeToEdge(this);
        setTitle("C2PA Details");
        Intent intent = getIntent();
        int fd = intent.getIntExtra(C2PA_REPORT, -1);
        ParcelFileDescriptor pfd = null;
        try {
            pfd = ParcelFileDescriptor.fromFd(fd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        parseFileDescriptor(pfd);
    }

    public void parseFileDescriptor(ParcelFileDescriptor parcelFileDescriptor) {
        try (FileInputStream fileInputStream = new FileInputStream(
                parcelFileDescriptor.getFileDescriptor())) {
            Log.d(TAG, "parseFileDescriptor size = " + fileInputStream.available());
            StringBuilder stringBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
            }
            String jsonString = stringBuilder.toString();
            // Log.d(TAG, "parseFileDescriptor jsonString = " + jsonString);
            if (jsonString != null && !jsonString.isEmpty()) {
                parseJson(jsonString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void parseJson(String jsonString) {
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

    public class Item {
        private String address;
        private Bitmap thumbnail;
        private String descriptor;
        private String typeLabel;
        private String capturedWith;
        private String capturedWithLabel;
        private String capturedLabel;
        private boolean isAiGenerated;
        private int modifications;
        private String capturedDateText;
        private String signedByText;
        private String signedWithText;

        public Item(String address, Bitmap thumbnail, String descriptor,
                    String typeLabel, String capturedWith, String capturedWithLabel,
                    String capturedLabel, boolean isAiGenerated, int modifications,
                    String capturedDateText, String signedByText, String signedWithText) {
            this.address = address;
            this.thumbnail = thumbnail;
            this.descriptor = descriptor;
            this.typeLabel = typeLabel;
            this.capturedWith = capturedWith;
            this.capturedWithLabel = capturedWithLabel;
            this.capturedLabel = capturedLabel;
            this.isAiGenerated = isAiGenerated;
            this.modifications = modifications;
            this.capturedDateText = capturedDateText;
            this.signedByText = signedByText;
            this.signedWithText = signedWithText;
        }
    }
}
