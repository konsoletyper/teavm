#pragma once

#if TEAVM_PSP || defined(__APPLE__)

#include <wchar.h>
#include <stdint.h>

typedef uint16_t char16_t;
typedef int char32_t;

/* Encode a single UTF-16 code unit to UTF-8. Handles the full BMP (U+0000–U+FFFF). */
static inline size_t c16rtomb(char * s, char16_t c16, mbstate_t * ps) {
    if (!s) return 1;
    uint32_t cp = (uint32_t)(uint16_t)c16;
    if (cp < 0x80) {
        s[0] = (char)cp;
        return 1;
    } else if (cp < 0x800) {
        s[0] = (char)(0xC0 | (cp >> 6));
        s[1] = (char)(0x80 | (cp & 0x3F));
        return 2;
    } else {
        s[0] = (char)(0xE0 | (cp >> 12));
        s[1] = (char)(0x80 | ((cp >> 6) & 0x3F));
        s[2] = (char)(0x80 | (cp & 0x3F));
        return 3;
    }
}

/*
 * Decode one UTF-8 sequence into a UTF-16 code unit.
 * Returns the number of input bytes consumed, (size_t)-2 if more bytes are
 * needed, or (size_t)-1 on a decoding error.
 * Code points outside the BMP are replaced with U+FFFD.
 */
static inline size_t mbrtoc16(char16_t * pc16, const char * s, size_t n, mbstate_t * ps) {
    if (!s || n == 0) return (size_t)-2;
    unsigned char c0 = (unsigned char)s[0];
    uint32_t cp;
    size_t len;
    if (c0 < 0x80) {
        cp = c0; len = 1;
    } else if ((c0 & 0xE0) == 0xC0) {
        if (n < 2) return (size_t)-2;
        cp = ((c0 & 0x1F) << 6) | ((unsigned char)s[1] & 0x3F);
        len = 2;
    } else if ((c0 & 0xF0) == 0xE0) {
        if (n < 3) return (size_t)-2;
        cp = ((c0 & 0x0F) << 12)
           | (((unsigned char)s[1] & 0x3F) << 6)
           | ((unsigned char)s[2] & 0x3F);
        len = 3;
    } else if ((c0 & 0xF8) == 0xF0) {
        if (n < 4) return (size_t)-2;
        cp = ((c0 & 0x07) << 18)
           | (((unsigned char)s[1] & 0x3F) << 12)
           | (((unsigned char)s[2] & 0x3F) << 6)
           | ((unsigned char)s[3] & 0x3F);
        len = 4;
    } else {
        return (size_t)-1;
    }
    if (pc16) *pc16 = (char16_t)(cp <= 0xFFFF ? cp : 0xFFFD);
    return len;
}

#else

#include <uchar.h>

#endif
