#include "fiber.h"
#include "definitions.h"
#include "stack.h"
#include "log.h"
#include <stddef.h>
#include <setjmp.h>
#include <stdlib.h>
#include <stdbool.h>

#define TEAVM_FIBER_STATUS_NEW 0
#define TEAVM_FIBER_STATUS_SCHEDULED 1

typedef struct TeaVM_FiberImpl {
    int32_t status;
    struct TeaVM_FiberImpl* previous;
    struct TeaVM_FiberImpl* next;
    union {
        void (*newAction)();
        struct {
            char* stackSnapshot;
            int32_t stackSize;
            TeaVM_StackFrame* shadowStackTop;
            jmp_buf jmp;
        } suspended;
    } data;
} TeaVM_FiberImpl;

static TeaVM_FiberImpl* teavm_fiber_first = NULL;
static TeaVM_FiberImpl* teavm_fiber_last = NULL;
static TeaVM_FiberImpl* teavm_fiber_currentImpl = NULL;
static void* teavm_fiber_stackStart = NULL;
static TeaVM_StackFrame* teavm_fiber_initShadowStack = NULL;
static jmp_buf teavm_fiber_hostJmpBuf;
static bool teavm_fiber_stackDirDetected = false;
static bool teavm_fiber_stackDirTopToBottom;
static char* teavm_fiber_resumingStackStart;

static void teavm_fiber_detectStackDir() {
    if (teavm_fiber_stackDirDetected) {
        return;
    }
    teavm_fiber_stackDirDetected = true;
    volatile void* dummy = &teavm_fiber_currentImpl;
    teavm_fiber_stackDirTopToBottom = ((char*) dummy) - ((char*) teavm_fiber_stackStart) < 0;
}

__attribute__((noinline))
static void teavm_fiber_resume() {
    volatile void* dummy = &teavm_fiber_currentImpl;
    int32_t currentDepth = (int32_t) (((char*) dummy) - ((char*) teavm_fiber_stackStart));
    if (currentDepth < 0) {
        currentDepth = -currentDepth;
    }
    int32_t guardSize = teavm_fiber_currentImpl->data.suspended.stackSize - currentDepth;
    if (guardSize < 0) {
        guardSize = 0;
    }
    guardSize += 4096;
    volatile char *guard = alloca(guardSize);
    memset((char*)guard, 0, guardSize);

    teavm_fiber_detectStackDir();
    teavm_fiber_resumingStackStart = teavm_fiber_stackStart;
    if (teavm_fiber_stackDirTopToBottom) {
        teavm_fiber_resumingStackStart -= teavm_fiber_currentImpl->data.suspended.stackSize;
    }
    teavm_stackTop = teavm_fiber_currentImpl->data.suspended.shadowStackTop;
    memcpy(teavm_fiber_resumingStackStart, teavm_fiber_currentImpl->data.suspended.stackSnapshot,
            teavm_fiber_currentImpl->data.suspended.stackSize);
    free(teavm_fiber_currentImpl->data.suspended.stackSnapshot);
    teavm_fiber_currentImpl->data.suspended.stackSnapshot = NULL;
    longjmp(teavm_fiber_currentImpl->data.suspended.jmp, 1);
    dummy = guard;
    __builtin_unreachable();
}

__attribute__((noinline))
static bool teavm_fiber_suspend() {
    volatile void* dummy = &teavm_fiber_currentImpl;
    teavm_fiber_detectStackDir();
    teavm_fiber_currentImpl->data.suspended.shadowStackTop = teavm_stackTop;
    teavm_fiber_currentImpl->data.suspended.stackSize = teavm_fiber_stackDirTopToBottom
            ? (int32_t) (((char*) teavm_fiber_stackStart) - ((char*) &dummy))
            : (int32_t) (((char*) &dummy) - ((char*) teavm_fiber_stackStart));
    teavm_fiber_currentImpl->data.suspended.stackSnapshot = malloc(teavm_fiber_currentImpl->data.suspended.stackSize);
    memcpy(teavm_fiber_currentImpl->data.suspended.stackSnapshot,
            teavm_fiber_stackDirTopToBottom ? &dummy : teavm_fiber_stackStart,
            teavm_fiber_currentImpl->data.suspended.stackSize);
    return setjmp(teavm_fiber_currentImpl->data.suspended.jmp) == 0;
}

