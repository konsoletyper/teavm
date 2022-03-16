#include "runtime.h"
#include "time.h"
#include "fiber.h"
#include "string.h"
#include "definitions.h"
#include <string.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>
#include <math.h>
#include <stdarg.h>

#if TEAVM_INCREMENTAL
    #include "virtcall.h"
#endif

#if TEAVM_UNIX
    #include <locale.h>
#endif

#if TEAVM_WINDOWS
    #include <Windows.h>
#endif

char *teavm_beforeClasses;

double teavm_rand() {
    return rand() / ((double) RAND_MAX + 1);
}

void teavm_beforeInit() {
    srand((unsigned int) time(NULL));

    #if TEAVM_UNIX
        setlocale (LC_ALL, "");
    #endif

    teavm_initFiber();
    teavm_initTime();
}

TeaVM_Array* teavm_parseArguments(int argc, char** argv) {
    TeaVM_Array* array = teavm_allocateStringArray(argc > 0 ? argc - 1 : 0);
    TeaVM_String** arrayData = TEAVM_ARRAY_DATA(array, TeaVM_String*);
    for (int i = 1; i < argc; ++i) {
        arrayData[i - 1] = teavm_cToString(argv[i]);
    }
    return array;
}

void teavm_afterInitClasses() {
    teavm_initStaticGcRoots();
    #if TEAVM_INCREMENTAL
        teavm_vc_done();
    #endif
}

TeaVM_Class* teavm_classClass;
TeaVM_Class* teavm_objectClass;
TeaVM_Class* teavm_stringClass;
TeaVM_Class* teavm_charArrayClass;

void teavm_initClasses() {
    teavm_beforeClasses = (char*) teavm_classReferences[0];
    for (int i = 1; i < teavm_classReferencesCount; ++i) {
        char* c = (char*) teavm_classReferences[i];
        if (c < teavm_beforeClasses) teavm_beforeClasses = c;
    }
    teavm_beforeClasses -= 4096;
    int32_t classHeader = TEAVM_PACK_CLASS(teavm_classClass) | (int32_t) INT32_C(0x80000000);
    for (int i = 0; i < teavm_classReferencesCount; ++i) {
        teavm_classReferences[i]->parent.header = classHeader;
        teavm_classReferences[i]->services = NULL;
    }
}

#define TEAVM_FILL_ARRAY_F(name, type, arrayType) \
    void* name(void* array, ...) { \
        type* data = TEAVM_ARRAY_DATA(array, type); \
        int32_t size = TEAVM_ARRAY_LENGTH(array); \
        va_list args; \
        va_start(args, array); \
        for (int32_t i = 0; i < size; ++i) { \
            *data++ = (type) va_arg(args, arrayType); \
        } \
        va_end(args); \
        return array; \
    }

TEAVM_FILL_ARRAY_F(teavm_fillArray, void*, void*)
TEAVM_FILL_ARRAY_F(teavm_fillBooleanArray, int8_t, int)
TEAVM_FILL_ARRAY_F(teavm_fillByteArray, int8_t, int)
TEAVM_FILL_ARRAY_F(teavm_fillShortArray, int16_t, int)
TEAVM_FILL_ARRAY_F(teavm_fillCharArray, char16_t, int)
TEAVM_FILL_ARRAY_F(teavm_fillIntArray, int32_t, int)
TEAVM_FILL_ARRAY_F(teavm_fillLongArray, int64_t, int64_t)
TEAVM_FILL_ARRAY_F(teavm_fillFloatArray, float, double)
TEAVM_FILL_ARRAY_F(teavm_fillDoubleArray, double, double)
