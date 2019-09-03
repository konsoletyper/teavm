#include "date.h"
#include "definitions.h"

#ifndef _XOPEN_SOURCE
#define _XOPEN_SOURCE
#endif

#ifndef __USE_XOPEN
#define __USE_XOPEN
#endif

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#if TEAVM_WINDOWS
    #define timegm _mkgmtime
    #define localtime_r(a, b) localtime_s(b, a)
#endif

static time_t teavm_epochStart;
static struct tm teavm_epochStartTm;
static char teavm_date_formatBuffer[512];
static char* teavm_date_defaultFormat = "%a %b %d %H:%M:%S %Z %Y";

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
    teavm_epochStart = timegm(&epochStart);
    localtime_r(&teavm_epochStart, &teavm_epochStartTm);
}

inline static int64_t teavm_date_timestamp(struct tm *t) {
    time_t result = mktime(t);
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
    return timegm(&t) + timestamp % 1000;
}

inline static struct tm* teavm_date_decompose(int64_t timestamp, struct tm *t) {
    *t = teavm_epochStartTm;
    int64_t seconds = (timestamp / 1000);
    t->tm_sec += (int) (seconds % 60);
    t->tm_min += (int) ((seconds / 60) % 60);
    t->tm_hour += (int) (seconds / 3600);
    mktime(t);
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
    time_t result = timegm(&t);
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
    t = teavm_epochStartTm;
    int64_t seconds = (time / 1000);
    t.tm_sec += (int) (seconds % 60);
    t.tm_min += (int) ((seconds / 60) % 60);
    t.tm_hour += (int) (seconds / 3600);
    mktime(&t);
    strftime(teavm_date_formatBuffer, 512, teavm_date_defaultFormat, &t);
    return teavm_date_formatBuffer;
}