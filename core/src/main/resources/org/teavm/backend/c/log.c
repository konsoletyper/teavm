#include "log.h"
#include "definitions.h"
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <wchar.h>

#if TEAVM_WINDOWS
    #include <Windows.h>
#endif

static char16_t* teavm_utf16ToUtf32(char16_t* source, char32_t* target) {
    char16_t c = *source;
    if ((c & 0xFC00) == 0xD800) {
        char16_t n = *(source + 1);
        if ((n & 0xFC00) == 0xDC00) {
            *target = (((c & ~0xFC00) << 10) | (n & ~0xFC00)) + 0x10000;
            return source + 2;
        }
    }
    *target = c;
    return source + 1;
}

void teavm_printString(char16_t* s) {
    #if !TEAVM_WINDOWS_LOG
        #if TEAVM_WINDOWS
            fprintf(stderr, "%ls", s);
        #else
            int32_t cap = 128;
            wchar_t* buf = malloc(sizeof(wchar_t) * cap);
            wchar_t* out = buf;
            int32_t sz = 0;
            while (*s != '\0') {
                s = teavm_utf16ToUtf32(s, out++);
                if (++sz == cap) {
                    cap *= 2;
                    buf = realloc(buf, sizeof(wchar_t) * cap);
                    out = buf + sz;
                }
            }
            *out = '\0';
            fprintf(stderr, "%ls", buf);
            free(buf);
        #endif
    #else
        OutputDebugStringW(s);
    #endif
}

void teavm_printInt(int32_t i) {
    #if !TEAVM_WINDOWS_LOG
        fprintf(stderr, "%" PRId32, i);
    #else
        wchar_t str[10];
        swprintf(str, 10, L"%d", i);
        OutputDebugStringW(str);
    #endif
}


#if !TEAVM_WINDOWS_LOG
    void teavm_logchar(int32_t c) {
        putwchar(c);
    }
#else
    void teavm_logchar(int32_t c) {
        char16_t buffer[2] = { (char16_t) c, 0 };
        OutputDebugStringW(buffer);
    }
#endif