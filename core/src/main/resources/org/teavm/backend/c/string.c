#include "string.h"
#include "stack.h"
#include <stdlib.h>
#include <string.h>
#include <stddef.h>

#if TEAVM_INCREMENTAL
#define TEAVM_ALLOC_STACK(sz) TEAVM_ALLOC_STACK_DEF(sz, NULL)
#endif

int32_t teavm_hashCode(TeaVM_String* string) {
    int32_t hashCode = INT32_C(0);
    int32_t length = string->characters->size;
    char16_t* chars = TEAVM_ARRAY_DATA(string->characters, char16_t);
    for (int32_t i = INT32_C(0); i < length; ++i) {
        hashCode = 31 * hashCode + chars[i];
    }
    return hashCode;
}

int32_t teavm_equals(TeaVM_String* first, TeaVM_String* second) {
    if (first->characters->size != second->characters->size) {
        return 0;
    }

    char16_t* firstChars = TEAVM_ARRAY_DATA(first->characters, char16_t);
    char16_t* secondChars = TEAVM_ARRAY_DATA(second->characters, char16_t);
    int32_t length = first->characters->size;
    for (int32_t i = INT32_C(0); i < length; ++i) {
        if (firstChars[i] != secondChars[i]) {
            return 0;
        }
    }
    return 1;
}

size_t teavm_mbSize(char16_t* javaChars, int32_t javaCharsCount) {
    size_t sz = 0;
    char buffer[8];
    mbstate_t state = {0};
    for (int32_t i = 0; i < javaCharsCount; ++i) {
        size_t result = c16rtomb(buffer, javaChars[i], &state);
        if (result == (size_t) -1) {
            break;
        }
        sz += result;
    }
    return sz;
}

int32_t teavm_c16Size(char* cstring, size_t count) {
    mbstate_t state = {0};
    int32_t sz = 0;
    while (count > 0) {
        size_t result = mbrtoc16(NULL, cstring, count, &state);
        if (result == (size_t) -1) {
            break;
        } else if ((int) result >= 0) {
            sz++;
            count -= result;
            cstring += result;
        }
    }

    return sz;
}

char* teavm_stringToC(void* obj) {
    if (obj == NULL) {
        return NULL;
    }

    TeaVM_String* javaString = (TeaVM_String*) obj;
    TeaVM_Array* charArray = javaString->characters;
    char16_t* javaChars = TEAVM_ARRAY_DATA(charArray, char16_t);

    size_t sz = teavm_mbSize(javaChars, charArray->size);
    char* result = malloc(sz + 1);

    char* dst = result;
    mbstate_t state = {0};
    for (int32_t i = 0; i < charArray->size; ++i) {
        size_t charResult = c16rtomb(dst, javaChars[i], &state);
        if (charResult == (size_t) -1) {
            break;
        }
        dst += charResult;
    }
    *dst = '\0';
    return result;
}

char16_t* teavm_stringToC16(void* obj) {
    if (obj == NULL) {
        return NULL;
    }

    TeaVM_String* javaString = (TeaVM_String*) obj;
    TeaVM_Array* charArray = javaString->characters;
    char16_t* javaChars = TEAVM_ARRAY_DATA(charArray, char16_t);
    size_t sz = charArray->size;
    char16_t* result = malloc((sz + 1) * sizeof(char16_t));
    if (sz > 0) {
        memcpy(result, javaChars, sz * sizeof(char16_t));
    }
    result[sz] = 0;
    return result;
}

TeaVM_String* teavm_cToString(char* cstring) {
    if (cstring == NULL) {
        return NULL;
    }

    TEAVM_ALLOC_STACK(1);
    TEAVM_CALL_SITE(-1);
    TEAVM_GC_ROOT_RELEASE(0);

    size_t clen = strlen(cstring);
    int32_t size = teavm_c16Size(cstring, clen);
    TeaVM_Array* charArray = teavm_allocateCharArray(size);
    TEAVM_GC_ROOT(0, charArray);

    char16_t* javaChars = TEAVM_ARRAY_DATA(charArray, char16_t);
    mbstate_t state = {0};
    for (int32_t i = 0; i < size; ++i) {
        size_t result = mbrtoc16(javaChars++, cstring, clen, &state);
        if (result == (size_t) -1) {
            break;
        } else if ((int) result >= 0) {
            clen -= result;
            cstring += result;
        }
    }

    TeaVM_String* result = teavm_createString(charArray);
    TEAVM_RELEASE_STACK;
    return result;
}

TeaVM_String* teavm_c16ToString(char16_t* cstring) {
    if (cstring == NULL) {
        return NULL;
    }


    TEAVM_ALLOC_STACK(1);
    TEAVM_CALL_SITE(-1);
    TEAVM_GC_ROOT_RELEASE(0);

    int32_t size = 0;
    while (cstring[size] != 0) {
        ++size;
    }
    TeaVM_Array* charArray = teavm_allocateCharArray(size);
    TEAVM_GC_ROOT(0, charArray);
    char16_t* javaChars = TEAVM_ARRAY_DATA(charArray, char16_t);
    memcpy(javaChars, cstring, size * sizeof(char16_t));

    TeaVM_String* result = teavm_createString(charArray);
    TEAVM_RELEASE_STACK;
    return result;
}

char16_t* teavm_mbToChar16(char* cstring, int32_t* length) {
    size_t clen = strlen(cstring);
    int32_t size = teavm_c16Size(cstring, clen);
    char16_t* javaChars = malloc(sizeof(char16_t) * (size + 2));
    mbstate_t state = {0};
    for (int32_t i = 0; i < size; ++i) {
        size_t result = mbrtoc16(javaChars + i, cstring, clen, &state);
        if (result == (size_t) -1) {
            break;
        } else if ((int) result >= 0) {
            clen -= result;
            cstring += result;
        }
    }
    *length = size;
    return javaChars;
}

char* teavm_char16ToMb(char16_t* javaChars, int32_t length) {
    size_t sz = teavm_mbSize(javaChars, length);
    char* cchars = malloc(sz + 1);

    char* dst = cchars;
    mbstate_t state = {0};
    for (int32_t i = 0; i < length; ++i) {
        size_t result = c16rtomb(dst, javaChars[i], &state);
        if (result == -1) {
            break;
        }
        dst += result;
    }
    *dst = '\0';
    return cchars;
}

void teavm_disposeStringList(TeaVM_StringList* list) {
    while (list != NULL) {
        TeaVM_StringList* next = list->next;
        if (list->data != NULL) {
            free(list->data);
        }
        free(list);
        list = next;
    }
}
TeaVM_StringList* teavm_appendString(TeaVM_StringList* list, char16_t* data, int32_t length) {
    TeaVM_StringList* entry = malloc(sizeof(TeaVM_StringList));
    if (entry == NULL) {
        teavm_disposeStringList(list);
        return NULL;
    }
    entry->data = data;
    entry->length = length;
    entry->next = list;
    return entry;
}
