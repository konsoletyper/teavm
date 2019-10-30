#include "virtcall.h"
#include "definitions.h"
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>

typedef struct {
    const char16_t* name;
    int32_t hash;
    int32_t id;
} TeaVM_MethodHashEntry;

static int32_t teavm_vc_hashSize;
static int32_t teavm_vc_hashCapacity;
static int32_t teavm_vc_hashThreshold;
static TeaVM_MethodHashEntry* teavm_vc_hashData = NULL;

static int32_t teavm_vc_classTableSize;
static int32_t teavm_vc_classTableCapacity;
static TeaVM_DynamicClass** teavm_vc_classTable;

static int32_t teavm_vc_computeHash(const char16_t* restrict str) {
     int32_t hash = 0;
     while (true) {
         char16_t c = *str++;
         if (c == 0) {
             break;
         }
         hash = 31 * hash + c;
     }

     hash = (int32_t) (hash << 16 | ((uint32_t) hash >> 16));
     hash ^= INT32_C(0xAAAAAAAA);
     return hash;
}

static bool teavm_vc_streq(const char16_t* restrict a, const char16_t* restrict b) {
    while (true) {
        if (*a != *b) {
            return false;
        }
        if (*a == 0) {
            return true;
        }
        ++a;
        ++b;
    }
}

static size_t teavm_vc_strlen(const char16_t* restrict str) {
    size_t size = 0;
    while (*str++ != 0) {
        ++size;
    }
    return size;
}

static int32_t teavm_vc_insertMethod(const char16_t* restrict name, int32_t hash, int32_t id) {
    uint32_t index = (uint32_t) hash % teavm_vc_hashCapacity;

    TeaVM_MethodHashEntry* entry;
    while (true) {
        entry = &teavm_vc_hashData[index];
        if (entry->name == NULL) {
            break;
        }
        index = (index + 1) % teavm_vc_hashCapacity;
    }

    size_t bytesInName = sizeof(char16_t) * (teavm_vc_strlen(name) + 1);
    entry->name = malloc(bytesInName);
    memcpy((void*) entry->name, name, bytesInName);
    entry->hash = hash;
    entry->id = id;
    teavm_vc_hashSize++;
    return entry->id;
}

static void teavm_vc_rehashMethods() {
    int32_t oldCapacity = teavm_vc_hashCapacity;
    teavm_vc_hashCapacity = teavm_vc_hashCapacity * 2 + 1;
    teavm_vc_hashThreshold = teavm_vc_hashCapacity / 2;
    TeaVM_MethodHashEntry* oldData = teavm_vc_hashData;
    size_t bytesToAllocate = teavm_vc_hashCapacity * sizeof(TeaVM_MethodHashEntry);
    teavm_vc_hashData = malloc(bytesToAllocate);
    memset(teavm_vc_hashData, 0, bytesToAllocate);

    for (int32_t i = 0; i < oldCapacity; ++i) {
        TeaVM_MethodHashEntry* entry = &oldData[i];
        if (entry->name != NULL) {
            teavm_vc_insertMethod(entry->name, entry->hash, entry->id);
        }
    }

    free(oldData);
}

int32_t teavm_vc_getMethodId(const char16_t* restrict name) {
    if (teavm_vc_hashData == NULL) {
        teavm_vc_hashCapacity = 257;
        teavm_vc_hashSize = 0;
        teavm_vc_hashThreshold = teavm_vc_hashCapacity / 2;
        size_t bytesToAllocate = teavm_vc_hashCapacity * sizeof(TeaVM_MethodHashEntry);
        teavm_vc_hashData = malloc(bytesToAllocate);
        memset(teavm_vc_hashData, 0, bytesToAllocate);
    }

    int32_t hash = teavm_vc_computeHash(name);
    uint32_t index = (uint32_t) hash % teavm_vc_hashCapacity;
    TeaVM_MethodHashEntry* entry;

    while (true) {
        entry = &teavm_vc_hashData[index];
        if (entry->name == NULL) {
            break;
        }
        if (entry->hash == hash && teavm_vc_streq(name, entry->name)) {
            return entry->id;
        }
        index = (index + 1) % teavm_vc_hashCapacity;
    }

    if (teavm_vc_hashSize >= teavm_vc_hashThreshold) {
        teavm_vc_rehashMethods();
    }

    int32_t id = (int32_t) ((teavm_vc_hashSize << 16) | ((uint32_t) teavm_vc_hashSize >> 16)) ^ INT32_C(0xAAAAAAAA);
    return teavm_vc_insertMethod(name, hash, id);
}

static void teavm_vc_insertMethodIntoClass(TeaVM_DynamicClass* restrict cls, int32_t id, void* method, bool permanent) {
    uint32_t index = (uint32_t) id % cls->capacity;
    TeaVM_DynamicClassEntry* entry;

    while (true) {
        entry = &cls->data[index];
        if (entry->method == NULL) {
            break;
        }
        entry->hasNext = 1;
        index = (index + 1) % cls->capacity;
    }

    entry->method = method;
    entry->id = id;
    entry->permanent = permanent;
    cls->size++;
}

