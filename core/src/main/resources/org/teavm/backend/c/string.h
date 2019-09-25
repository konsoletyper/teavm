#pragma once
#include <stdlib.h>
#include "core.h"

#define TEAVM_STRING(length, hash, s) &(TeaVM_String) { \
    .characters = (TeaVM_Array*) & (struct { TeaVM_Array hdr; char16_t data[(length) + 1]; }) { \
        .hdr = { .size = length }, \
        .data = s \
    }, \
    .hashCode = INT32_C(hash) \
}

#define TEAVM_STRING_FROM_CODES(length, hash, ...) &(TeaVM_String) { \
    .characters = (TeaVM_Array*) & (struct { TeaVM_Array hdr; char16_t data[(length) + 1]; }) { \
        .hdr = { .size = length }, \
        .data = { __VA_ARGS__ } \
    }, \
    .hashCode = INT32_C(hash) \
}

extern int32_t teavm_hashCode(TeaVM_String*);
extern int32_t teavm_equals(TeaVM_String*, TeaVM_String*);
extern TeaVM_Array* teavm_allocateStringArray(int32_t size);
extern TeaVM_Array* teavm_allocateCharArray(int32_t size);
extern TeaVM_String* teavm_createString(TeaVM_Array* chars);

extern char* teavm_stringToC(void*);
extern TeaVM_String* teavm_cToString(char*);
extern char16_t* teavm_stringToC16(void*);
extern TeaVM_String* teavm_c16ToString(char16_t*);
extern char16_t* teavm_mbToChar16(char*, int32_t*);
extern char* teavm_char16ToMb(char16_t*, int32_t);
static inline void teavm_free(void* s) {
    if (s != NULL) {
        free(s);
    }
}

#define TEAVM_SURROGATE_BIT_MASK 0xFC00
#define TEAVM_SURROGATE_INV_BIT_MASK 0x03FF
#define TEAVM_HIGH_SURROGATE_BITS 0xD800
#define TEAVM_LOW_SURROGATE_BITS 0xDC00
#define TEAVM_MIN_SUPPLEMENTARY_CODE_POINT 0x010000

//extern int32_t teavm_utf8_encode(char16_t*, int32_t, char*);
//extern int32_t teavm_utf8_decode(char*, int32_t, char16_t*);

typedef struct TeaVM_StringList {
    char16_t* data;
    int32_t length;
    struct TeaVM_StringList* next;
} TeaVM_StringList;

extern void teavm_disposeStringList(TeaVM_StringList*);
extern TeaVM_StringList* teavm_appendString(TeaVM_StringList*, char16_t*, int32_t);