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