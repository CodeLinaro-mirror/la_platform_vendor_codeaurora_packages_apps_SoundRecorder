/**
 * Copyright (c) 2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

#include <jni.h>
#include <android/log.h>
#include <vndk/hardware_buffer.h>
#include <aidlcommonsupport/NativeHandle.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <map>
#include <utils/Log.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <errno.h>

using ::aidl::android::hardware::common::NativeHandle;

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "recorder_utils_jni", __VA_ARGS__)
#define SIZE_2MB 0x200000

#define T_ERROR(error_code)                                                      \
    ret = (error_code);                                                          \
    ALOGE("%s::%d err=0x%x errno:%d(%s)", __func__, __LINE__, error_code, errno, \
          strerror(errno));                                                      \
    goto exit;

#define T_CHECK_ERR(cond, error_code) \
    if (!(cond)) {                    \
        T_ERROR(error_code)           \
    }

#define T_CHECK(cond)                        \
    if (!(cond)) {                           \
        ALOGE("%s::%d", __func__, __LINE__); \
        goto exit;                           \
    }

#define LOGD_PRINT(...)      \
    do {                     \
        ALOGD(__VA_ARGS__);  \
        printf(__VA_ARGS__); \
        printf("\n");        \
    } while (0)
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jintArray JNICALL Java_com_android_soundrecorder_util_FileUtils_nativeGetHardwareBufferFd(
        JNIEnv *env, jobject obj, jstring jpath);
JNIEXPORT void JNICALL Java_com_android_soundrecorder_util_FileUtils_nativeFreeFd(
        JNIEnv *env, jobject obj, jint id);

#ifdef __cplusplus
}
#endif

struct AHardwareBufferDeleter {
        void operator()(AHardwareBuffer* buffer) const {
                if (buffer != nullptr) {
                        AHardwareBuffer_release(buffer);
                    }
            }
    };

using HardwareBufferPtr = std::unique_ptr<AHardwareBuffer, AHardwareBufferDeleter>;

void JNICALL Java_com_android_soundrecorder_util_FileUtils_nativeFreeFd(
        JNIEnv *env, jobject obj, jint id){
}

jintArray JNICALL Java_com_android_soundrecorder_util_FileUtils_nativeGetHardwareBufferFd(
        JNIEnv *env, jobject obj, jstring jpath) {
    int32_t ret = 0;
    int32_t fd = -1, ashmemFd = -1, buffLen = 0;
    struct stat appStat = {};
    void *buffer = nullptr, *outBuffer = nullptr;
    size_t copied = 0, fdSize = 0;
    AHardwareBuffer *hwBuffer = nullptr;
    AHardwareBuffer_Desc bufDesc;
    NativeHandle nativeHandleAIDL;
    const native_handle_t* nativeHandle;
    int values[2] = {-1, -1};

    jintArray result = env->NewIntArray(2);

    return result;
}