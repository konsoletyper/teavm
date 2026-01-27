#pragma once
#include <stdint.h>
#include "core.h"

extern int32_t teavm_isSupertypeOfArray(TeaVM_Class* superclass, TeaVM_Class* subclass);
extern TeaVM_Class* teavm_createArrayClass(TeaVM_Class* itemClass);
extern TeaVM_Class* teavm_getArrayClass(TeaVM_Class* itemClass);
extern int teavm_arrayClassCount();
extern TeaVM_Class* teavm_arrayClass(int32_t index);