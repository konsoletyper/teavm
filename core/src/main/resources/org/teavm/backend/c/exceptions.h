#pragma once
#include "definitions.h"
#include "core.h"
#include <stdint.h>
#include <stddef.h>

#if TEAVM_USE_SETJMP
    #include <setjmp.h>
#endif

#if TEAVM_USE_SETJMP
    #define TEAVM_JUMP_SUPPORTED 1

    #define TEAVM_RESTORE_JUMP_BUFFER \
        teavm_shadowStack.header.jmpTarget = teavm_shadowStack.header.jmpTarget->previous

    #define TEAVM_TRY \
        do { \
            TeaVM_LongjmpDesc teavm_longJmpdesc; \
            teavm_longJmpdesc.previous = teavm_shadowStack.header.jmpTarget; \
            teavm_shadowStack.header.jmpTarget = &teavm_longJmpdesc; \
            int teavm_exceptionHandler = setjmp(teavm_longJmpdesc.buffer); \
            if (teavm_exceptionHandler == 0) {
    #define TEAVM_CATCH \
                TEAVM_RESTORE_JUMP_BUFFER; \
            } else { \
                TEAVM_RESTORE_JUMP_BUFFER; \
                switch (teavm_exceptionHandler) {
    #define TEAVM_END_TRY \
                    default: \
                        longjmp(teavm_shadowStack.header.jmpTarget->buffer, teavm_exceptionHandler); \
                        break; \
                } \
            } \
        } while (0);

    #define TEAVM_JUMP_TO_FRAME(frame, id) \
        teavm_stackTop = (TeaVM_StackFrame*) (frame); \
        longjmp(teavm_stackTop->jmpTarget->buffer, id)


    #if TEAVM_UNIX
        #define TEAVM_UNREACHABLE __builtin_unreachable();
    #endif
    #if TEAVM_WINDOWS
        #define TEAVM_UNREACHABLE __assume(0);
    #endif
    #ifndef TEAVM_UNREACHABLE
        #define TEAVM_UNREACHABLE return;
    #endif

    inline static void* teavm_nullCheck(void* o) {
        if (o == NULL) {
            teavm_throwNullPointerException();
            TEAVM_UNREACHABLE
        }
        return o;
    }

    inline static int32_t teavm_checkBounds(int32_t index, void* array) {
        if (index < 0 || index >= TEAVM_ARRAY_LENGTH(array)) {
            teavm_throwArrayIndexOutOfBoundsException();
            TEAVM_UNREACHABLE
        }
        return index;
    }

    inline static int32_t teavm_checkLowerBound(int32_t index) {
        if (index < 0) {
            teavm_throwArrayIndexOutOfBoundsException();
            TEAVM_UNREACHABLE
        }
        return index;
    }

    inline static int32_t teavm_checkUpperBound(int32_t index, void* array) {
        if (index >= TEAVM_ARRAY_LENGTH(array)) {
            teavm_throwArrayIndexOutOfBoundsException();
            TEAVM_UNREACHABLE
        }
        return index;
    }

#else
    #define TEAVM_JUMP_SUPPORTED 0
    #define TEAVM_JUMP_TO_FRAME(frame, id)
#endif

extern void* teavm_catchException();
