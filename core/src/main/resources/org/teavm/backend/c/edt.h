#pragma once
#include <stdint.h>
#include "fiber.h"

extern void teavm_edt_init();
extern void teavm_edt_waitFor(int64_t timeout);
extern void teavm_edt_interrupt();
extern TeaVM_Fiber teavm_edt_fiber();