#pragma once

#if TEAVM_PSP

#include <wchar.h>

typedef uint16_t char16_t;
typedef int char32_t;

static inline size_t c16rtomb(char * s, char16_t c16, mbstate_t * ps) {
    if (s) *s = (char) c16; // Rough conversion
    return 1;
}

static inline size_t mbrtoc16(char16_t * pc16, const char * s, size_t n, mbstate_t * ps) {
    if (pc16 && n > 0) *pc16 = (char16_t) *s; // Rough conversion
    return 1;
}

#else

#include <uchar.h>

#endif
