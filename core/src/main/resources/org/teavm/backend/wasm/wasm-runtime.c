#include <inttypes.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <math.h>
#include <wchar.h>
#include <wctype.h>
#include <time.h>
#include <uchar.h>

static int8_t *wasm_heap;
static int32_t wasm_heap_size;

float teavm_teavm_getNaN() {
    return NAN;
}

#define teavm_isnan isnan
#define teavm_isinf isinf
#define teavm_isfinite isfinite
#define teavmMath_sin sin
#define teavmMath_cos cos
#define teavmMath_sqrt sqrt
#define teavmMath_ceil ceil
#define teavmMath_floor floor

double teavm_currentTimeMillis() {
    struct timespec time;
    clock_gettime(CLOCK_REALTIME, &time);

    return time.tv_sec * 1000 + (int64_t) round(time.tv_nsec / 1000000);
}

double teavm_nanoTime() {
    struct timespec time;
    clock_gettime(CLOCK_REALTIME, &time);

    return time.tv_sec * 1000000000 + (int64_t) round(time.tv_nsec);
}

static union { float f; int32_t i; } reinterpret_union_32;
static union { double f; int64_t i; } reinterpret_union_64;

int64_t reinterpret_float64(double v) {
    reinterpret_union_64.f = v;
    return reinterpret_union_64.i;
}

double reinterpret_int64(int64_t v) {
    reinterpret_union_64.i = v;
    return reinterpret_union_64.f;
}

int32_t reinterpret_float32(double v) {
    reinterpret_union_32.f = v;
    return reinterpret_union_32.i;
}

float reinterpret_int32(int32_t v) {
    reinterpret_union_32.i = v;
    return reinterpret_union_32.f;
}

void teavm_logOutOfMemory() {
    abort();
}

void teavm_logString(int32_t string) {
    uint32_t arrayPtr = *(uint32_t*) (wasm_heap + string + 8);
    uint32_t length = *(uint32_t*) (wasm_heap + arrayPtr + 8);
    for (int32_t i = 0; i < length; ++i) {
        char16_t c = *(char16_t*) (wasm_heap + i * 2 + arrayPtr + 12);
        putwchar(c);
    }
}

void teavm_logInt(int32_t v) {
    wprintf(L"%" PRId32, v);
}