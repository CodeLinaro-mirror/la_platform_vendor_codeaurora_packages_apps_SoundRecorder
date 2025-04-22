/**
 * Copyright (c) 2024, 2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.soundrecorder;

import static com.android.soundrecorder.filelist.FileListFragment.MIME_TYPE_AUDIO;
import static com.android.soundrecorder.util.Utils.EXTRA_C2PA_INVALID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.common.Ashmem;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.soundrecorder.c2pa.C2PAAdapter;
import com.android.soundrecorder.util.FileUtils;
import com.android.soundrecorder.util.Utils;
import com.truepic.lensverify.data.c2padata.C2PAData;
import com.truepic.lensverify.data.c2padata.ManifestStore;
import com.truepic.lensverify.utils.C2PAPresenter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
        boolean c2paInvalid = intent.getBooleanExtra(EXTRA_C2PA_INVALID, false);
        ImageView backButton = findViewById(R.id.back);
        backButton.setClickable(true);
        backButton.setOnClickListener(view -> finish());
        if (c2paInvalid) {
            Log.d(TAG, "onCreate c2paInvalid, show Error message");
            TextView message = (TextView) findViewById(R.id.message);
            RecyclerView list = (RecyclerView) findViewById(R.id.list);
            message.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        } else {
            int fd = intent.getIntExtra(C2PA_REPORT, -1);
            ParcelFileDescriptor pfd = null;
            try {
                pfd = ParcelFileDescriptor.fromFd(fd);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            parseFileDescriptor(pfd);
        }
    }

    public void parseFileDescriptor(ParcelFileDescriptor parcelFileDescriptor) {
        try (FileInputStream fileInputStream = new FileInputStream(
                parcelFileDescriptor.getFileDescriptor())) {
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
        C2PAData data = Utils.jsonToC2PAData(jsonString);
        Resources res = getResources();
        C2PAPresenter.Labels labels = new C2PAPresenter.Labels(
                "",
                "",
                "",
                res.getString(R.string.c2pa_info_thumbnail_type_photo),
                res.getString(R.string.c2pa_info_thumbnail_type_image),
                res.getString(R.string.c2pa_info_thumbnail_type_video),
                res.getString(R.string.c2pa_info_thumbnail_type_audio),
                res.getString(R.string.c2pa_info_captured),
                res.getString(R.string.c2pa_info_created),
                res.getString(R.string.c2pa_info_captured_with),
                res.getString(R.string.c2pa_info_created_with)
        );

        C2PAPresenter presenter = new C2PAPresenter(MIME_TYPE_AUDIO, data, labels);
        ArrayList<Item> list = new ArrayList<>();
        List<ManifestStore> reversedManifests = presenter.getManifests();
        Collections.reverse(reversedManifests);
        for (ManifestStore manifestStore : reversedManifests) {
            Item item = new Item(
                    getAddress(this, manifestStore),
                    presenter.getThumbnail(manifestStore, 200),
                    "",
                    presenter.getType(),
                    presenter.getTypeLabel(),
                    presenter.getCapturedWith(manifestStore),
                    presenter.getCapturedWithLabel(manifestStore),
                    presenter.getCapturedLabel(manifestStore),
                    presenter.isAiGenerated(manifestStore),
                    presenter.getModifications(manifestStore),
                    presenter.getCapturedDate(manifestStore),
                    presenter.getSignedBy(manifestStore)
            );
            list.add(item);
        }

        C2PAAdapter adapter = new C2PAAdapter(list, this);

        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(
                this, LinearLayoutManager.VERTICAL);
        recyclerView.addItemDecoration(divider);
        recyclerView.setAdapter(adapter);
    }

    public static String buildAddress(Address address) {
        if (address == null) return "";

        StringBuilder ret = new StringBuilder();

        if(address.getLocality() != null && !address.getLocality().isEmpty()) {
            ret.append(address.getLocality());
        }

        if(address.getAdminArea() != null && !address.getAdminArea().isEmpty()) {
            if(ret.length() > 0) ret.append(", ");
            ret.append(stateAbbreviation(address.getAdminArea()));
        }


        if(address.getCountryCode() != null && !address.getCountryCode().isEmpty()) {
            if(ret.length() > 0) ret.append(", ");

            if(address.getLocality() != null && !address.getLocality().isEmpty()) {
                ret.append(address.getCountryCode());
            } else {
                ret.append(address.getCountryName());
            }
        }

        return ret.toString();
    }

    public static String stateAbbreviation(String state) {
        return switch (state) {
            case "Alabama" -> "AL";
            case "Alaska" -> "AK";
            case "Alberta" -> "AB";
            case "American Samoa" -> "AS";
            case "Arizona" -> "AZ";
            case "Arkansas" -> "AR";
            case "Armed Forces (AE)" -> "AE";
            case "Armed Forces Americas" -> "AA";
            case "Armed Forces Pacific" -> "AP";
            case "British Columbia" -> "BC";
            case "California" -> "CA";
            case "Colorado" -> "CO";
            case "Connecticut" -> "CT";
            case "Delaware" -> "DE";
            case "District Of Columbia" -> "DC";
            case "Florida" -> "FL";
            case "Georgia" -> "GA";
            case "Guam" -> "GU";
            case "Hawaii" -> "HI";
            case "Idaho" -> "ID";
            case "Illinois" -> "IL";
            case "Indiana" -> "IN";
            case "Iowa" -> "IA";
            case "Kansas" -> "KS";
            case "Kentucky" -> "KY";
            case "Louisiana" -> "LA";
            case "Maine" -> "ME";
            case "Manitoba" -> "MB";
            case "Maryland" -> "MD";
            case "Massachusetts" -> "MA";
            case "Michigan" -> "MI";
            case "Minnesota" -> "MN";
            case "Mississippi" -> "MS";
            case "Missouri" -> "MO";
            case "Montana" -> "MT";
            case "Nebraska" -> "NE";
            case "Nevada" -> "NV";
            case "New Brunswick" -> "NB";
            case "New Hampshire" -> "NH";
            case "New Jersey" -> "NJ";
            case "New Mexico" -> "NM";
            case "New York" -> "NY";
            case "Newfoundland" -> "NF";
            case "North Carolina" -> "NC";
            case "North Dakota" -> "ND";
            case "Northwest Territories" -> "NT";
            case "Nova Scotia" -> "NS";
            case "Nunavut" -> "NU";
            case "Ohio" -> "OH";
            case "Oklahoma" -> "OK";
            case "Ontario" -> "ON";
            case "Oregon" -> "OR";
            case "Pennsylvania" -> "PA";
            case "Prince Edward Island" -> "PE";
            case "Puerto Rico" -> "PR";
            case "Quebec" -> "PQ";
            case "Rhode Island" -> "RI";
            case "Saskatchewan" -> "SK";
            case "South Carolina" -> "SC";
            case "South Dakota" -> "SD";
            case "Tennessee" -> "TN";
            case "Texas" -> "TX";
            case "Utah" -> "UT";
            case "Vermont" -> "VT";
            case "Virgin Islands" -> "VI";
            case "Virginia" -> "VA";
            case "Washington" -> "WA";
            case "West Virginia" -> "WV";
            case "Wisconsin" -> "WI";
            case "Wyoming" -> "WY";
            case "Yukon Territory" -> "YT";
            default -> state;
        };
    }

    public static String getAddress(Context context, ManifestStore manifestStore) {
        AtomicReference<String> retAddress = new AtomicReference<>(null);

        if (manifestStore != null && manifestStore.getAssertions() != null
                && manifestStore.getAssertions().getMetadata() != null) {
            manifestStore.getAssertions().getMetadata().forEach(it -> {
                try {
                    if (it.getData() != null && it.getData().getLongitude() != null
                            && !it.getData().getLongitude().isEmpty()
                            && it.getData().getLatitude() != null
                            && !it.getData().getLatitude().isEmpty()) {
                        double longitude = Double.parseDouble(it.getData().getLongitude());
                        double latitude = Double.parseDouble(it.getData().getLatitude());

                        if (!Geocoder.isPresent()) {
                            // geocoding not present, fallback to coordinates
                            retAddress.set(latitude + "," + longitude);
                            return;
                        }

                        Geocoder geocoder = new Geocoder(context);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            CountDownLatch countDownLatch = new CountDownLatch(1);
                            geocoder.getFromLocation(latitude, longitude, 1, addresses -> {
                                retAddress.set(buildAddress(addresses.get(0)));
                                countDownLatch.countDown();
                            });
                            countDownLatch.await();
                        } else {
                            try {
                                retAddress.set(buildAddress(
                                        geocoder.getFromLocation(latitude, longitude, 1).get(0)));
                            } catch (Exception e) {
                                retAddress.set(latitude + "," + longitude);
                            }
                        }
                    }
                } catch(Exception e) {
                    // process or ignore
                }
            });
        }

        return retAddress.get();
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
        private C2PAPresenter.Type type;
        private String typeLabel;
        private String capturedWith;
        private String capturedWithLabel;
        private String capturedLabel;
        private boolean isAiGenerated;
        private int modifications;
        private String capturedDateText;
        private String signedByText;

        public Item(String address, Bitmap thumbnail, String descriptor, C2PAPresenter.Type type,
                    String typeLabel, String capturedWith, String capturedWithLabel,
                    String capturedLabel, boolean isAiGenerated, int modifications,
                    String capturedDateText, String signedByText) {
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
        }


        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Item : ");
            sb.append("Descriptor = " + descriptor);
            sb.append(", capturedWith = " + capturedWith);
            sb.append(", capturedWithLabel = " + capturedWithLabel);
            sb.append(", modifications = " + modifications);
            sb.append(", isAiGenerated = " + isAiGenerated);
            sb.append(", capturedDateText = " + capturedDateText);
            sb.append(", signedByText = " + signedByText);
            sb.append(", address = " + address);
            return sb.toString();
        }

        public String getCapturedWith() {
            return capturedWith;
        }

        public String getAddress() {
            return address;
        }

        public Bitmap getThumbnail() {
            return thumbnail;
        }

        public int getModifications() {
            return modifications;
        }

        public boolean isAiGenerated() {
            return isAiGenerated;
        }

        public String getCapturedDateText() {
            return capturedDateText;
        }

        public String getSignedByText() {
            return signedByText;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public String getDescriptor() {
            return descriptor;
        }
        public String getCapturedWithLabel() {
            return capturedWithLabel;
        }
        public String getCapturedLabel() {
            return capturedLabel;
        }
    }
}
