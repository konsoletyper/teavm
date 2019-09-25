#pragma once
#include <stdint.h>
#include "string.h"

#define TEAVM_HASHTABLE_ENTRIES 512

typedef struct TeaVM_HashtableEntry {
    TeaVM_String* data;
    int32_t hash;
    struct TeaVM_HashtableEntry* next;
} TeaVM_HashtableEntry;

typedef struct TeaVM_HashtableEntrySet {
    TeaVM_HashtableEntry data[TEAVM_HASHTABLE_ENTRIES];
    int32_t size;
    struct TeaVM_HashtableEntrySet* next;
} TeaVM_HashtableEntrySet;

extern TeaVM_HashtableEntrySet* teavm_stringHashtableData;

extern TeaVM_String* teavm_registerString(TeaVM_String*);