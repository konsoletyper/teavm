#include <convert-params.h>
#include <stdint.h>

int incrementAllBytes(int8_t* data, int32_t count) {
    if (data == NULL) {
        return 0;
    }
    for (int32_t i = 0; i < count; ++i) {
        data[i]++;
    }
    return 1;
}

void incrementAllShorts(int16_t* data, int32_t count) {
    for (int32_t i = 0; i < count; ++i) {
        data[i]++;
    }
}

void incrementAllChars(uint16_t* data, int32_t count) {
    for (int32_t i = 0; i < count; ++i) {
        data[i]++;
    }
}

void incrementAllInts(int32_t* data, int32_t count) {
    for (int32_t i = 0; i < count; ++i) {
        data[i]++;
    }
}

void incrementAllLongs(int64_t* data, int32_t count) {
    for (int32_t i = 0; i < count; ++i) {
        data[i]++;
    }
}

void incrementAllFloats(float* data, int32_t count) {
    for (int32_t i = 0; i < count; ++i) {
        data[i]++;
    }
}

void incrementAllDoubles(double* data, int32_t count) {
    for (int32_t i = 0; i < count; ++i) {
        data[i]++;
    }
}