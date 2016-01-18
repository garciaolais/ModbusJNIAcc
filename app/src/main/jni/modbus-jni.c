/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include "lib/src/modbus.h"
#include <errno.h>
#include <android/log.h>
#include <sys/endian.h>

/* This is a trivial JNI fireraven where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/fireraven/libmodbus/main.java
 */
void
Java_com_fireraven_libmodbus_main_foo( JNIEnv* env,
                                       jobject thiz )
{

}


void modbus_set_float_array(int offset, uint16_t *arr,float f);
void modbus_set_float_badc(float f, uint16_t *dest);


modbus_t *ctx;

#define LOGD(LOG_TAG, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGV(LOG_TAG, ...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGE(LOG_TAG, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

jint
Java_com_fireraven_libmodbus_main_modbusConnect(JNIEnv* env,
                                                  jobject thiz,
                                                  jstring ip,
                                                  jint port)
{
    const char *DEBUG= "modbusConnect";
    const char *nativeIp = (*env)->GetStringUTFChars(env, ip, 0);

    ctx = modbus_new_tcp(nativeIp, port);
    if(modbus_connect(ctx) == -1){
        LOGD(DEBUG,"ERROR");
        LOGE(DEBUG,"modus_connect: %s",modbus_strerror(errno));
        modbus_free(ctx);
        return -1;
    }
    return 0;
}

jint
Java_com_fireraven_libmodbus_main_modbusSendSensor(JNIEnv* env,
                                                     jobject thiz,
                                                     jfloat axisX,
                                                     jfloat axisY,
                                                     jfloat axisZ)
{
    const char *DEBUG= "modbusSendSensor";
    int nb = 6;
    int rc = 0;
    uint16_t *tab_rp_registers;

    tab_rp_registers=(uint16_t *) malloc(nb * sizeof(uint16_t));
    memset(tab_rp_registers, 0, nb * sizeof(uint16_t));
    modbus_set_float_array(0, tab_rp_registers, axisX);
    modbus_set_float_array(2, tab_rp_registers, axisY);
    modbus_set_float_array(4, tab_rp_registers, axisZ);
    rc = modbus_write_registers(ctx, 0, nb, tab_rp_registers);

    if( rc != nb ){
        LOGE(DEBUG,"ERROR");
        LOGE(DEBUG,"modbus_write_registers: %s",modbus_strerror(errno));
        free(tab_rp_registers);
        return -1;
    }

    free(tab_rp_registers);
    tab_rp_registers = NULL;
    return 0;
}

void
Java_com_fireraven_libmodbus_main_modbusDisconnect(JNIEnv* env,
                                                     jobject thiz)
{
    modbus_close(ctx);
    modbus_free(ctx);
}

void modbus_set_float_array(int offset, uint16_t *arr,float f)
{
    int i;
    int nb;
    uint16_t *tmp;

    nb = 2;
    tmp = (uint16_t*)malloc(nb * sizeof(uint16_t));
    memset(tmp,0, nb * sizeof(uint16_t));
    modbus_set_float_badc(f, tmp);

    for (i = 0; i < nb; i++) {
        arr[i + offset] = tmp[i];
    }

    free(tmp);
}

/* Set a float to 4 bytes for Modbus with byte swap conversion (BADC) */
void modbus_set_float_badc(float f, uint16_t *dest)
{
    uint32_t i;

    memcpy(&i, &f, sizeof(uint32_t));
    i = htonl(i);
    dest[0] = (uint16_t)__builtin_bswap16(i >> 16);
    dest[1] = (uint16_t)__builtin_bswap16(i & 0xFFFF);
}




