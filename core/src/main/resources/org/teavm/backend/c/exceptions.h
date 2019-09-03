#pragma once
#include "definitions.h"
#include <stdint.h>

#if TEAVM_USE_SETJMP
    #include <setjmp.h>
#endif

extern void* teavm_throwClassCastException();
extern void teavm_throwNullPointerException();

#if TEAVM_USE_SETJMP
    #define TEAVM_JUMP_SUPPORTED 1
    #define TEAVM_TRY \
        do { \
            jmp_buf teavm_tryBuffer; \
            jmp_buf* teavm_oldTryBuffer = teavm_shadowStack.header.jmpTarget; \
            teavm_shadowStack.header.jmpTarget = &teavm_tryBuffer; \
            int teavm_exceptionHandler = setjmp(teavm_tryBuffer); \
            switch (teavm_exceptionHandler) { \
                case 0: {
    #define TEAVM_CATCH \
                    break; \
                } \
                default: { \
                    longjmp(*teavm_oldTryBuffer, teavm_exceptionHandler); \
                    break; \
                }
    #define TEAVM_END_TRY \
            } \
            teavm_shadowStack.header.jmpTarget = teavm_oldTryBuffer; \
        } while (0);

    #define TEAVM_JUMP_TO_FRAME(frame, id) \
        teavm_stackTop = (TeaVM_StackFrame*) (frame); \
        longjmp(*teavm_stackTop->jmpTarget, id)
    inline static void* teavm_nullCheck(void* o) {
        if (o == NULL) {
            teavm_throwNullPointerException();
            #if TEAVM_UNIX
                __builtin_unreachable();
            #endif
            #if TEAVM_WINDOWS
                __assume(0);
            #endif
        }
        return o;
    }

    #if TEAVM_UNIX
        #define TEAVM_UNREACHABLE __builtin_unreachable();
    #endif
    #if TEAVM_WINDOWS
        #define TEAVM_UNREACHABLE __assume(0);
    #endif
    #ifndef TEAVM_UNREACHABLE
        #define TEAVM_UNREACHABLE return;
    #endif

#else
    #define TEAVM_JUMP_SUPPORTED 0
    #define TEAVM_JUMP_TO_FRAME(frame, id)
#endif

extern void* teavm_catchException();
