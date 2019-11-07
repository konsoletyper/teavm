#include "stringhash.h"
#include <string.h>
#include <stdint.h>
#include <stdlib.h>
#include <uchar.h>
#include <wchar.h>

static TeaVM_HashtableEntry** teavm_stringHashtable = NULL;
TeaVM_HashtableEntrySet* teavm_stringHashtableData = NULL;
static int32_t teavm_stringHashtableSize = 0;
static int32_t teavm_stringHashtableFill = 0;
static int32_t teavm_stringHashtableThreshold = 0;

static void teavm_updateStringHashtableThreshold() {
    teavm_stringHashtableThreshold = (int32_t) (0.6f * teavm_stringHashtableSize) - INT32_C(1);
}

static TeaVM_HashtableEntry* teavm_stringHashtableNewEntry() {
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
