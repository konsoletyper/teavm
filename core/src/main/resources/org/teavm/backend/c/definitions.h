#pragma once
#include "config.h"

#define TEAVM_WINDOWS 0
#define TEAVM_WINDOWS_UWP 0
#define TEAVM_UNIX 0

#ifdef _MSC_VER
    #define alignas(n) __declspec(align(n))
    #define restrict __restrict
    #pragma comment (lib,"uuid.lib")
    #pragma warning(disable:4116)
    #pragma warning(disable:4102)

    #ifdef WINAPI_FAMILY
        #if WINAPI_FAMILY == WINAPI_FAMILY_APP || WINAPI_FAMILY == 2 || WINAPI_FAMILY == 3 || WINAPI_FAMILY == 5
            #undef TEAVM_WINDOWS_UWP
            #define TEAVM_WINDOWS_UWP 1
        #endif
    #endif

    #undef TEAVM_WINDOWS
    #define TEAVM_WINDOWS 1
#endif

#ifdef __GNUC__
    #undef TEAVM_UNIX
    #define TEAVM_UNIX 1
    #include <stdalign.h>
#endif

#ifndef TEAVM_USE_SETJMP
    #define TEAVM_USE_SETJMP 1
#endif

#ifndef TEAVM_MEMORY_TRACE
    #define TEAVM_MEMORY_TRACE 0
#endif

#if TEAVM_MEMORY_TRACE
    #ifndef TEAVM_HEAP_DUMP
        #define TEAVM_HEAP_DUMP 1
    #endif
#endif

#ifndef TEAVM_HEAP_DUMP
    #define TEAVM_HEAP_DUMP 0
#endif

#ifndef TEAVM_INCREMENTAL
    #define TEAVM_INCREMENTAL 0
#endif

#ifndef TEAVM_WINDOWS_LOG
    #define TEAVM_WINDOWS_LOG 0
#endif

#ifndef TEAVM_CUSTOM_LOG
    #define TEAVM_CUSTOM_LOG 0
#endif

#ifndef TEAVM_GC_LOG
    #define TEAVM_GC_LOG 0
#endif

#ifndef TEAVM_GC_STATS
    #define TEAVM_GC_STATS 0
#endif

#ifndef TEAVM_OBFUSCATED
    #define TEAVM_OBFUSCATED 0
#endif