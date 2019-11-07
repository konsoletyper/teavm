#include "references.h"
#include "core.h"

int32_t teavm_reference_enqueue(TeaVM_Reference* reference) {
    TeaVM_ReferenceQueue* queue = reference->queue;
    if (queue == NULL || reference->next != NULL) {
        return INT32_C(0);
    }

    if (queue->last == NULL) {
        queue->first = reference;
    } else {
        teavm_gc_writeBarrier(queue->last);
        queue->last->next = reference;
    }
    queue->last = reference;
    teavm_gc_writeBarrier(queue);

    return INT32_C(1);
}

int32_t teavm_reference_isEnqueued(TeaVM_Reference* reference) {
    return reference->queue != NULL && reference->next != NULL ? INT32_C(1) : INT32_C(0);
}

void teavm_reference_clear(TeaVM_Reference* reference) {
    reference->object = NULL;
}

TeaVM_Object* teavm_reference_get(TeaVM_Reference* reference) {
    return reference->object;
}

TeaVM_Reference* teavm_reference_poll(TeaVM_ReferenceQueue* queue) {
    if (queue->first == NULL) {
        return NULL;
    }

    TeaVM_Reference* reference = queue->first;
    queue->first = reference->next;
    teavm_gc_writeBarrier(queue);
    if (queue->first == NULL) {
        queue->last = NULL;
    }
    return reference;
}

void teavm_reference_init(TeaVM_Reference* reference, TeaVM_Object* object, TeaVM_ReferenceQueue* queue) {
    reference->object = object;
    reference->queue = queue;
}