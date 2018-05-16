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

static inline int teavm_isHighSurrogate(char16_t c) {
    return (c & 0xFC00) == 0xD800;
}

static inline int teavm_isLowSurrogate(char16_t c) {
    return (c & 0xFC00) == 0xDC00;
}

static inline int teavm_isSurrogatePair(char16_t* chars, int32_t index, int32_t limit) {
    return index < limit - 1 && teavm_isHighSurrogate(chars[index]) && teavm_isLowSurrogate(chars[index + 1]);
}

static inline int teavm_getCodePoint(char16_t* chars, int32_t *index, int32_t limit) {
    wchar_t codePoint;
    if (teavm_isSurrogatePair(chars, *index, limit)) {
        codePoint = (wchar_t) (((((chars[*index] & 0x03FF) << 10) | chars[*index + 1] & 0x03FF)) + 0x010000);
        (*index)++;
    } else {
        codePoint = (wchar_t) chars[*index];
    }
    return codePoint;
}

static size_t teavm_mbSize(char16_t* javaChars, int32_t javaCharsCount) {
    size_t sz = 0;
    char buffer[6];
    for (int32_t i = 0; i < javaCharsCount; ++i) {
        sz += wctomb(buffer, teavm_getCodePoint(javaChars, &i, javaCharsCount));
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
    for (int32_t i = 0; i < charArray->size; ++i) {
        dst += wctomb(dst, teavm_getCodePoint(javaChars, &i, charArray->size));
    }
    *dst = '\0';
    return result;
}

static inline void teavm_free(void* s) {
    if (s != NULL) {
        free(s);
    }
}