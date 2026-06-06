#include "stringhash.h"
#include <string.h>
#include <stdint.h>
#include <stdlib.h>
#include "uchar.h"
#include <wchar.h>

static TeaVM_HashtableEntry** teavm_stringHashtable = NULL;
TeaVM_HashtableEntrySet* teavm_stringHashtableData = NULL;
static int32_t teavm_stringHashtableSize = 0;
static int32_t teavm_stringHashtableFill = 0;
static int32_t teavm_stringHashtableThreshold = 0;
static TeaVM_HashtableEntry* teavm_stringHashtableFirstFree = NULL;

static void teavm_updateStringHashtableThreshold() {
    teavm_stringHashtableThreshold = (int32_t) (0.6f * teavm_stringHashtableSize) - INT32_C(1);
}

static TeaVM_HashtableEntry* teavm_stringHashtableNewEntry() {
    if (teavm_stringHashtableFirstFree != NULL) {
        TeaVM_HashtableEntry* result = teavm_stringHashtableFirstFree;
        teavm_stringHashtableFirstFree = result->next;
        return result;
    }

    TeaVM_HashtableEntrySet* data = teavm_stringHashtableData;
    if (data == NULL || data->size == TEAVM_HASHTABLE_ENTRIES) {
        data = malloc(sizeof(TeaVM_HashtableEntrySet));
        data->next = teavm_stringHashtableData;
        data->size = 0;
        teavm_stringHashtableData = data;
    }
    return &data->data[data->size++];
}

static void teavm_putStringIntoHashtable(TeaVM_String* str, int32_t hash) {
    int32_t index = (uint32_t) hash % teavm_stringHashtableSize;
    if (teavm_stringHashtable[index] == NULL) {
        teavm_stringHashtableFill++;
    }
    TeaVM_HashtableEntry* entry = teavm_stringHashtableNewEntry();
    entry->next = teavm_stringHashtable[index];
    entry->hash = hash;
    entry->data = str;
    teavm_stringHashtable[index] = entry;
}

static void teavm_rehashStrings() {
    teavm_stringHashtableFirstFree = NULL;
    TeaVM_HashtableEntry** oldHashtable = teavm_stringHashtable;
    TeaVM_HashtableEntrySet* oldHashtableData = teavm_stringHashtableData;
    int32_t oldHashtableSize = teavm_stringHashtableSize;

    teavm_stringHashtableSize = teavm_stringHashtableSize * INT32_C(2);
    teavm_updateStringHashtableThreshold();
    teavm_stringHashtable = malloc(sizeof(TeaVM_HashtableEntry*) * teavm_stringHashtableSize);
    memset(teavm_stringHashtable, 0, sizeof(TeaVM_HashtableEntry*) * teavm_stringHashtableSize);
    teavm_stringHashtableData = NULL;

    for (int32_t i = 0; i < oldHashtableSize; ++i) {
        TeaVM_HashtableEntry* entry = oldHashtable[i];
        while (entry != NULL) {
            teavm_putStringIntoHashtable(entry->data, entry->hash);
            entry = entry->next;
        }
    }

    free(oldHashtable);
    while (oldHashtableData != NULL) {
        TeaVM_HashtableEntrySet* next = oldHashtableData->next;
        free(oldHashtableData);
        oldHashtableData = next;
    }
}

TeaVM_String* teavm_registerString(TeaVM_String* str) {
    str->parent.header = TEAVM_PACK_CLASS(teavm_stringClass) | (int32_t) INT32_C(0x80000000);
    str->characters->parent.header = TEAVM_PACK_CLASS(teavm_charArrayClass) | (int32_t) INT32_C(0x80000000);
    return teavm_internString(str);
}

