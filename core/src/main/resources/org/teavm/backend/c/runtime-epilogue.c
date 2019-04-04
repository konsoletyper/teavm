static inline int32_t instanceof(void* obj, int32_t (*cls)(JavaClass*)) {
    return obj != NULL && cls(CLASS_OF(obj));
}

static inline void* checkcast(void* obj, int32_t (*cls)(JavaClass*)) {
    return obj == NULL || cls(CLASS_OF(obj)) ? obj : throwClassCastException();
}

static int32_t teavm_hashCode(JavaString* string) {
    int32_t hashCode = INT32_C(0);
    int32_t length = string->characters->size;
    char16_t* chars = ARRAY_DATA(string->characters, char16_t);
    for (int32_t i = INT32_C(0); i < length; ++i) {
        hashCode = 31 * hashCode + chars[i];
    }
    return hashCode;
}

static int32_t teavm_equals(JavaString* first, JavaString* second) {
    if (first->characters->size != second->characters->size) {
        return 0;
    }

    char16_t* firstChars = ARRAY_DATA(first->characters, char16_t);
    char16_t* secondChars = ARRAY_DATA(second->characters, char16_t);
    int32_t length = first->characters->size;
    for (int32_t i = INT32_C(0); i < length; ++i) {
        if (firstChars[i] != secondChars[i]) {
            return 0;
        }
    }
    return 1;
}

static JavaArray* teavm_resourceMapKeys(TeaVM_ResourceMap *map) {
    int32_t size = 0;
    for (int32_t i = 0; i < map->size; ++i) {
        if (map->entries[i].key != NULL) {
            size++;
        }
    }

    int32_t index = 0;
    void* array = teavm_allocateStringArray(size);
    void** data = ARRAY_DATA(array, void*);
    for (int32_t i = 0; i < map->size; ++i) {
        if (map->entries[i].key != NULL) {
            data[index++] = map->entries[i].key;
        }
    }

    return array;
}

static size_t teavm_mbSize(char16_t* javaChars, int32_t javaCharsCount) {
    size_t sz = 0;
    char buffer[__STDC_UTF_16__];
    mbstate_t state = {0};
    for (int32_t i = 0; i < javaCharsCount; ++i) {
        size_t result = c16rtomb(buffer, javaChars[i], &state);
        if (result < 0) {
            break;
        }
        sz += result;
    }
    return sz;
}

static int32_t teavm_c16Size(char* cstring, size_t count) {
    mbstate_t state = {0};
    int32_t sz = 0;
    while (count > 0) {
        size_t result = mbrtoc16(NULL, cstring, count, &state);
        if (result == -1) {
            break;
        } else if (result >= 0) {
            sz++;
            count -= result;
            cstring += result;
        }
    }

    return sz;
}

static char* teavm_stringToC(void* obj) {
    if (obj == NULL) {
        return NULL;
    }

    JavaString* javaString = (JavaString*) obj;
    JavaArray* charArray = javaString->characters;
    char16_t* javaChars = ARRAY_DATA(charArray, char16_t);

    size_t sz = teavm_mbSize(javaChars, charArray->size);
    char* result = malloc(sz + 1);

    int32_t j = 0;
    char* dst = result;
    mbstate_t state = {0};
    for (int32_t i = 0; i < charArray->size; ++i) {
        dst += c16rtomb(dst, javaChars[i], &state);
    }
    *dst = '\0';
    return result;
}

static JavaString* teavm_cToString(char* cstring) {
    if (cstring == NULL) {
        return NULL;
    }

    size_t clen = strlen(cstring);
    int32_t size = teavm_c16Size(cstring, clen);
    JavaArray* charArray = teavm_allocateCharArray(size);
    char16_t* javaChars = ARRAY_DATA(charArray, char16_t);
    mbstate_t state = {0};
    for (int32_t i = 0; i < size; ++i) {
        int32_t result = mbrtoc16(javaChars++, cstring, clen, &state);
        if (result == -1) {
            break;
        } else if (result >= 0) {
            clen -= result;
            cstring += result;
        }
    }
    return teavm_createString(charArray);
}

static inline void teavm_free(void* s) {
    if (s != NULL) {
        free(s);
    }
}

static JavaArray* teavm_parseArguments(int argc, char** argv) {
    JavaArray* array = teavm_allocateStringArray(argc - 1);
    JavaString** arrayData = ARRAY_DATA(array, JavaString*);
    for (int i = 1; i < argc; ++i) {
        arrayData[i - 1] = teavm_cToString(argv[i]);
    }
    return array;
}