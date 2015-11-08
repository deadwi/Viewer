#ifndef _STRING_CONVERTER_FOR_JNI_
#define _STRING_CONVERTER_FOR_JNI_

#include <jni.h>
#include <stdio.h>
#include <string.h>

#ifdef __cplusplus
extern "C"
{
#endif

char *jbyteArray2cstr( JNIEnv *env, jbyteArray javaBytes );
jbyteArray cstr2jbyteArray( JNIEnv *env, const char *nativeStr);
jbyteArray javaGetBytes( JNIEnv *env, jstring str );
jbyteArray javaGetBytesEncoding( JNIEnv *env, jstring str, const char *encoding );
jstring javaNewString( JNIEnv *env, jbyteArray javaBytes );
jstring javaNewStringEncoding(JNIEnv *env, jbyteArray javaBytes, const char *encoding );

jstring javaNewStringChar(JNIEnv* env, const char* nativeStr);
jstring javaNewStringCharEucKR(JNIEnv* env, const char* nativeStr);
char* cstrFromJavaStringEucKR(JNIEnv* env, jstring str);

#ifdef __cplusplus
}
#endif

#endif //_STRING_CONVERTER_FOR_JNI_
