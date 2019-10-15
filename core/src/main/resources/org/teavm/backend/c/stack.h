#pragma once
#include <stdint.h>
#include <uchar.h>
#include "string.h"
#include "definitions.h"

#if TEAVM_USE_SETJMP
#include <setjmp.h>
#endif

typedef struct {
    TeaVM_String* value;
} TeaVM_StringPtr;

typedef struct TeaVM_MethodLocation {
    TeaVM_String** fileName;
    TeaVM_String** className;
    TeaVM_String** methodName;
} TeaVM_MethodLocation;

typedef struct TeaVM_CallSiteLocation {
    TeaVM_MethodLocation* method;
    int32_t lineNumber;
    struct TeaVM_CallSiteLocation* next;
} TeaVM_CallSiteLocation;

typedef struct TeaVM_ExceptionHandler {
    int32_t id;
    TeaVM_Class* exceptionClass;
    struct TeaVM_ExceptionHandler* next;
} TeaVM_ExceptionHandler;

typedef struct TeaVM_CallSite {
    TeaVM_ExceptionHandler* firstHandler;
    TeaVM_CallSiteLocation* location;
} TeaVM_CallSite;

typedef struct TeaVM_StackFrame {
    struct TeaVM_StackFrame* next;
    #if TEAVM_INCREMENTAL
        TeaVM_CallSite* callSites;
    #endif
    #if TEAVM_USE_SETJMP
        jmp_buf* jmpTarget;
    #endif
    int32_t size;
    int32_t callSiteId;
} TeaVM_StackFrame;

#if !TEAVM_INCREMENTAL
    extern TeaVM_CallSite teavm_callSites[];
    #define TEAVM_FIND_CALLSITE(id, frame) (teavm_callSites + id)
#else
    #define TEAVM_FIND_CALLSITE(id, frame) (((TeaVM_StackFrame*) (frame))->callSites + id)
#endif


#if TEAVM_INCREMENTAL

    #define TEAVM_ALLOC_STACK_DEF(sz, cs) \
        struct { TeaVM_StackFrame header; void* data[(sz)]; } teavm_shadowStack; \
        teavm_shadowStack.header.next = teavm_stackTop; \
        teavm_shadowStack.header.callSites = (cs); \
        teavm_shadowStack.header.size = (sz); \
        teavm_stackTop = &teavm_shadowStack.header

    #define TEAVM_STACK_HEADER_ADD_SIZE 1

#else

    #define TEAVM_ALLOC_STACK(sz) \
        struct { TeaVM_StackFrame header; void* data[(sz)]; } teavm_shadowStack; \
        teavm_shadowStack.header.next = teavm_stackTop; \
        teavm_shadowStack.header.size = (sz); \
        teavm_stackTop = &teavm_shadowStack.header

    #define TEAVM_STACK_HEADER_ADD_SIZE 0

#endif


#define TEAVM_RELEASE_STACK (teavm_stackTop = teavm_shadowStack.header.next)
#define TEAVM_GC_ROOT(index, ptr) teavm_shadowStack.data[index] = ptr
#define TEAVM_GC_ROOT_RELEASE(index) teavm_shadowStack.data[index] = NULL
#define TEAVM_GC_ROOTS_COUNT(ptr) (((TeaVM_StackFrame*) (ptr))->size);
#define TEAVM_GET_GC_ROOTS(ptr) &((struct { TeaVM_StackFrame header; void* data[1]; }*) (ptr))->data;
#define TEAVM_CALL_SITE(id) (teavm_shadowStack.header.callSiteId = (id))
#define TEAVM_WITH_CALL_SITE_ID(id, expr) (TEAVM_CALL_SITE(id), (expr))
#define TEAVM_EXCEPTION_HANDLER (teavm_shadowStack.header.callSiteId)
#define TEAVM_SET_EXCEPTION_HANDLER(frame, id) (((TeaVM_StackFrame*) (frame))->callSiteId = (id))
#define TEAVM_GET_NEXT_FRAME(frame) (((TeaVM_StackFrame*) (frame))->next)
#define TEAVM_GET_CALL_SITE_ID(frame) (((TeaVM_StackFrame*) (frame))->callSiteId)

extern TeaVM_StackFrame* teavm_stackTop;