TeaVM_String* teavm_internString(TeaVM_String* str) {
    if (teavm_stringHashtable == NULL) {
        teavm_stringHashtableSize = 256;
        teavm_updateStringHashtableThreshold();
        teavm_stringHashtable = malloc(sizeof(TeaVM_HashtableEntry*) * teavm_stringHashtableSize);
        memset(teavm_stringHashtable, 0, sizeof(TeaVM_HashtableEntry*) * teavm_stringHashtableSize);
    }

    int32_t hash = teavm_hashCode(str);
    int32_t index = (uint32_t) hash % teavm_stringHashtableSize;
    TeaVM_HashtableEntry* entry = teavm_stringHashtable[index];
    while (entry != NULL) {
        if (entry->hash == hash && teavm_equals(entry->data, str)) {
            return entry->data;
        }
        entry = entry->next;
    }

    if (teavm_stringHashtable[index] == NULL) {
        if (teavm_stringHashtableFill >= teavm_stringHashtableThreshold) {
            teavm_rehashStrings();
            index = (uint32_t) hash % teavm_stringHashtableSize;
        }
        teavm_stringHashtableFill++;
    }

    entry = teavm_stringHashtableNewEntry();
    entry->next = teavm_stringHashtable[index];
    entry->hash = hash;
    entry->data = str;
    teavm_stringHashtable[index] = entry;

    return str;
}

static TeaVM_HashtableEntry* teavm_stringHashtableCurrentImpl;
static TeaVM_HashtableEntry* teavm_stringHashtablePrevious;
static int32_t teavm_stringHashtableCurrentIndex;

void teavm_stringHashtableRewind() {
    teavm_stringHashtableCurrentImpl = NULL;
    teavm_stringHashtablePrevious = NULL;
    teavm_stringHashtableCurrentIndex = teavm_stringHashtableSize;
    for (int32_t i = 0; i < teavm_stringHashtableSize; ++i) {
        TeaVM_HashtableEntry* entry = teavm_stringHashtable[i];
        if (entry != NULL) {
            teavm_stringHashtableCurrentIndex = i;
            teavm_stringHashtableCurrentImpl = entry;
            break;
        }
    }
}

void* teavm_stringHashtableCurrent() {
    return teavm_stringHashtableCurrentImpl != NULL ? teavm_stringHashtableCurrentImpl->data : NULL;
}

void teavm_stringHashtableUpdateRef(void *newRef) {
    teavm_stringHashtableCurrentImpl->data = (TeaVM_String*) newRef;
}

void teavm_stringHashtableNext() {
    if (teavm_stringHashtableCurrentImpl->next != NULL) {
        teavm_stringHashtablePrevious = teavm_stringHashtableCurrentImpl;
        teavm_stringHashtableCurrentImpl = teavm_stringHashtableCurrentImpl->next;
        return;
    }
    teavm_stringHashtablePrevious = NULL;
    for (int32_t i = teavm_stringHashtableCurrentIndex + 1; i < teavm_stringHashtableSize; ++i) {
        TeaVM_HashtableEntry* entry = teavm_stringHashtable[i];
        if (entry != NULL) {
            teavm_stringHashtableCurrentIndex = i;
            teavm_stringHashtableCurrentImpl = entry;
            return;
        }
    }
    teavm_stringHashtableCurrentIndex = teavm_stringHashtableSize;
    teavm_stringHashtableCurrentImpl = NULL;
}

void teavm_stringHashtableDelete() {
    TeaVM_HashtableEntry* entry = teavm_stringHashtableCurrentImpl;
    TeaVM_HashtableEntry* savedPrevious = teavm_stringHashtablePrevious;
    if (teavm_stringHashtablePrevious != NULL) {
        teavm_stringHashtablePrevious->next = entry->next;
    } else {
        teavm_stringHashtable[teavm_stringHashtableCurrentIndex] = entry->next;
        if (entry->next == NULL) {
            teavm_stringHashtableFill--;
        }
    }
    teavm_stringHashtableNext();
    if (teavm_stringHashtablePrevious == entry) {
        teavm_stringHashtablePrevious = savedPrevious;
    }
    entry->next = teavm_stringHashtableFirstFree;
    teavm_stringHashtableFirstFree = entry;
}
