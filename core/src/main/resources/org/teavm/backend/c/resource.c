#include "resource.h"
#include "string.h"
#include "core.h"

TeaVM_ResourceMapEntry* teavm_lookupResource(TeaVM_ResourceMap *map, TeaVM_String* string) {
    uint32_t hashCode = teavm_hashCode(string);
    for (int32_t i = 0; i < map->size; ++i) {
        uint32_t index = (hashCode + i) % map->size;
        if (map->entries[index].key == NULL) {
            return NULL;
        }
        if (teavm_equals(*map->entries[index].key, string)) {
            return &map->entries[index];
        }
    }
    return NULL;
}

TeaVM_Array* teavm_resourceMapKeys(TeaVM_ResourceMap *map) {
    int32_t size = 0;
    for (int32_t i = 0; i < map->size; ++i) {
        if (map->entries[i].key != NULL) {
            size++;
        }
    }

    int32_t index = 0;
    void* array = teavm_allocateStringArray(size);
    void** data = TEAVM_ARRAY_DATA(array, void*);
    for (int32_t i = 0; i < map->size; ++i) {
        if (map->entries[i].key != NULL) {
            data[index++] = map->entries[i].key;
        }
    }

    return array;
}