#include "arrayclass.h"
#include "arrayclass_gen.h"
#include "log.h"
#include <string.h>

/* Packed class pointers discard three low bits. Align every pool entry, not
 * only the pool base: a 32-bit vtable can have a size congruent to 4 mod 8. */
static struct {
    alignas(8) TEAVM_OBJECT_CLASS value;
} teavm_dynamicClassPool[TEAVM_DYNAMIC_CLASS_POOL_CAPACITY];
static int32_t teavm_dynamicClassPoolSize = INT32_C(0);

int32_t teavm_isSupertypeOfArray(TeaVM_Class* superclass, TeaVM_Class* subclass) {
    if (subclass->itemType == NULL) {
        return INT32_C(0);
    }
    return superclass->itemType->isSupertypeOf(superclass->itemType, subclass->itemType);
}

TeaVM_Class* teavm_getArrayClass(TeaVM_Class* itemType) {
    if (itemType->arrayType != NULL) {
        return itemType->arrayType;
    }
    return teavm_createArrayClass(itemType);
}

TeaVM_Class* teavm_createArrayClass(TeaVM_Class* itemType) {
    if (teavm_dynamicClassPoolSize + 1 >= TEAVM_DYNAMIC_CLASS_POOL_CAPACITY) {
        teavm_printWString(L"Metaspace size\n");
        abort();
    }
    TEAVM_OBJECT_CLASS* ptr = &teavm_dynamicClassPool[teavm_dynamicClassPoolSize++].value;
    TeaVM_Class* classPtr = (TeaVM_Class*) ptr;
    classPtr->flags = 1;
    classPtr->modifiers = 0;
    classPtr->size = sizeof(void*);
    classPtr->itemType = itemType;
    classPtr->superclass = &TEAVM_OBJECT_CLASS_PTR.parent;
    classPtr->isSupertypeOf = &teavm_isSupertypeOfArray;
    classPtr->classObject = NULL;
    classPtr->next = teavm_firstClass;
    teavm_firstClass = classPtr;
    int32_t offset = sizeof(TeaVM_Class);
    int32_t limit = sizeof(TEAVM_OBJECT_CLASS);
    memcpy((char*) ptr + offset, (char*) &TEAVM_OBJECT_CLASS_PTR + offset, limit - offset);
    itemType->arrayType = &ptr->parent;
    return classPtr;
}

int teavm_arrayClassCount() {
    return teavm_dynamicClassPoolSize;
}

extern TeaVM_Class* teavm_arrayClass(int32_t index) {
    return &teavm_dynamicClassPool[index].value.parent;
}
