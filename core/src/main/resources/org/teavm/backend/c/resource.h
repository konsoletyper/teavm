#pragma once
#include "core.h"
#include "string.h"
#include <stdint.h>

typedef struct {
    int32_t size;
    void* data[1];
} TeaVM_ResourceArray;

typedef struct {
    TeaVM_String** key;
    void* value;
} TeaVM_ResourceMapEntry;

typedef struct {
    int32_t size;
    TeaVM_ResourceMapEntry entries[1];
} TeaVM_ResourceMap;


extern TeaVM_ResourceMapEntry* teavm_lookupResource(TeaVM_ResourceMap *map, TeaVM_String* string);

static inline void* teavm_lookupResourceValue(TeaVM_ResourceMap *map, TeaVM_String* string) {
    TeaVM_ResourceMapEntry *entry = teavm_lookupResource(map, string);
    return entry != NULL ? entry->value : NULL;
}

extern TeaVM_Array* teavm_resourceMapKeys(TeaVM_ResourceMap *);