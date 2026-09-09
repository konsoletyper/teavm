#ifdef __APPLE__
    #ifndef _DARWIN_C_SOURCE
    #define _DARWIN_C_SOURCE
    #endif
#else
    #ifndef _XOPEN_SOURCE
    #define _XOPEN_SOURCE
    #endif

    #ifndef __USE_XOPEN
    #define __USE_XOPEN
    #endif

    #ifndef _GNU_SOURCE
    #define _GNU_SOURCE
    #endif
#endif

#include "date.h"
#include "definitions.h"

#if TEAVM_WINDOWS
    #include <errno.h>
    #define timegm _mkgmtime
    #define localtime_r(a, b) localtime_s(b, a)
#endif

static time_t teavm_epochStart;
static struct tm teavm_epochStartTm;
static char teavm_date_formatBuffer[512];
static char* teavm_date_defaultFormat = "%a %b %d %H:%M:%S %Z %Y";

#if TEAVM_WINDOWS
// MSVC's mktime/_mkgmtime only support years 1970 through 3000 (they fail with EINVAL
// outside that range, unlike glibc's, which has no such limit). As a fallback for years
// outside that range, do proleptic Gregorian calendar <-> days-since-epoch conversion
// ourselves. Algorithm by Howard Hinnant, see http://howardhinnant.github.io/date_algorithms.html
static int64_t teavm_date_localOffsetSeconds;

static int64_t teavm_date_daysFromCivil(int64_t y, int m, int d) {
    y -= m <= 2 ? 1 : 0;
    int64_t era = (y >= 0 ? y : y - 399) / 400;
    int64_t yoe = y - era * 400;
    int64_t doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
    int64_t doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    return era * 146097 + doe - 719468;
}

static void teavm_date_civilFromDays(int64_t z, int64_t *y, int *m, int *d) {
    z += 719468;
    int64_t era = (z >= 0 ? z : z - 146096) / 146097;
    int64_t doe = z - era * 146097;
    int64_t yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
    int64_t yyyy = yoe + era * 400;
    int64_t doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    int64_t mp = (5 * doy + 2) / 153;
    *d = (int) (doy - (153 * mp + 2) / 5 + 1);
    *m = (int) (mp < 10 ? mp + 3 : mp - 9);
    *y = yyyy + (*m <= 2 ? 1 : 0);
}

// Treats the given broken-down time as UTC and returns the corresponding number of
// milliseconds since the Unix epoch, without relying on the platform's CRT.
static int64_t teavm_date_toEpochMillis(struct tm *t) {
    int64_t days = teavm_date_daysFromCivil(1900 + (int64_t) t->tm_year, t->tm_mon + 1, t->tm_mday);
    int64_t seconds = days * INT64_C(86400) + t->tm_hour * INT64_C(3600) + t->tm_min * INT64_C(60) + t->tm_sec;
    return seconds * INT64_C(1000);
}

// Fills in broken-down UTC time fields from the given number of milliseconds since the Unix
// epoch, without relying on the platform's CRT.
static void teavm_date_fromEpochMillis(int64_t millis, struct tm *t) {
    int64_t seconds = millis >= 0 ? millis / 1000 : -((-millis + 999) / 1000);
    int64_t days = seconds / 86400;
    int64_t secondsOfDay = seconds % 86400;
    if (secondsOfDay < 0) {
        secondsOfDay += 86400;
        days -= 1;
    }
    int64_t y;
    int m;
    int d;
    teavm_date_civilFromDays(days, &y, &m, &d);
    t->tm_year = (int) (y - 1900);
    t->tm_mon = m - 1;
    t->tm_mday = d;
    t->tm_hour = (int) (secondsOfDay / 3600);
    t->tm_min = (int) ((secondsOfDay / 60) % 60);
    t->tm_sec = (int) (secondsOfDay % 60);
    t->tm_wday = (int) ((((days % 7) + 7) % 7 + 4) % 7);
    t->tm_isdst = 0;
}
#endif

void teavm_date_init() {
    struct tm epochStart = {
        .tm_year = 70,
        .tm_mon = 0,
        .tm_mday = 1,
        .tm_hour = 0,
        .tm_min = 0,
        .tm_sec = 0,
        .tm_isdst = -1
    };
#if TEAVM_PSP
    teavm_epochStart = mktime(&epochStart);
#else
    teavm_epochStart = timegm(&epochStart);
#endif
    localtime_r(&teavm_epochStart, &teavm_epochStartTm);
#if TEAVM_WINDOWS
    teavm_date_localOffsetSeconds = teavm_date_toEpochMillis(&teavm_epochStartTm) / 1000;
#endif
}

inline static int64_t teavm_date_timestamp(struct tm *t) {
#if TEAVM_WINDOWS
    errno = 0;
    time_t result = mktime(t);
    if (result == (time_t) -1 && errno != 0) {
        int64_t utcMillis = teavm_date_toEpochMillis(t) - teavm_date_localOffsetSeconds * INT64_C(1000);
        return utcMillis - (int64_t) 1000 * teavm_epochStart;
    }
#else
    time_t result = mktime(t);
#endif
    return (int64_t) (1000 * difftime(result, teavm_epochStart));
}

int64_t teavm_date_timeToTimestamp(time_t t) {
    return (int64_t) (1000 * difftime(t, teavm_epochStart));
}

