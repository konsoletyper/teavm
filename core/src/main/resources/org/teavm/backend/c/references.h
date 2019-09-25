#pragma once
#include <stdint.h>
#include "core.h"

struct TeaVM_ReferenceQueue;

typedef struct TeaVM_Reference {
    TeaVM_Object parent;
    struct TeaVM_ReferenceQueue* queue;
    TeaVM_Object* object;
    struct TeaVM_Reference* next;
} TeaVM_Reference;

typedef struct TeaVM_ReferenceQueue {
    TeaVM_Object parent;
    TeaVM_Reference* first;
    TeaVM_Reference* last;
} TeaVM_ReferenceQueue;

extern int32_t teavm_reference_enqueue(TeaVM_Reference*);

extern int32_t teavm_reference_isEnqueued(TeaVM_Reference*);

extern void teavm_reference_clear(TeaVM_Reference*);

extern TeaVM_Object* teavm_reference_get(TeaVM_Reference*);

extern TeaVM_Reference* teavm_reference_poll(TeaVM_ReferenceQueue*);

extern void teavm_reference_init(TeaVM_Reference*, TeaVM_Object*, TeaVM_ReferenceQueue*);