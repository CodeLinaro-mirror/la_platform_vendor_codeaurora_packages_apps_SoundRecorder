/*
 * Copyright (c) 2016-2017, The Linux Foundation. All rights reserved.

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

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.hardware.common.Ashmem;
import android.hardware.HardwareBuffer;
import android.os.ParcelFileDescriptor;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import android.os.Build;
import android.content.ContentUris;
import android.database.Cursor;
import android.content.ContentResolver;


public class FileUtils {
    static {
        System.loadLibrary("jni_recorderutils");
    }
    public static final int NOT_FOUND = -1;
    public static final int SAVE_FILE_START_INDEX = 1;
    private static final String TAG = "FileUtils";
    private static Map<FileDescriptor, SharedMemory> fd_mem =
            new HashMap<FileDescriptor, SharedMemory>();
    public static String getLastFileName(File file, boolean withExtension) {
        if (file == null) {
            return null;
        }
        return getLastFileName(file.getName(), withExtension);
    }

    public static String getLastFileName(String fileName, boolean withExtension) {
        if (fileName == null) {
            return null;
        }
        if (!withExtension) {
            int dotIndex = fileName.lastIndexOf(".");
            int pathSegmentIndex = fileName.lastIndexOf(File.separator) + 1;
            if (dotIndex == NOT_FOUND) {
                dotIndex = fileName.length();
            }
            if (pathSegmentIndex == NOT_FOUND) {
                pathSegmentIndex = 0;
            }
            fileName = fileName.substring(pathSegmentIndex, dotIndex);
        }
        return fileName;
    }

    public static String getFileExtension(File file, boolean withDot) {
        if (file == null) {
            return null;
        }
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == NOT_FOUND) {
            return null;
        }
        if (withDot) {
            return fileName.substring(dotIndex, fileName.length());
        }
        return fileName.substring(dotIndex + 1, fileName.length());
    }

    public static File renameFile(File file, String newName) {
        if (file == null) {
            return null;
        }
        String filePath = file.getAbsolutePath();
        String folderPath = file.getParent();
        String extension = filePath.substring(filePath.lastIndexOf("."), filePath.length());
        File newFile = new File(folderPath, newName + extension);
        if (file.renameTo(newFile)) {
            return newFile;
        }
        return file;
    }

    public static boolean exists(File file) {
        return file != null && file.exists();
    }

    public static boolean isFolderEmpty(String filePath) {
        File file = new File(filePath);
        File[] files = file.listFiles();
        return files == null || files.length == 0;
    }

    public static boolean deleteFile(File file, Context context) {
        boolean ret = false;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                // remove all files in folder.
                deleteFile(f, context);
            }
        }
        if (file.delete()) {
            // update database.
            DatabaseUtils.delete(context, file);
            ret = true;
        }
        return ret;
    }


    public static Uri uriFromFile(File file, Context context) {
        if (file == null || context == null) {
            return null;
        }

        ContentResolver cr = context.getContentResolver();
        Cursor c = null;
        long id = 0;
        c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.DATA + "=?", new String[] { file.getAbsolutePath() }, null);

        if(c != null){
            c.moveToFirst();
            id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
        }

        Uri uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

        Log.d(TAG,"uriFromFile uri="+uri);
        return uri;
    }

    public static ArrayList<Uri> urisFromFolder(File folder, Context context) {
        if (folder == null || !folder.isDirectory()) {
            return null;
        }

        ArrayList<Uri> uris = new ArrayList<Uri>();
        File[] list = folder.listFiles();
        for (File file : list) {
            if (Build.VERSION.SDK_INT >= 24) {
                uris.add(uriFromFile(file,context));
            } else {
                uris.add(Uri.fromFile(file));
            }
        }
        return uris;
    }

    public static Ashmem getDataInAshmemObj(ByteBuffer pData) {

        int ret = 0;
        int rDataSize = 0;
        Ashmem rAshmem;
        SharedMemory sharedFd = null;
        ByteBuffer bbf = null;
        String s;
        byte[] recieveSM;

        rAshmem = new Ashmem();

        Log.i(TAG, "getDataInAshmem:Enter.");
        if (pData == null) {
            Log.e(TAG, "getDataInAshmem: ERROR: Null ptr passed");
            return null;
        }

        rDataSize = pData.array().length - pData.arrayOffset();
        sharedFd = createSharedMemory(rDataSize);
        Log.i(TAG, "SharedFd : " + Integer.toString(sharedFd.getFileDescriptor().getInt$()));
        try {
            bbf = sharedFd.map(OsConstants.PROT_READ|OsConstants.PROT_WRITE, 0, rDataSize);
        } catch (ErrnoException e) {
            Log.e(TAG, "getDataInAshmem: ERROR: Failed to map Sharedmemory : ", e);
            sharedFd.close();
            return null;
        }
        pData.flip();
        pData.position(pData.arrayOffset() + pData.position());
        bbf.put(pData.array(), pData.position(), rDataSize);

        try {
            rAshmem.fd = ParcelFileDescriptor.dup(sharedFd.getFileDescriptor());
            rAshmem.size = rDataSize;
            fd_mem.put(rAshmem.fd.getFileDescriptor(), sharedFd);
            unmapSharedMemory(rAshmem.fd, bbf);
        } catch (IOException e) {
            Log.e(TAG, "getDataInAshmem: ERROR: Failed to get file descriptor : ", e);
            sharedFd.unmap(bbf);
            sharedFd.close();
            fd_mem.remove(rAshmem.fd);
            return null;
        }

        Log.i(TAG, "getDataInAshmem:Exit.");
        return rAshmem;
    }

    private static SharedMemory createSharedMemory(int size) {

        SharedMemory sFD = null;

        try {
            sFD = SharedMemory.create("", size);
        } catch (ErrnoException e) {
            Log.e(TAG, "createSharedMemory: ERROR: Failed to create Sharedmemory : ", e);
        }

        if (sFD == null || sFD.getSize() != size) {
            Log.e(TAG, "createSharedMemory: ERROR: Failed to allocate shared memory");
            sFD.close();
            return null;
        }

        return sFD;
    }

    private static void unmapSharedMemory(ParcelFileDescriptor pFd, ByteBuffer bBuf) {

        FileDescriptor fd = pFd.getFileDescriptor();
        if (!fd_mem.containsKey(fd)) {
            Log.e(TAG, "unmapSharedMemory: ERROR: FD not found in cached map");
            return;
        }

        fd_mem.get(fd).unmap(bBuf);
    }

    public static ByteBuffer readFileToByteBuffer(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        ByteBuffer byteBuffer;
        try {
            long longLength = raf.length();
            int length = (int) longLength;
            if (length != longLength) throw new IOException("File size >= 2 GB");
            Log.d(TAG,"signC2PA readFileToByteBuffer buffer size =  " + length);
            byte[] data = new byte[length];
            raf.readFully(data);
            byteBuffer = ByteBuffer.allocate(data.length);
            byteBuffer.put(data);
        } finally {
            raf.close();
        }
        return byteBuffer;
    }

    public static int[] getHardwareBufferFd(String filePath) {
        return nativeGetHardwareBufferFd(filePath);
    }

    public static void freeFd(int id) {
        nativeFreeFd(id);
    }

    private native static int[] nativeGetHardwareBufferFd(String filePath);
    private native static void nativeFreeFd(int id);
}
