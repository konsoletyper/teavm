#pragma once
#include <stdint.h>
#include "definitions.h"

#if TEAVM_WINDOWS
    extern int64_t teavm_unixTimeOffset;
    extern int64_t teavm_perfFrequency, teavm_perfInitTime;
#endif

extern void teavm_initTime();
extern int64_t teavm_currentTimeMillis();
extern int64_t teavm_currentTimeNano();
extern int32_t teavm_timeZoneOffset();