static void teavm_fiber_host() {
    volatile void* dummy = &teavm_fiber_currentImpl;
    teavm_fiber_stackStart = &dummy;
    teavm_fiber_initShadowStack = teavm_stackTop;
    setjmp(teavm_fiber_hostJmpBuf);
    if (teavm_fiber_currentImpl != NULL) {
        if (teavm_fiber_currentImpl->status != TEAVM_FIBER_STATUS_NEW) {
            teavm_printWString(L"[TEAVM] Assertion error: host always expects NEW fibers\n");
            abort();
        }
        void (*action)() = teavm_fiber_currentImpl->data.newAction;
        teavm_fiber_currentImpl->status = TEAVM_FIBER_STATUS_SCHEDULED;
        teavm_fiber_currentImpl->data.newAction = NULL;
        teavm_stackTop = teavm_fiber_initShadowStack;
        action();
    }
}

static void teavm_fiber_bootstrap(TeaVM_FiberImpl* fiber) {
    if (fiber->status != TEAVM_FIBER_STATUS_NEW) {
        teavm_printWString(L"[TEAVM] Assertion error: first running fiber must be in NEW state\n");
        abort();
    }
    teavm_fiber_currentImpl = fiber;
    teavm_fiber_host();
}

TeaVM_Fiber teavm_fiber_current() {
    return teavm_fiber_currentImpl;
}

TeaVM_Fiber teavm_fiber_create(void (*action)()) {
    TeaVM_FiberImpl* fiber = malloc(sizeof(TeaVM_FiberImpl));
    fiber->status = TEAVM_FIBER_STATUS_NEW;
    fiber->data.newAction = action;
    fiber->next = NULL;
    if (teavm_fiber_last != NULL) {
        teavm_fiber_last->next = fiber;
        fiber->previous = teavm_fiber_last;
    } else {
        teavm_fiber_first = fiber;
        fiber->previous = NULL;
    }
    teavm_fiber_last = fiber;
    return fiber;
}

void teavm_fiber_destroy(TeaVM_Fiber fiber) {
    TeaVM_FiberImpl* fiberImpl = (TeaVM_FiberImpl*) fiber;
    if (fiberImpl == teavm_fiber_currentImpl) {
        teavm_printWString(L"[TEAVM] Assertion error: can't destroy currently running fiber\n");
        abort();
    }
    if (fiberImpl->previous != NULL) {
        fiberImpl->previous->next = fiberImpl->next;
    } else {
        teavm_fiber_first = fiberImpl->next;
    }
    if (fiberImpl->next != NULL) {
        fiberImpl->next->previous = fiberImpl->previous;
    } else {
        teavm_fiber_last = fiberImpl->previous;
    }
    if (fiberImpl->status == TEAVM_FIBER_STATUS_SCHEDULED && fiberImpl->data.suspended.stackSnapshot != NULL) {
        free(fiberImpl->data.suspended.stackSnapshot);
    }
    free(fiberImpl);
}

void teavm_fiber_switch(TeaVM_Fiber fiber) {
    TeaVM_FiberImpl* fiberImpl = (TeaVM_FiberImpl*) fiber;
    if (teavm_fiber_currentImpl == NULL) {
        teavm_fiber_bootstrap(fiberImpl);
    } else if (fiberImpl != teavm_fiber_currentImpl) {
        if (teavm_fiber_suspend()) {
            teavm_fiber_currentImpl = fiberImpl;
            if (teavm_fiber_currentImpl->status == TEAVM_FIBER_STATUS_NEW) {
                longjmp(teavm_fiber_hostJmpBuf, 1);
            } else {
                teavm_fiber_resume();
            }
        }
    } else {
        teavm_printWString(L"[TEAVM] Assertion error: can't switch to already running fiber\n");
        abort();
    }
}

static TeaVM_FiberImpl* teavm_fiber_currentIterImpl = NULL;

void teavm_fiber_rewindIter() {
    teavm_fiber_currentIterImpl = teavm_fiber_first;
}

TeaVM_Fiber teavm_fiber_currentIter() {
    return teavm_fiber_currentIterImpl;
}

void teavm_fiber_nextIter() {
    teavm_fiber_currentIterImpl = teavm_fiber_currentIterImpl->next;
}

void* teavm_fiber_suspendedStack(TeaVM_Fiber fiber) {
    TeaVM_FiberImpl* fiberImpl = (TeaVM_FiberImpl*) fiber;
    if (fiberImpl->status != TEAVM_FIBER_STATUS_SCHEDULED) {
        return NULL;
    }
    return fiberImpl->data.suspended.shadowStackTop;
}