#include "arrayclass.h"
#include "arrayclass_gen.h"
#include <string.h>

static TEAVM_OBJECT_CLASS teavm_dynamicClassPool[TEAVM_DYNAMIC_CLASS_POOL_CAPACITY];
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
    TEAVM_OBJECT_CLASS* ptr = &teavm_dynamicClassPool[teavm_dynamicClassPoolSize++];
    TeaVM_Class* classPtr = (TeaVM_Class*) ptr;
    classPtr->parent.header = TEAVM_PACK_CLASS(teavm_classClass) | (int32_t) INT32_C(0x80000000);
    classPtr->size = sizeof(void*);
    classPtr->itemType = itemType;
    classPtr->superclass = &TEAVM_OBJECT_CLASS_PTR.parent;
    classPtr->isSupertypeOf = &teavm_isSupertypeOfArray;
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
    return &teavm_dynamicClassPool[index].parent;
}