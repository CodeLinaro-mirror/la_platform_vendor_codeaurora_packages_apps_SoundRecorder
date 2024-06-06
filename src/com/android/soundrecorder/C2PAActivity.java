/**
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.soundrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.soundrecorder.c2pa.C2PAAdapter;
import com.android.soundrecorder.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class C2PAActivity extends Activity {

    public static final String C2PA_REPORT = "c2pa_report";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c2pa);
        Utils.setUpEdgeToEdge(this);
        setTitle("C2PA Details");
        Intent intent = getIntent();
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
