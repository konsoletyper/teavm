#include "runtime.h"
#include <string.h>
#include <stdint.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <uchar.h>
#include <wchar.h>
#include <wctype.h>
#include <time.h>
#include <math.h>

#ifdef __GNUC__
#include <unistd.h>
#include <sys/mman.h>
#include <signal.h>
#include <locale.h>
#endif

#ifdef _MSC_VER
#include <Windows.h>
#include <synchapi.h>
#endif

void* teavm_gc_heapAddress = NULL;
#ifdef TEAVM_MEMORY_TRACE
    uint8_t* teavm_gc_heapMap = NULL;
    uint8_t* teavm_gc_markMap = NULL;
#endif

TeaVM_StackFrame* teavm_stackTop = NULL;

void* teavm_gc_gcStorageAddress = NULL;
int32_t teavm_gc_gcStorageSize = INT32_C(0);
void* teavm_gc_regionsAddress = NULL;
int32_t teavm_gc_regionSize = INT32_C(32768);
int32_t teavm_gc_regionMaxCount = INT32_C(0);
int64_t teavm_gc_availableBytes = INT64_C(0);

char *teavm_beforeClasses;

double teavm_rand() {
    return rand() / ((double) RAND_MAX + 1);
}

TeaVM_ResourceMapEntry* teavm_lookupResource(TeaVM_ResourceMap *map, TeaVM_String* string) {
    uint32_t hashCode = teavm_hashCode(string);
    for (int32_t i = 0; i < map->size; ++i) {
        uint32_t index = (hashCode + i) % map->size;
        if (map->entries[index].key == NULL) {
            return NULL;
        }
        if (teavm_equals(*map->entries[index].key, string)) {
            return &map->entries[index];
        }
    }
    return NULL;
}

#ifdef __GNUC__
static timer_t teavm_queueTimer;
#endif

#ifdef _MSC_VER
static HANDLE teavm_queueTimer;
int64_t teavm_unixTimeOffset;
int64_t teavm_perfFrequency, teavm_perfInitTime;
#endif

