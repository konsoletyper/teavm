#include "time.h"
#include "definitions.h"
#include <time.h>
#include <math.h>

#if TEAVM_WINDOWS
    #include <Windows.h>
#endif

#if TEAVM_WINDOWS
    int64_t teavm_unixTimeOffset;
    int64_t teavm_perfFrequency, teavm_perfInitTime;
#endif

void teavm_initTime() {
    #if TEAVM_WINDOWS
        SYSTEMTIME unixEpochStart = {
            .wYear = 1970,
            .wMonth = 1,
            .wDayOfWeek = 3,
            .wDay = 1
        };
        FILETIME fileTimeStart;
        SystemTimeToFileTime(&unixEpochStart, &fileTimeStart);
        teavm_unixTimeOffset = fileTimeStart.dwLowDateTime | ((uint64_t) fileTimeStart.dwHighDateTime << 32);
    #endif
}

#if TEAVM_UNIX
    int64_t teavm_currentTimeMillis() {
        struct timespec time;
        clock_gettime(CLOCK_REALTIME, &time);

        return time.tv_sec * INT64_C(1000) + (int64_t) round(time.tv_nsec / 1000000);
    }
    int64_t teavm_currentTimeNano() {
        struct timespec time;
        clock_gettime(CLOCK_REALTIME, &time);

        return time.tv_sec * INT64_C(1000000000) + (int64_t) round(time.tv_nsec);
    }
#endif

#if TEAVM_WINDOWS
    int64_t teavm_currentTimeMillis() {
        SYSTEMTIME time;
        FILETIME fileTime;
        GetSystemTime(&time);
        SystemTimeToFileTime(&time, &fileTime);
        uint64_t current = fileTime.dwLowDateTime | ((uint64_t) fileTime.dwHighDateTime << 32);
        return (int64_t) ((current - teavm_unixTimeOffset) / 10000);
    }

    int64_t teavm_currentTimeNano() {
        LARGE_INTEGER perf = { .QuadPart = 0 };
        QueryPerformanceCounter(&perf);
        int64_t dt = perf.QuadPart - teavm_perfInitTime;
        return dt * INT64_C(1000000000) / teavm_perfFrequency;
    }
#endif


#if TEAVM_WINDOWS
    #undef gmtime_r
    #undef localtime_r
    #define gmtime_r(a, b) gmtime_s(b, a)
    #define localtime_r(a, b) localtime_s(b, a)
#endif

int32_t teavm_timeZoneOffset() {
    struct tm tm;
    time_t t = time(NULL);
    localtime_r(&t, &tm);
    time_t local = mktime(&tm);
    gmtime_r(&t, &tm);
    time_t utc = mktime(&tm);
    return (int32_t) (difftime(utc, local) / 60);
}
