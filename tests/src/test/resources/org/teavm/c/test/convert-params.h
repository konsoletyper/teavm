#include <stdint.h>

int incrementAllBytes(int8_t* data, int32_t count);
void incrementAllShorts(int16_t* data, int32_t count);
void incrementAllChars(uint16_t* data, int32_t count);
void incrementAllInts(int32_t* data, int32_t count);
void incrementAllLongs(int64_t* data, int32_t count);
void incrementAllFloats(float* data, int32_t count);
void incrementAllDoubles(double* data, int32_t count);