void teavm_beforeInit() {
    srand((unsigned int) time(NULL));

    #ifdef __GNUC__
        setlocale (LC_ALL, "");

        struct sigaction sigact;
        sigact.sa_flags = 0;
        sigact.sa_handler = NULL;
        sigaction(SIGRTMIN, &sigact, NULL);

        sigset_t signals;
        sigemptyset(&signals );
        sigaddset(&signals, SIGRTMIN);
        sigprocmask(SIG_BLOCK, &signals, NULL);

        struct sigevent sev;
        sev.sigev_notify = SIGEV_SIGNAL;
        sev.sigev_signo = SIGRTMIN;
        timer_create(CLOCK_REALTIME, &sev, &teavm_queueTimer);
    #endif

    #ifdef _MSC_VER
        LARGE_INTEGER perf = { .QuadPart  = 0 };
        QueryPerformanceFrequency(&perf);
        teavm_perfFrequency = perf.QuadPart;
        QueryPerformanceCounter(&perf);
        teavm_perfInitTime = perf.QuadPart;

        teavm_queueTimer = CreateEvent(NULL, TRUE, FALSE, TEXT("TeaVM_eventQueue"));

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

#ifdef __GNUC__

static void* teavm_virtualAlloc(int size) {
    return mmap(NULL, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, 0, 0);
}

static long teavm_pageSize() {
    return sysconf(_SC_PAGE_SIZE);
}

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

#ifdef _MSC_VER
static void* teavm_virtualAlloc(int size) {
    #ifdef _WINDOWS_UWP
        return VirtualAllocFromApp(
                NULL,
                size,
                MEM_RESERVE | MEM_COMMIT,
                PAGE_READWRITE
        );
    #else
        return VirtualAlloc(
                NULL,
                size,
                MEM_RESERVE | MEM_COMMIT,
                PAGE_READWRITE
        );
    #endif
}

static long teavm_pageSize() {
    SYSTEM_INFO systemInfo;
    GetSystemInfo(&systemInfo);
    return systemInfo.dwPageSize;
}

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

static int teavm_pageCount(int64_t size, int64_t pageSize) {
    return (int) ((size + pageSize + 1) / pageSize * pageSize);
}

void teavm_initHeap(int64_t heapSize) {
    long workSize = (long) (heapSize / 16);
    long regionsSize = (long) (heapSize / teavm_gc_regionSize) + 1;
    long pageSize = teavm_pageSize();

    teavm_gc_heapAddress = teavm_virtualAlloc(teavm_pageCount(heapSize, pageSize));
    teavm_gc_gcStorageAddress = teavm_virtualAlloc(teavm_pageCount(workSize, pageSize));
    teavm_gc_regionsAddress = teavm_virtualAlloc(teavm_pageCount(regionsSize * 2, pageSize));

    #ifdef TEAVM_MEMORY_TRACE
        int64_t heapMapSize = heapSize / sizeof(void*);
        teavm_gc_heapMap = teavm_virtualAlloc(teavm_pageCount(heapMapSize, pageSize));
        memset(teavm_gc_heapMap, 0, heapMapSize);
        teavm_gc_markMap = teavm_virtualAlloc(teavm_pageCount(heapMapSize, pageSize));
    #endif

    teavm_gc_gcStorageSize = (int) workSize;
    teavm_gc_regionMaxCount = regionsSize;
    teavm_gc_availableBytes = heapSize;
}

#ifdef _MSC_VER
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

#ifdef __GNUC__

void teavm_waitFor(int64_t timeout) {
    struct itimerspec its = {0};
    its.it_value.tv_sec = timeout / 1000;
    its.it_value.tv_nsec = (timeout % 1000) * 1000000L;
    timer_settime(teavm_queueTimer, 0, &its, NULL);

    sigset_t signals;
    sigemptyset(&signals);
    sigaddset(&signals, SIGRTMIN);
    siginfo_t actualSignal;
    sigwaitinfo(&signals, &actualSignal);
}

void teavm_interrupt() {
    struct itimerspec its = {0};
    timer_settime(teavm_queueTimer, 0, &its, NULL);
    raise(SIGRTMIN);
}

#endif

#ifdef _MSC_VER

void teavm_waitFor(int64_t timeout) {
    WaitForSingleObject(teavm_queueTimer, (DWORD) timeout);
    ResetEvent(teavm_queueTimer);
}

void teavm_interrupt() {
    SetEvent(teavm_queueTimer);
}

#endif

#ifndef TEAVM_WINDOWS_LOG
    #define TEAVM_OUTPUT_STRING(s) fprintf(stderr, s)
#else
    #define TEAVM_OUTPUT_STRING(s) OutputDebugStringW(L##s)
#endif

void teavm_outOfMemory() {
    TEAVM_OUTPUT_STRING("Application crashed due to lack of free memory\n");
    abort();
}

static char16_t* teavm_utf16ToUtf32(char16_t* source, char32_t* target) {
    char16_t c = *source;
    if ((c & 0xFC00) == 0xD800) {
        char16_t n = *(source + 1);
        if ((n & 0xFC00) == 0xDC00) {
            *target = (((c & ~0xFC00) << 10) | (n & ~0xFC00)) + 0x10000;
            return source + 2;
        }
    }
    *target = c;
    return source + 1;
}

void teavm_printString(char16_t* s) {
    #ifndef TEAVM_WINDOWS_LOG
        #ifdef _MSC_VER
            fprintf(stderr, "%ls", s);
        #else
            int32_t cap = 128;
            wchar_t* buf = malloc(sizeof(wchar_t) * cap);
            wchar_t* out = buf;
            int32_t sz = 0;
            while (*s != '\0') {
                s = teavm_utf16ToUtf32(s, out++);
                if (++sz == cap) {
                    cap *= 2;
                    buf = realloc(buf, sizeof(wchar_t) * cap);
                    out = buf + sz;
                }
            }
            *out = '\0';
            fprintf(stderr, "%ls", buf);
            free(buf);
        #endif
    #else
        OutputDebugStringW(s);
    #endif
}

void teavm_printInt(int32_t i) {
    #ifndef TEAVM_WINDOWS_LOG
        fprintf(stderr, "%" PRId32, i);
    #else
        wchar_t str[10];
        swprintf(str, 10, L"%d", i);
        OutputDebugStringW(str);
    #endif
}

int32_t teavm_hashCode(TeaVM_String* string) {
    int32_t hashCode = INT32_C(0);
    int32_t length = string->characters->size;
    char16_t* chars = TEAVM_ARRAY_DATA(string->characters, char16_t);
    for (int32_t i = INT32_C(0); i < length; ++i) {
        hashCode = 31 * hashCode + chars[i];
    }
    return hashCode;
}

int32_t teavm_equals(TeaVM_String* first, TeaVM_String* second) {
    if (first->characters->size != second->characters->size) {
        return 0;
    }

    char16_t* firstChars = TEAVM_ARRAY_DATA(first->characters, char16_t);
    char16_t* secondChars = TEAVM_ARRAY_DATA(second->characters, char16_t);
    int32_t length = first->characters->size;
    for (int32_t i = INT32_C(0); i < length; ++i) {
        if (firstChars[i] != secondChars[i]) {
            return 0;
        }
    }
    return 1;
}

TeaVM_Array* teavm_resourceMapKeys(TeaVM_ResourceMap *map) {
    int32_t size = 0;
    for (int32_t i = 0; i < map->size; ++i) {
        if (map->entries[i].key != NULL) {
            size++;
        }
    }

    int32_t index = 0;
    void* array = teavm_allocateStringArray(size);
    void** data = TEAVM_ARRAY_DATA(array, void*);
    for (int32_t i = 0; i < map->size; ++i) {
        if (map->entries[i].key != NULL) {
            data[index++] = map->entries[i].key;
        }
    }

    return array;
}

size_t teavm_mbSize(char16_t* javaChars, int32_t javaCharsCount) {
    size_t sz = 0;
    char buffer[8];
    mbstate_t state = {0};
    for (int32_t i = 0; i < javaCharsCount; ++i) {
        size_t result = c16rtomb(buffer, javaChars[i], &state);
        if (result == (size_t) -1) {
            break;
        }
        sz += result;
    }
    return sz;
}

int32_t teavm_c16Size(char* cstring, size_t count) {
    mbstate_t state = {0};
    int32_t sz = 0;
    while (count > 0) {
        size_t result = mbrtoc16(NULL, cstring, count, &state);
        if (result == (size_t) -1) {
            break;
        } else if ((int) result >= 0) {
            sz++;
            count -= result;
            cstring += result;
        }
    }

    return sz;
}

char* teavm_stringToC(void* obj) {
    if (obj == NULL) {
        return NULL;
    }

    TeaVM_String* javaString = (TeaVM_String*) obj;
    TeaVM_Array* charArray = javaString->characters;
    char16_t* javaChars = TEAVM_ARRAY_DATA(charArray, char16_t);

    size_t sz = teavm_mbSize(javaChars, charArray->size);
    char* result = malloc(sz + 1);

    char* dst = result;
    mbstate_t state = {0};
    for (int32_t i = 0; i < charArray->size; ++i) {
        size_t charResult = c16rtomb(dst, javaChars[i], &state);
        if (charResult == (size_t) -1) {
            break;
        }
        dst += charResult;
    }
    *dst = '\0';
    return result;
}

char16_t* teavm_stringToC16(void* obj) {
    if (obj == NULL) {
        return NULL;
    }

    TeaVM_String* javaString = (TeaVM_String*) obj;
    TeaVM_Array* charArray = javaString->characters;
    char16_t* javaChars = TEAVM_ARRAY_DATA(charArray, char16_t);
    size_t sz = charArray->size;
    char16_t* result = malloc((sz + 1) * sizeof(char16_t));
    memcpy(result, javaChars, sz * sizeof(char16_t));
    result[sz] = 0;
    return result;
}

TeaVM_String* teavm_cToString(char* cstring) {
    if (cstring == NULL) {
        return NULL;
    }

    size_t clen = strlen(cstring);
    int32_t size = teavm_c16Size(cstring, clen);
    TeaVM_Array* charArray = teavm_allocateCharArray(size);
    char16_t* javaChars = TEAVM_ARRAY_DATA(charArray, char16_t);
    mbstate_t state = {0};
    for (int32_t i = 0; i < size; ++i) {
        size_t result = mbrtoc16(javaChars++, cstring, clen, &state);
        if (result == (size_t) -1) {
            break;
        } else if ((int) result >= 0) {
            clen -= result;
            cstring += result;
        }
    }
    return teavm_createString(charArray);
}

TeaVM_String* teavm_c16ToString(char16_t* cstring) {
    if (cstring == NULL) {
        return NULL;
    }

    int32_t size = 0;
    while (cstring[size] != 0) {
        ++size;
    }
    TeaVM_Array* charArray = teavm_allocateCharArray(size);
    char16_t* javaChars = TEAVM_ARRAY_DATA(charArray, char16_t);
    memcpy(javaChars, cstring, size * sizeof(char16_t));
    return teavm_createString(charArray);
}

char16_t* teavm_mbToChar16(char* cstring, int32_t* length) {
    size_t clen = strlen(cstring);
    int32_t size = teavm_c16Size(cstring, clen);
    char16_t* javaChars = malloc(sizeof(char16_t) * (size + 2));
    mbstate_t state = {0};
    for (int32_t i = 0; i < size; ++i) {
        size_t result = mbrtoc16(javaChars + i, cstring, clen, &state);
        if (result == (size_t) -1) {
            break;
        } else if ((int) result >= 0) {
            clen -= result;
            cstring += result;
        }
    }
    *length = size;
    return javaChars;
}

char* teavm_char16ToMb(char16_t* javaChars, int32_t length) {
    size_t sz = teavm_mbSize(javaChars, length);
    char* cchars = malloc(sz + 1);

    char* dst = cchars;
    mbstate_t state = {0};
    for (int32_t i = 0; i < length; ++i) {
        size_t result = c16rtomb(dst, javaChars[i], &state);
        if (result == -1) {
            break;
        }
        dst += result;
    }
    *dst = '\0';
    return cchars;
}

TeaVM_Array* teavm_parseArguments(int argc, char** argv) {
    TeaVM_Array* array = teavm_allocateStringArray(argc > 0 ? argc - 1 : 0);
    TeaVM_String** arrayData = TEAVM_ARRAY_DATA(array, TeaVM_String*);
    for (int i = 1; i < argc; ++i) {
        arrayData[i - 1] = teavm_cToString(argv[i]);
    }
    return array;
}

typedef struct TeaVM_StaticGcRootDescriptor {
    void*** roots;
    int count;
} TeaVM_StaticGcRootDescriptor;

typedef struct TeaVM_StaticGcRootDescriptorTable {
    struct TeaVM_StaticGcRootDescriptorTable* next;
    int size;
    TeaVM_StaticGcRootDescriptor data[256];
} TeaVM_StaticGcRootDescriptorTable;

static TeaVM_StaticGcRootDescriptorTable *teavm_staticGcRootsBuilder = NULL;
static int teavm_staticGcRootDataSize = 0;
void*** teavm_gc_staticRoots;

void teavm_registerStaticGcRoots(void*** roots, int count) {
    if (count == 0) {
        return;
    }

    TeaVM_StaticGcRootDescriptorTable* builder = teavm_staticGcRootsBuilder;
    if (builder == NULL || builder->size == 256) {
        builder = malloc(sizeof(TeaVM_StaticGcRootDescriptorTable));
        builder->size = 0;
        builder->next = teavm_staticGcRootsBuilder;
        teavm_staticGcRootsBuilder = builder;
    }

    int i = builder->size++;
    builder->data[i].roots = roots;
    builder->data[i].count = count;
    teavm_staticGcRootDataSize += count;
}

void teavm_afterInitClasses() {
    teavm_gc_staticRoots = malloc(sizeof(void**) * (teavm_staticGcRootDataSize + 1));

    void*** target = teavm_gc_staticRoots;
    *target++ = (void**) (intptr_t) teavm_staticGcRootDataSize;
    TeaVM_StaticGcRootDescriptorTable* builder = teavm_staticGcRootsBuilder;

    while (builder != NULL) {
        for (int j = 0; j < builder->size; ++j) {
            int count = builder->data[j].count;
            memcpy(target, builder->data[j].roots, count * sizeof(void**));
            target += count;
        }
        TeaVM_StaticGcRootDescriptorTable* next = builder->next;
        free(builder);
        builder = next;
    }
}

void teavm_disposeStringList(TeaVM_StringList* list) {
    while (list != NULL) {
        TeaVM_StringList* next = list->next;
        if (list->data != NULL) {
            free(list->data);
        }
        free(list);
        list = next;
    }
}
TeaVM_StringList* teavm_appendString(TeaVM_StringList* list, char16_t* data, int32_t length) {
    TeaVM_StringList* entry = malloc(sizeof(TeaVM_StringList));
    if (entry == NULL) {
        teavm_disposeStringList(list);
        return NULL;
    }
    entry->data = data;
    entry->length = length;
    entry->next = list;
    return entry;
}

#ifndef TEAVM_WINDOWS_LOG
void teavm_logchar(int32_t c) {
    putwchar(c);
}
#else
void teavm_logchar(int32_t c) {
	char16_t buffer[2] = { (char16_t) c, 0 };
	OutputDebugStringW(buffer);
}
#endif

#ifdef TEAVM_MEMORY_TRACE

static wchar_t* teavm_gc_dumpDirectory = NULL;

void teavm_gc_assertSize(int32_t size) {
    if (size % sizeof(void*) != 0) {
        abort();
    }
}

#endif

void teavm_gc_allocate(void* address, int32_t size) {
    #ifdef TEAVM_MEMORY_TRACE
        teavm_gc_assertAddress(address);
        teavm_gc_assertSize(size);

        size /= sizeof(void*);
        uint8_t* map = teavm_gc_heapMap + (((char*) address - (char*) teavm_gc_heapAddress) / sizeof(void*));

        if (*map != 0) {
            fprintf(stderr, "[GC] trying allocate at memory in use at: %d\n",
                    (int) ((char*) address - (char*) teavm_gc_heapAddress));
            abort();
        }
        *map++ = 1;

        for (int32_t i = 1; i < size; ++i) {
            if (*map != 0) {
                fprintf(stderr, "[GC] trying allocate at memory in use at: %d\n",
                        (int) ((char*) address - (char*) teavm_gc_heapAddress));
                abort();
            }
            *map++ = 2;
        }
    #endif
}

void teavm_gc_free(void* address, int32_t size) {
    #ifdef TEAVM_MEMORY_TRACE
        teavm_gc_assertAddress(address);
        teavm_gc_assertSize(size);

        int32_t offset = (int32_t) (((char*) address - (char*) teavm_gc_heapAddress) / sizeof(void*));
        uint8_t* markMap = teavm_gc_markMap + offset;
        size /= sizeof(void*);
        for (int32_t i = 0; i < size; ++i) {
            if (markMap[i] != 0) {
                fprintf(stderr, "[GC] trying to release reachable object at: %d\n",
                        (int) ((char*) address - (char*) teavm_gc_heapAddress));
                abort();
            }
        }

        uint8_t* map = teavm_gc_heapMap + offset;
        memset(map, 0, size);
    #endif
}

void teavm_gc_assertFree(void* address, int32_t size) {
    #ifdef TEAVM_MEMORY_TRACE
        teavm_gc_assertAddress(address);
        teavm_gc_assertSize(size);

        int32_t offset = (int32_t) (((char*) address - (char*) teavm_gc_heapAddress) / sizeof(void*));
        uint8_t* map = teavm_gc_heapMap + offset;
        size /= sizeof(void*);
        for (int32_t i = 0; i < size; ++i) {
            if (map[i] != 0) {
                fprintf(stderr, "[GC] memory supposed to be free at: %d\n",
                        (int) ((char*) address - (char*) teavm_gc_heapAddress));
                abort();
            }
        }
    #endif
}

void teavm_gc_initMark() {
    #ifdef TEAVM_MEMORY_TRACE
        memset(teavm_gc_markMap, 0, teavm_gc_availableBytes / sizeof(void*));
    #endif
}

#ifdef TEAVM_MEMORY_TRACE
int32_t teavm_gc_objectSize(void* address) {
    TeaVM_Class* cls = TEAVM_CLASS_OF(address);
    if (cls->itemType == NULL) {
        return cls->size;
    }

    int32_t itemSize = cls->itemType->flags & 2 ? cls->itemType->size : sizeof(void*);
    TeaVM_Array* array = (TeaVM_Array*) address;
    char* size = TEAVM_ALIGN((void*) sizeof(TeaVM_Array), itemSize);
    size += array->size * itemSize;
    size = TEAVM_ALIGN(size, sizeof(void*));
    return (int32_t) (intptr_t) size;
}
#endif

void teavm_gc_mark(void* address) {
    #ifdef TEAVM_MEMORY_TRACE
        if (address < teavm_gc_heapAddress
                || (char*) address >= (char*) teavm_gc_heapAddress + teavm_gc_availableBytes) {
            return;
        }

        teavm_gc_assertAddress(address);

        int32_t offset = (int32_t) (((char*) address - (char*) teavm_gc_heapAddress) / sizeof(void*));
        uint8_t* map = teavm_gc_heapMap + offset;
        uint8_t* markMap = teavm_gc_markMap + offset;

        int32_t size = teavm_gc_objectSize(address);
        teavm_gc_assertSize(size);
        size /= sizeof(void*);

        if (*map++ != 1 || *markMap != 0) {
            fprintf(stderr, "[GC] assertion failed marking object at: %d\n", (int) ((char*) address - (char*) teavm_gc_heapAddress));
            abort();
        }
        *markMap++ = 1;

        for (int32_t i = 1; i < size; ++i) {
            if (*map++ != 2 || *markMap != 0) {
                abort();
            }
            *markMap++ = 1;
        }
    #endif
}

void teavm_gc_move(void* from, void* to, int32_t size) {
    #ifdef TEAVM_MEMORY_TRACE
        teavm_gc_assertAddress(from);
        teavm_gc_assertAddress(to);
        teavm_gc_assertSize(size);

        uint8_t* mapFrom = teavm_gc_heapMap + (((char*) from - (char*) teavm_gc_heapAddress) / sizeof(void*));
        uint8_t* mapTo = teavm_gc_heapMap + (((char*) to - (char*) teavm_gc_heapAddress) / sizeof(void*));
        size /= sizeof(void*);

        if (mapFrom > mapTo) {
            for (int32_t i = 0; i < size; ++i) {
                if (mapFrom[i] == 0 || mapTo[i] != 0) {
                    fprintf(stderr, "[GC] assertion failed moving object from: %d to %d\n",
                        (int) ((char*) from - (char*) teavm_gc_heapAddress), (int) ((char*) to - (char*) teavm_gc_heapAddress));
                    abort();
                }
                mapTo[i] = mapFrom[i];
                mapFrom[i] = 0;
            }
        } else {
            for (int32_t i = size - 1; i >= 0; --i) {
                if (mapFrom[i] == 0 || mapTo[i] != 0) {
                    abort();
                }
                mapTo[i] = mapFrom[i];
                mapFrom[i] = 0;
            }
        }
    #endif
}

static FILE* teavm_gc_traceFile = NULL;

static FILE* teavm_gc_openDumpFile(wchar_t* name) {
    wchar_t* fullName = name;
    size_t fullNameLen = wcslen(name);
    if (teavm_gc_dumpDirectory != NULL) {
        size_t prefixLen = wcslen(teavm_gc_dumpDirectory);
        size_t nameLen = fullNameLen;
        fullNameLen = nameLen + prefixLen;
        fullName = malloc((prefixLen + nameLen + 1) * sizeof(wchar_t));
        memcpy(fullName, teavm_gc_dumpDirectory, prefixLen * sizeof(wchar_t));
        memcpy(fullName + prefixLen, name, (nameLen + 1) * sizeof(wchar_t));
    }

    FILE* result;
    #ifdef _MSC_VER
        _wfopen_s(&result, fullName, L"w");
    #else
        size_t fullNameMbSize = 3 * (fullNameLen + 1) * sizeof(wchar_t);
        char* fullNameMb = malloc(fullNameMbSize);
        mbstate_t state = { 0 };
        wcsrtombs(fullNameMb, fullName, fullNameMbSize, state);
        result = fopen(fullNameMb, "w");
    #endif

    if (fullName != name) {
        free(fullName);
    }

    return result;
}

#ifdef TEAVM_MEMORY_TRACE
    static void teavm_writeHeapMemory(char* name) {
        #ifdef TEAVM_GC_LOG
            if (teavm_gc_traceFile == NULL) {
                teavm_gc_traceFile = teavm_gc_openDumpFile(L"teavm-gc-trace.txt");
            }
            FILE* file = teavm_gc_traceFile;
            fprintf(file, "%s:", name);

            int32_t numbers = 4096;
            int64_t mapSize = teavm_gc_availableBytes / sizeof(void*);
            for (int i = 0; i < numbers; ++i) {
                int64_t start = mapSize * i / numbers;
                int64_t end = mapSize * (i + 1) / numbers;
                int count = 0;
                for (int j = start; j < end; ++j) {
                    if (teavm_gc_heapMap[j] != 0) {
                        count++;
                    }
                }
                int rate = count * 4096 / (end - start);
                fprintf(file, " %d", rate);
            }
            fprintf(file, "\n");
            fflush(file);
        #endif
    }

    void teavm_gc_checkHeapConsistency() {
        TeaVM_Object* obj = teavm_gc_heapAddress;
        while ((char*) obj < (char*) teavm_gc_heapAddress + teavm_gc_availableBytes) {
            int32_t size;
            if (obj->header == 0) {
                size = obj->hash;
                teavm_gc_assertFree(obj, size);
            } else {
                teavm_verify(obj);
                TeaVM_Class* cls = TEAVM_CLASS_OF(obj);
                if (cls->itemType != NULL) {
                    if (!(cls->itemType->flags & 2)) {
                        char* offset = NULL;
                        offset += sizeof(TeaVM_Array);
                        offset = TEAVM_ALIGN(offset, sizeof(void*));
                        void** data = (void**)((char*)obj + (uintptr_t)offset);
                        int32_t size = ((TeaVM_Array*)obj)->size;
                        for (int32_t i = 0; i < size; ++i) {
                            teavm_verify(data[i]);
                        }
                    }
                } else {
                    while (cls != NULL) {
                        int32_t kind = (cls->flags >> 7) & 7;
                        if (kind == 1) {

                        } else if (kind == 2) {

                        } else {
                            int16_t* layout = cls->layout;
                            if (layout != NULL) {
                                int16_t size = *layout++;
                                for (int32_t i = 0; i < size; ++i) {
                                    void** ptr = (void**) ((char*) obj + *layout++);
                                    teavm_verify(*ptr);
                                }
                            }
                        }

                        cls = cls->superclass;
                    }
                }
                size = teavm_gc_objectSize(obj);
            }
            obj = (TeaVM_Object*) ((char*) obj + size);
        }
    }
#endif

void teavm_gc_gcStarted() {
    #ifdef TEAVM_MEMORY_TRACE
        teavm_writeHeapMemory("start");
        teavm_gc_checkHeapConsistency();
    #endif
}

void teavm_gc_sweepCompleted() {
    #ifdef TEAVM_MEMORY_TRACE
        teavm_writeHeapMemory("sweep");
        teavm_gc_checkHeapConsistency();
    #endif
}

void teavm_gc_defragCompleted() {
    #ifdef TEAVM_MEMORY_TRACE
        teavm_writeHeapMemory("defrag");
    #endif
}

void teavm_gc_setDumpDirectory(const wchar_t* path) {
    #ifdef TEAVM_MEMORY_TRACE
        if (teavm_gc_dumpDirectory != NULL) {
            free(teavm_gc_dumpDirectory);
        }
        size_t pathLen = wcslen(path);
        size_t bytesLen = sizeof(wchar_t) * (pathLen + 1);
        teavm_gc_dumpDirectory = malloc(bytesLen);
        memcpy(teavm_gc_dumpDirectory, path, bytesLen);
    #endif
}