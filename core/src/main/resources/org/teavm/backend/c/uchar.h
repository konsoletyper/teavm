#pragma once

#include "wchar.h"

#if TEAVM_PSP

#include <stdint.h>
typedef uint16_t char16_t;
typedef int char32_t;

#else

#include <uchar.h>

#endif
