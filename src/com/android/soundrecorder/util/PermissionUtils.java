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

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.android.soundrecorder.R;
import com.android.soundrecorder.SoundRecorder;

import java.util.ArrayList;
import java.util.List;


public class PermissionUtils extends Activity{

    public enum PermissionType {
        RECORD, PLAY
    }

    private final static String TAG = "PermissionUtils";
    private static final String PACKAGE_URL_SCHEME = "package:";
    private static final int REQUEST_CODE = 200;
    private static final String PERVIOUS_INTENT = "pervious_intent";
    private static final String REQUEST_PERMISSIONS = "request_permissions";
    private String[] mRequestedPermissons;
    private Intent mPreviousIntent;


    private static String[] RECORD_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAPTURE_AUDIO_OUTPUT,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static String[] PLAY_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreviousIntent = (Intent)getIntent().getExtras().get(PERVIOUS_INTENT);
        mRequestedPermissons = getIntent().getStringArrayExtra(REQUEST_PERMISSIONS);
        if (savedInstanceState == null && mRequestedPermissons != null) {
            String[] neededPermissions =
                    checkRequestedPermission(PermissionUtils.this, mRequestedPermissons);
            if (neededPermissions != null && neededPermissions.length != 0) {
                requestPermissions(neededPermissions, REQUEST_CODE);
            }
        }
    }


    public static String[] getOperationPermissions(PermissionType type) {
        switch (type) {
            case RECORD:
                return RECORD_PERMISSIONS;
            case PLAY:
                return PLAY_PERMISSIONS;
            default:
                return null;
        }
    }

    public static boolean checkAndRequestPermission(Activity activity, String[] permissions) {
        String[] neededPermissions = checkRequestedPermission(activity, permissions);
        if (neededPermissions.length == 0) {
            return false;
        } else {
            Intent intent = new Intent();
            intent.putExtra(PERVIOUS_INTENT, activity.getIntent());
            intent.putExtra(REQUEST_PERMISSIONS, permissions);
            intent.setClass(activity, PermissionUtils.class);
            activity.startActivity(intent);
            return true;
        }
    }

    private static String[] checkRequestedPermission(Activity activity, String[] permissionName) {
        boolean isPermissionGranted = true;
        List<String> needRequestPermission = new ArrayList<String>();
        for (String tmp : permissionName) {
            isPermissionGranted = (PackageManager.PERMISSION_GRANTED ==
                    activity.checkSelfPermission(tmp));
            if (!isPermissionGranted) {
                needRequestPermission.add(tmp);
            }
        }
        String[] needRequestPermissionArray = new String[needRequestPermission.size()];
        needRequestPermission.toArray(needRequestPermissionArray);
        return needRequestPermissionArray;
    }

    public static boolean checkPermissions(final Activity activity, PermissionType type,
            int operationHandle) {
        String[] permissions = getOperationPermissions(type);
        return checkPermissions(activity, permissions, operationHandle);
    }

    public static boolean checkPermissions(final Activity activity, String[] permissions,
            int operationHandle) {
        if (permissions == null)
            return true;
        boolean isPermissionGranted = true;
        ArrayList<String> permissionList = new ArrayList<String>();
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_GRANTED != activity.checkSelfPermission(permission)) {
                permissionList.add(permission);
                isPermissionGranted = false;
            }
        }

        if (!isPermissionGranted) {
            String[] permissionArray = new String[permissionList.size()];
            permissionList.toArray(permissionArray);
            activity.requestPermissions(permissionArray, operationHandle);
        }

        return isPermissionGranted;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                int[] grantResults) {
        boolean isAllPermissionsGranted = true;
        if (requestCode != REQUEST_CODE || permissions == null
                || grantResults == null ||
                permissions.length == 0 || grantResults.length == 0) {
            isAllPermissionsGranted = false;
        } else {
            for (int i : grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED)
                    isAllPermissionsGranted = false;
            }
        }

        if(isAllPermissionsGranted) {
            finish();
        } else {
            showMissingPermissionDialog();
        }
    }

    private void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.dialog_content);
        builder.setTitle(R.string.dialog_title_help);

        builder.setPositiveButton(R.string.dialog_button_settings,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse(PACKAGE_URL_SCHEME + getPackageName()));
                startActivity(intent);
                finish();
            }
        });

        builder.setNegativeButton(R.string.dialog_button_quit,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        builder.show();
    }

    public static boolean checkPermissionResult(String[] permissions, int[] grantResults) {
        if (permissions == null || grantResults == null || permissions.length == 0
                || grantResults.length == 0) {
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
