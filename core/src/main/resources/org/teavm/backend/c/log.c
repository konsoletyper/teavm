#include "log.h"
#include "definitions.h"
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <wchar.h>

#if TEAVM_WINDOWS
    #include <Windows.h>
#endif

#if !TEAVM_CUSTOM_LOG
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

    void teavm_printWString(wchar_t* s) {
        #if !TEAVM_WINDOWS_LOG
            fprintf(stderr, "%ls", s);
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
        void teavm_logCodePoint(int32_t c) {
            putwchar(c);
        }
    #else
        void teavm_logCodePoint(int32_t c) {
            char16_t buffer[2] = { (char16_t) c, 0 };
            OutputDebugStringW(buffer);
        }
    #endif
#endif

static int32_t teavm_logCharBuffer;
static int teavm_logCharBufferRemaining;
static int teavm_logCharBufferSize;

void teavm_logchar(int32_t c) {
    if (teavm_logCharBufferRemaining > 0) {
        teavm_logCharBuffer = teavm_logCharBuffer << 6 | (c & 0x3F);
        if (--teavm_logCharBufferRemaining == 0) {
            teavm_logCodePoint(teavm_logCharBuffer);
        }
    } else if ((c & 0x80) == 0) {
        teavm_logCodePoint(c);
    } else if ((c & 0xE0) == 0xC0) {
        teavm_logCharBuffer = c & 0x1F;
        teavm_logCharBufferRemaining = 1;
    } else if ((c & 0xF0) == 0xE0) {
        teavm_logCharBuffer = c & 0x0F;
        teavm_logCharBufferRemaining = 2;
    } else if ((c & 0xF8) == 0xF0) {
        teavm_logCharBuffer = c & 0x07;
        teavm_logCharBufferRemaining = 3;
    }
}