static void teavm_vc_rehashClass(TeaVM_DynamicClass* restrict cls) {
    int32_t oldCapacity = cls->capacity;
    cls->capacity = cls->capacity * 2 + 1;
    cls->threshold = cls->capacity / 2;
    TeaVM_DynamicClassEntry* oldData = cls->data;
    size_t bytesToAllocate = sizeof(TeaVM_DynamicClassEntry) * cls->capacity;
    cls->data = malloc(bytesToAllocate);
    memset(cls->data, 0, bytesToAllocate);

    for (int32_t i = 0; i < oldCapacity; ++i) {
        TeaVM_DynamicClassEntry* entry = &oldData[i];
        if (entry->method != NULL) {
            teavm_vc_insertMethodIntoClass(cls, entry->id, entry->method, entry->permanent);
        }
    }

    free(oldData);
}

static void teavm_vc_registerMethodImpl(TeaVM_DynamicClass* restrict cls, int32_t id, void* method, bool permanent) {
    if (cls->data == NULL) {
        cls->capacity = 17;
        cls->threshold = cls->capacity / 2;
        cls->size = 0;
        size_t bytesToAllocate = sizeof(TeaVM_DynamicClassEntry) * cls->capacity;
        cls->data = malloc(bytesToAllocate);
        memset(cls->data, 0, bytesToAllocate);
    }

    uint32_t index = (uint32_t) id % cls->capacity;
    TeaVM_DynamicClassEntry* entry;

    while (true) {
        entry = &cls->data[index];
        if (entry->method == NULL) {
            break;
        }
        if (entry->id == id) {
            if (!entry->permanent) {
                entry->method = method;
                entry->permanent = permanent;
            }
            return;
        }
        index = (index + 1) % cls->capacity;
    }

    if (cls->size >= cls->threshold) {
        teavm_vc_rehashClass(cls);
    }

    teavm_vc_insertMethodIntoClass(cls, id, method, permanent);
}

void teavm_vc_registerMethod(TeaVM_DynamicClass* restrict cls, int32_t id, void* method) {
    teavm_vc_registerMethodImpl(cls, id, method, true);
}

static void teavm_vc_addClass(TeaVM_DynamicClass* restrict cls) {
    if (teavm_vc_classTable == NULL) {
        teavm_vc_classTableCapacity = 256;
        teavm_vc_classTableSize = 0;
        teavm_vc_classTable = malloc(sizeof(TeaVM_DynamicClass*) * teavm_vc_classTableCapacity);
    }
    if (teavm_vc_classTableSize == teavm_vc_classTableCapacity) {
        teavm_vc_classTableCapacity *= 2;
        teavm_vc_classTable = realloc(teavm_vc_classTable, sizeof(TeaVM_DynamicClass*) * teavm_vc_classTableCapacity);
    }
    teavm_vc_classTable[teavm_vc_classTableSize++] = cls;
}

void teavm_vc_copyMethods(TeaVM_DynamicClass* restrict from, TeaVM_DynamicClass* restrict to) {
    if (to->copy == NULL) {
        teavm_vc_addClass(to);
        int32_t initialCapacity = 4;
        to->copy = malloc(sizeof(TeaVM_DynamicClassCopy) + sizeof(TeaVM_DynamicClass*) * initialCapacity);
        to->copy->capacity = initialCapacity;
        to->copy->size = 0;
    }
    if (to->copy->size == to->copy->capacity) {
        to->copy->capacity *= 2;
        to->copy = realloc(to->copy, sizeof(TeaVM_DynamicClassCopy) + sizeof(TeaVM_DynamicClass*) * to->copy->capacity);
    }
    to->copy->data[to->copy->size++] = from;
}

void* teavm_vc_lookupMethod(const TeaVM_DynamicClass* restrict cls, int32_t id) {
    if (cls->data == NULL) {
        return NULL;
    }

    uint32_t index = (uint32_t) id % cls->capacity;
    TeaVM_DynamicClassEntry* entry;

    while (true) {
        entry = &cls->data[index];
        if (entry->method == NULL) {
            return NULL;
        }
        if (entry->id == id) {
            return entry->method;
        }
        if (!entry->hasNext) {
            return NULL;
        }

        index = (index + 1) % cls->capacity;
    }
}

static void teavm_vc_cleanupMethodHashtable() {
    if (teavm_vc_hashData == NULL) {
        return;
    }

    for (int32_t i = 0; i < teavm_vc_hashCapacity; ++i) {
        const char16_t* name = teavm_vc_hashData[i].name;
        if (name != NULL) {
            free((void*) name);
        }
    }

    free(teavm_vc_hashData);
}

static void teavm_vc_performCopyOnSingleClass(TeaVM_DynamicClass* restrict cls) {
    if (cls->copy == NULL) {
        return;
    }

    TeaVM_DynamicClassCopy* restrict copy = cls->copy;
    for (int32_t i = 0; i < copy->size; ++i) {
        TeaVM_DynamicClass* restrict from = copy->data[i];
        teavm_vc_performCopyOnSingleClass(from);
        if (from->data != NULL) {
            for (int32_t j = 0; j < from->capacity; ++j) {
                TeaVM_DynamicClassEntry* entry = &from->data[j];
                if (entry->method != NULL) {
                    teavm_vc_registerMethodImpl(cls, entry->id, entry->method, false);
                }
            }
        }
    }

    free(cls->copy);
    cls->copy = NULL;
}

static void teavm_vc_copyClassTables() {
    if (teavm_vc_classTable == NULL) {
        return;
    }

    for (int32_t i = 0; i < teavm_vc_classTableSize; ++i) {
        teavm_vc_performCopyOnSingleClass(teavm_vc_classTable[i]);
    }

    free(teavm_vc_classTable);
}

void teavm_vc_done() {
    teavm_vc_cleanupMethodHashtable();
    teavm_vc_copyClassTables();
}
