#pragma once
#include <stdint.h>
#include <uchar.h>
#include <wchar.h>

extern void teavm_printString(char16_t*);
extern void teavm_printWString(wchar_t*);
extern void teavm_printInt(int32_t);
extern void teavm_logchar(int32_t);
extern void teavm_logCodePoint(int32_t);