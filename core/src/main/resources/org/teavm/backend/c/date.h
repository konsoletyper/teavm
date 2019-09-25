#pragma once
#include <stdint.h>
#include <time.h>

extern void teavm_date_init();
extern int64_t teavm_date_timeToTimestamp(time_t);
extern time_t teavm_date_timestampToTime(int64_t);
extern int64_t teavm_date_create(int32_t,int32_t,int32_t,int32_t,int32_t,int32_t);
extern int64_t teavm_date_createUtc(int32_t,int32_t,int32_t,int32_t,int32_t,int32_t);
extern int64_t teavm_date_parse(char*);
extern int32_t teavm_date_getYear(int64_t);
extern int64_t teavm_date_setYear(int64_t,int32_t);
extern int32_t teavm_date_getMonth(int64_t);
extern int64_t teavm_date_setMonth(int64_t,int32_t);
extern int32_t teavm_date_getDate(int64_t);
extern int64_t teavm_date_setDate(int64_t,int32_t);
extern int32_t teavm_date_getDay(int64_t);
extern int32_t teavm_date_getHours(int64_t);
extern int64_t teavm_date_setHours(int64_t,int32_t);
extern int32_t teavm_date_getMinutes(int64_t);
extern int64_t teavm_date_setMinutes(int64_t,int32_t);
extern int32_t teavm_date_getSeconds(int64_t);
extern int64_t teavm_date_setSeconds(int64_t,int32_t);
extern char* teavm_date_format(int64_t);