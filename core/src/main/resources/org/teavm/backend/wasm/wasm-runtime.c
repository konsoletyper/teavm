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
#include <unistd.h>

static int8_t *wasm_heap;
static int32_t wasm_heap_size;
static int wasm_args;
static char** wasm_argv;

#define teavmMath_sin sin
#define teavmMath_cos cos
#define teavmMath_sqrt sqrt
#define teavmMath_ceil ceil
#define teavmMath_floor floor

double teavmMath_random() {
    return rand() / (double) RAND_MAX;
}

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

int32_t wasi_snapshot_preview1_clock_time_get(int32_t clock_id, int64_t precision, int32_t result_ptr) {
    int64_t* resultAddr = (int64_t*) (wasm_heap + result_ptr);
    struct timespec time;
    clock_gettime(CLOCK_REALTIME, &time);
    *resultAddr = time.tv_sec * 1000000000 + (int64_t) round(time.tv_nsec);
    return 0;
}

int32_t wasi_snapshot_preview1_args_sizes_get(int32_t argv_size, int32_t argv_buffer_size) {
    int32_t* argvSizePtr = (int32_t*) (wasm_heap + argv_size);
    int32_t* argvBufferSizePtr = (int32_t*) (wasm_heap + argv_buffer_size);
    *argvSizePtr = (int32_t) wasm_args;

    int32_t bufferSize = 0;
    for (int i = 0; i < wasm_args; ++i) {
        bufferSize += (int32_t) strlen(wasm_argv[i]) + 1;
    }
    *argvBufferSizePtr = bufferSize;
    return 0;
}

int32_t wasi_snapshot_preview1_args_get(int32_t sizes_ptr, int32_t args_ptr) {
    int32_t* sizesPtr = (int32_t*) (wasm_heap + sizes_ptr);
    char* argsPtr = (char*) (wasm_heap + args_ptr);
    int offset = 0;
    for (int i = 0; i < wasm_args; ++i) {
        sizesPtr[i] = (int32_t) offset + args_ptr;
        int len = strlen(wasm_argv[i]) + 1;
        memcpy(argsPtr + offset, wasm_argv[i], len);
        offset += len;
    }
    return 0;
}

typedef struct {
    int32_t tag;
    union {
        struct {
            int32_t name_length;
        } dir;
    } data;
} WasiPrestat;

int32_t wasi_snapshot_preview1_fd_prestat_get(int32_t fd, int32_t prestat_ptr) {
    if (fd != 3) {
        return 8;
    }
    WasiPrestat* prestat = (WasiPrestat*) (wasm_heap + prestat_ptr);
    prestat->tag = 0;
    prestat->data.dir.name_length = 1;
    return 0;
}

int32_t wasi_snapshot_preview1_fd_prestat_dir_name(int32_t fd, int32_t path, int32_t path_length) {
    char* pathPtr = (char*) (wasm_heap + path);
    *pathPtr = '/';
    return 0;
}

typedef struct {
    int32_t buf;
    int32_t buf_len;
} WasiIOVec;

int32_t wasi_snapshot_preview1_fd_write(int32_t fd, int32_t iovs, int32_t count, int32_t result) {
    WasiIOVec* vec = (WasiIOVec*) (wasm_heap + iovs);
    int32_t written = 0;
    for (int32_t i = 0; i < count; ++i) {
        written += write((int) fd, (char*) (wasm_heap + vec->buf), vec->buf_len);
    }
    int32_t* resultPtr = (int32_t*) (wasm_heap + result);
    *resultPtr = written;
    return 0;
}

void teavm_putwcharsOut(int32_t chars, int32_t count) {
    char* chars_array = (char*) (wasm_heap + chars);
    for (int32_t i = 0; i < count; ++i) {
        putc(chars_array[i], stdout);
    }
}

void teavm_putwcharsErr(int32_t chars, int32_t count) {
    char* chars_array = (char*) (wasm_heap + chars);
    for (int32_t i = 0; i < count; ++i) {
        putc(chars_array[i], stderr);
    }
}