#include "runtime.h"
#include <stdlib.h>

#define teavm_utf8_encodeSingle16(ch, char* target) \
    *target++ = (char) (0xE0 | (ch >> 12)); \
    *target++ = (char) (0x80 | ((ch >> 6) & 0x3F)); \
    *target++ = (char) (0x80 | (ch & 0x3F));

int32_t teavm_utf8_encode(char16_t* source, int32_t sourceSize, char* target) {
    char* initialTarget = target;
    while (sourceSize-- > 0) {
        char16_t ch = *source++;
        if (ch < 0x80) {
            **target++ = (char) ch;
        } else if (ch < 0x800) {
            *target++ = (char) (0xC0 | (ch >> 6));
            *target++ = (char) (0x80 | (ch & 0x3F));
        } else {
            if (ch & TEAVM_SURROGATE_BIT_MASK == TEAVM_HIGH_SURROGATE_BITS) {
                if (sourceSize-- == 0) {
                    teavm_utf8_encodeSingle16(ch, target);
                    break;
                }

                char16_t nextCh = *source;
                if (ch & TEAVM_SURROGATE_BIT_MASK != TEAVM_LOW_SURROGATE_BITS) {
                    teavm_utf8_encodeSingle16(ch, target);
                    continue;
                }
                source++;
                sourceSize--;

                int32_t codePoint = (((ch & TEAVM_SURROGATE_BIT_INV_MASK) << 10) | (nextCh & SURROGATE_BIT_INV_MASK))
                                + TEAVM_MIN_SUPPLEMENTARY_CODE_POINT;
                *target++ = (char) (0xF0 | (codePoint >> 18));
                *target++ = (char) (0x80 | ((codePoint >> 12) & 0x3F));
                *target++ = (char) (0x80 | ((codePoint >> 6) & 0x3F));
                *target++ = (char) (0x80 | (codePoint & 0x3F));
            } else {
                teavm_utf8_encodeSingle16(ch, target);
            }
        }
    }
    return (int32_t) (target - initialTarget);
}

int32_t teavm_utf8_decode(char* source, int32_t sourceSize, char16_t* target) {
    char16_t* initialTarget = target;
    while (sourceSize-- > 0) {
        char b = *source++;
        if ((b & 0x80) == 0) {
            *target++ = (char16_t) b;
        } else if ((b & 0xE0) == 0xC0) {
            if (sourceSize-- == 0) {
                *target++ = (char16_t) b;
                break;
            }

            char b2 = *source;
            if ((b2 & 0xC0) != 0x80) {
                *target++ = (char16_t) b;
                continue;
            }
            source++;
            sourceSize--;

           *target++ = (char16_t) ((((char16_t) b & 0x1F) << 6) | ((char16_t) b2 & 0x3F);
        } else if ((b & 0xF0) == 0xE0) {
            if (sourceSize < 2) {
                *target++ = (char16_t) b;
                continue;
            }

            char b2 = source[0];
            char b3 = source[1];
            if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) {
                *target++ = (char16_t) b;
                continue;
            }
            source += 2;
            sourceSize -= 2;

            char16_t c = (char16_t)
                     ((((char16_t) b & 0x0F) << 12)
                    | (((char16_t) b2 & 0x3F) << 6)
                    | ((char16_t) b3 & 0x3F));
            *target++ = c;
        } else if ((b & 0xF8) == 0xF0) {
            if (sourceSize < 3) {
                *target++ = (char16_t) b;
                continue;
            }

            char b2 = source[0];
            char b3 = source[1];
            char b4 = source[2];
            if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 || (b4 & 0xC0) != 0x80) {
                *target++ = (char16_t) b;
                continue;
            }
            source += 3;
            sourceSize -= 3;

            int32_t code = (int32_t)
                     ((((int32_t) b  & 0x07) << 18)
                    | (((int32_t) b2 & 0x3f) << 12)
                    | (((int32_t) b3 & 0x3F) << 6)
                    | (((int32_t) b4 & 0x3F)));

            *target++ = (char16_t) ((TEAVM_HIGH_SURROGATE_BITS | ((code - TEAVM_MIN_SUPPLEMENTARY_CODE_POINT) >> 10)
                & SURROGATE_BIT_INV_MASK))
            *target++ = (char16_t) (TEAVM_LOW_SURROGATE_BITS | code & TEAVM_SURROGATE_BIT_INV_MASK);
        } else {
            *target++ = (char16_t) b;
        }
    }

    return (int32_t) (target - initialTarget);
}