time_t teavm_date_timestampToTime(int64_t timestamp) {
    int64_t seconds = (timestamp / 1000);
    struct tm t = {
        .tm_year = 70,
        .tm_mon = 0,
        .tm_mday = 1,
        .tm_hour = (int) (seconds / 3600),
        .tm_min = (int) ((seconds / 60) % 60),
        .tm_sec = (int) (seconds % 60),
        .tm_isdst = -1
    };
#if TEAVM_PSP
    return mktime(&t) + timestamp % 1000;  // Approximate with mktime for PSP
#else
    return timegm(&t) + timestamp % 1000;
#endif
}

inline static struct tm* teavm_date_decompose(int64_t timestamp, struct tm *t) {
    *t = teavm_epochStartTm;
    int64_t seconds = (timestamp / 1000);
    t->tm_sec += (int) (seconds % 60);
    t->tm_min += (int) ((seconds / 60) % 60);
    t->tm_hour += (int) (seconds / 3600);
#if TEAVM_WINDOWS
    errno = 0;
    time_t result = mktime(t);
    if (result == (time_t) -1 && errno != 0) {
        int64_t localMillis = timestamp + teavm_date_localOffsetSeconds * INT64_C(1000);
        teavm_date_fromEpochMillis(localMillis, t);
    }
#else
    mktime(t);
#endif
    return t;
}

int64_t teavm_date_create(int32_t year, int32_t month, int32_t day, int32_t hour, int32_t minute, int32_t second) {
    struct tm t = {
        .tm_year = year,
        .tm_mon = month,
        .tm_mday = day,
        .tm_hour = hour,
        .tm_min = minute,
        .tm_sec = second,
        .tm_isdst = -1
    };
    return teavm_date_timestamp(&t);
}

int64_t teavm_date_createUtc(int32_t year, int32_t month, int32_t day, int32_t hour, int32_t minute, int32_t second) {
    struct tm t = {
        .tm_year = year,
        .tm_mon = month,
        .tm_mday = day,
        .tm_hour = hour,
        .tm_min = minute,
        .tm_sec = second,
        .tm_isdst = -1
    };
#if TEAVM_PSP
    time_t result = mktime(&t);  // Approximate with mktime for PSP
#elif TEAVM_WINDOWS
    errno = 0;
    time_t result = timegm(&t);
    if (result == (time_t) -1 && errno != 0) {
        return teavm_date_toEpochMillis(&t) - (int64_t) 1000 * teavm_epochStart;
    }
#else
    time_t result = timegm(&t);
#endif
    return (int64_t) (1000 * difftime(result, teavm_epochStart));
}

int64_t teavm_date_parse(char* s) {
    #if TEAVM_UNIX
        struct tm t;
        strptime(s, teavm_date_defaultFormat, &t);
        time_t result = mktime(&t);
        return (int64_t) (1000 * difftime(result, teavm_epochStart));
    #else
        return 0;
    #endif
}

int32_t teavm_date_getYear(int64_t time) {
    struct tm t;
    return (int32_t) teavm_date_decompose(time, &t)->tm_year;
}

int64_t teavm_date_setYear(int64_t time, int32_t year) {
    struct tm t;
    teavm_date_decompose(time, &t)->tm_year = year;
    return teavm_date_timestamp(&t);
}

int32_t teavm_date_getMonth(int64_t time) {
    struct tm t;
    return (int32_t) teavm_date_decompose(time, &t)->tm_mon;
}

int64_t teavm_date_setMonth(int64_t time, int32_t month) {
    struct tm t;
    teavm_date_decompose(time, &t)->tm_mon = month;
    return teavm_date_timestamp(&t);
}

int32_t teavm_date_getDate(int64_t time) {
    struct tm t;
    return (int32_t) teavm_date_decompose(time, &t)->tm_mday;
}

int64_t teavm_date_setDate(int64_t time, int32_t date) {
    struct tm t;
    teavm_date_decompose(time, &t)->tm_mday = date;
    return teavm_date_timestamp(&t);
}

int32_t teavm_date_getDay(int64_t time) {
    struct tm t;
    return (int32_t) teavm_date_decompose(time, &t)->tm_wday;
}

int32_t teavm_date_getHours(int64_t time) {
    struct tm t;
    return (int32_t) teavm_date_decompose(time, &t)->tm_hour;
}

int64_t teavm_date_setHours(int64_t time, int32_t hours) {
    struct tm t;
    teavm_date_decompose(time, &t)->tm_hour = hours;
    return teavm_date_timestamp(&t);
}

int32_t teavm_date_getMinutes(int64_t time) {
    struct tm t;
    return (int32_t) teavm_date_decompose(time, &t)->tm_min;
}

int64_t teavm_date_setMinutes(int64_t time, int32_t minutes) {
    struct tm t;
    teavm_date_decompose(time, &t)->tm_min = minutes;
    return teavm_date_timestamp(&t);
}

int32_t teavm_date_getSeconds(int64_t time) {
    struct tm t;
    return (int32_t) teavm_date_decompose(time, &t)->tm_sec;
}

int64_t teavm_date_setSeconds(int64_t time, int32_t seconds) {
    struct tm t;
    teavm_date_decompose(time, &t)->tm_sec = seconds;
    return teavm_date_timestamp(&t);
}

char* teavm_date_format(int64_t time) {
    struct tm t;
    teavm_date_decompose(time, &t);
    strftime(teavm_date_formatBuffer, 512, teavm_date_defaultFormat, &t);
    return teavm_date_formatBuffer;
}