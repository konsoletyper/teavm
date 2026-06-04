#pragma once
#include <stdint.h>

typedef void* TeaVM_FiberAction(void* actionData, void* data);

#define TEAVM_FIBER_SWITCH_FINISHED  0
#define TEAVM_FIBER_SWITCH_SUSPENDED 0

typedef void* TeaVM_Fiber;

extern TeaVM_Fiber teavm_fiber_current();
extern void teavm_fiber_switch(TeaVM_Fiber fiber);
extern TeaVM_Fiber teavm_fiber_create(void (*action)());
extern void teavm_fiber_destroy(TeaVM_Fiber fiber);

extern void teavm_fiber_rewindIter();
extern TeaVM_Fiber teavm_fiber_currentIter();
extern void teavm_fiber_nextIter();
extern void* teavm_fiber_suspendedStack(TeaVM_Fiber fiber);