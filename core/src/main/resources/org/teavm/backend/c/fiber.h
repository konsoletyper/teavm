#pragma once
#include <stdint.h>
#include "time.h"

extern void teavm_initFiber();
extern void teavm_waitFor(int64_t timeout);
extern void teavm_interrupt();