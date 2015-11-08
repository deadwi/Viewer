#include "StringConverter_For_JNI.h"

static jclass class_String;
static jmethodID mid_getBytes, mid_getBytesEncoding;
static jmethodID mid_newString, mid_newStringEncoding;

char *jbyteArray2cstr( JNIEnv *env, jbyteArray javaBytes )
{
    size_t len = env->GetArrayLength(javaBytes);
    jbyte *nativeBytes = env->GetByteArrayElements(javaBytes, 0);
    char *nativeStr = (char *)malloc(len+1);
    strncpy( nativeStr, (const char*)nativeBytes, len );
    nativeStr[len] = '\0';
    env->ReleaseByteArrayElements(javaBytes, nativeBytes, JNI_ABORT);

    return nativeStr;
}

jbyteArray cstr2jbyteArray( JNIEnv *env, const char *nativeStr)
{
    jbyteArray javaBytes;
    int len = strlen( nativeStr );
    javaBytes = env->NewByteArray(len);
    env->SetByteArrayRegion(javaBytes, 0, len, (jbyte *) nativeStr );

    return javaBytes;
}

jbyteArray javaGetBytes( JNIEnv *env, jstring str )
{
    if ( mid_getBytes == 0 )
    {
        if ( class_String == 0 )
        {
            jclass cls = env->FindClass("java/lang/String");
            if ( cls == 0 )
            {
                return 0;
            }

            class_String = (jclass)env->NewGlobalRef(cls);
            env->DeleteLocalRef(cls);
            if ( class_String == 0 )
            {
                return 0;
            }
        }

        mid_getBytes = env->GetMethodID(class_String, "getBytes", "()[B");
        if (mid_getBytes == 0)
        {
            return 0;
        }
    }

    return (jbyteArray)env->CallObjectMethod(str, mid_getBytes );
}

jbyteArray javaGetBytesEncoding( JNIEnv *env, jstring str, const char *encoding )
{
    if ( mid_getBytesEncoding == 0 )
    {
        if ( class_String == 0 )
        {
            jclass cls = env->FindClass("java/lang/String");
            if ( cls == 0 )
            {
                return 0;
            }

            class_String = (jclass)env->NewGlobalRef(cls);
            env->DeleteLocalRef(cls);
            if ( class_String == 0 )
            {
                return 0;
            }
        }

        mid_getBytesEncoding = env->GetMethodID(class_String, "getBytes", "(Ljava/lang/String;)[B");
        if (mid_getBytesEncoding == 0)
        {
            return 0;
        }
    }

    jstring jstr = env->NewStringUTF(encoding);
    jbyteArray retArray = (jbyteArray)env->CallObjectMethod(str, mid_getBytesEncoding, jstr);
    env->DeleteLocalRef(jstr);

    return retArray;
}

jstring javaNewString( JNIEnv *env, jbyteArray javaBytes )
{
    if ( mid_newString == 0 )
    {
        if ( class_String == 0 )
        {
            jclass cls = env->FindClass("java/lang/String");
            if ( cls == 0 )
            {
                return 0;
            }

            class_String = (jclass)env->NewGlobalRef(cls);
            env->DeleteLocalRef(cls);
            if ( class_String == 0 )
            {
                return 0;
            }
        }

        mid_newString = env->GetMethodID(class_String, "<init>", "([B)V");
        if ( mid_newString == 0 )
        {
            return 0;
        }
    }

    return (jstring)env->NewObject(class_String, mid_newString, javaBytes );
}

jstring javaNewStringEncoding(JNIEnv *env, jbyteArray javaBytes, const char *encoding )
{
    int len;
    jstring str;
    if ( mid_newString == 0 )
    {
        if ( class_String == 0 )
        {
            jclass cls = env->FindClass("java/lang/String");
            if ( cls == 0 )
            {
                return 0;
            }

            class_String = (jclass)env->NewGlobalRef(cls);
            env->DeleteLocalRef(cls);
            if ( class_String == 0 )
            {
                return 0;
            }

        }

        mid_newString = env->GetMethodID(class_String, "<init>", "([BLjava/lang/String;)V");
        if ( mid_newString == 0 )
        {
            return 0;
        }
    }

    jstring jstr = env->NewStringUTF(encoding);
    str = (jstring)env->NewObject(class_String, mid_newString, javaBytes, jstr);
    env->DeleteLocalRef(jstr);

    return str;
}

jstring javaNewStringChar(JNIEnv* env, const char* nativeStr)
{
    jbyteArray byteArray = cstr2jbyteArray(env, nativeStr);
    jstring jstr = javaNewString(env, byteArray);
    env->DeleteLocalRef(byteArray);

    return jstr;
}

jstring javaNewStringCharEucKR(JNIEnv* env, const char* nativeStr)
{
    jbyteArray byteArray = cstr2jbyteArray(env, nativeStr);
    jstring jstr = javaNewStringEncoding(env, byteArray, "euc-kr");
    env->DeleteLocalRef(byteArray);

    return jstr;
}

char* cstrFromJavaStringEucKR(JNIEnv* env, jstring str)
{
    jbyteArray byteArray = javaGetBytesEncoding(env,str,"euc-kr");
    char* out = jbyteArray2cstr(env,byteArray);
    env->DeleteLocalRef(byteArray);

    return out;
}