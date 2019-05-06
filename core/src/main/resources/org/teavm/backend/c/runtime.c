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
#endif

#ifdef _MSC_VER
#include <Windows.h>
#endif

void* teavm_gc_heapAddress = NULL;

void** teavm_stackTop;

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
        if (teavm_equals(map->entries[index].key, string)) {
            return &map->entries[index];
        }
    }
    return NULL;
}

#ifdef __GNUC__
static timer_t teavm_queueTimer;
#endif

void teavm_beforeInit() {
    srand(time(NULL));

    #ifdef __GNUC__
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
}

#ifdef __GNUC__
void teavm_initHeap(int64_t heapSize) {
    long workSize = heapSize / 16;
    long regionsSize = (long) (heapSize / teavm_gc_regionSize);

    long pageSize = sysconf(_SC_PAGE_SIZE);
    int heapPages = (int) ((heapSize + pageSize + 1) / pageSize * pageSize);
    int workPages = (int) ((workSize + pageSize + 1) / pageSize * pageSize);
    int regionsPages = (int) ((regionsSize * 2 + pageSize + 1) / pageSize * pageSize);

    teavm_gc_heapAddress = mmap(
            NULL,
            heapPages,
            PROT_READ | PROT_WRITE,
            MAP_PRIVATE | MAP_ANONYMOUS,
            0, 0);
    teavm_gc_gcStorageAddress = mmap(
            NULL,
            workPages,
            PROT_READ | PROT_WRITE,
            MAP_PRIVATE | MAP_ANONYMOUS,
            0, 0);
    teavm_gc_regionsAddress = mmap(
            NULL,
            regionsPages,
            PROT_READ | PROT_WRITE,
            MAP_PRIVATE | MAP_ANONYMOUS,
            0, 0);

    teavm_gc_gcStorageSize = (int) workSize;
    teavm_gc_regionMaxCount = regionsSize;
    teavm_gc_availableBytes = heapSize;
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
void teavm_initHeap(int64_t heapSize) {
    long workSize = heapSize / 16;
    long regionsSize = (long) (heapSize / teavm_gc_regionSize);

    SYSTEM_INFO systemInfo;
    GetSystemInfo(&systemInfo);
    long pageSize = systemInfo.dwPageSize;
    int heapPages = (int) ((heapSize + pageSize + 1) / pageSize * pageSize);
    int workPages = (int) ((workSize + pageSize + 1) / pageSize * pageSize);
    int regionsPages = (int) ((regionsSize * 2 + pageSize + 1) / pageSize * pageSize);

    teavm_gc_heapAddress = VirtualAlloc(
            NULL,
            heapPages,
            MEM_RESERVE | MEM_COMMIT,
            PAGE_READWRITE
    );
    teavm_gc_gcStorageAddress = VirtualAlloc(
            NULL,
            workPages,
            MEM_RESERVE | MEM_COMMIT,
            PAGE_READWRITE
    );
    teavm_gc_regionsAddress = VirtualAlloc(
            NULL,
            regionsPages,
            MEM_RESERVE | MEM_COMMIT,
            PAGE_READWRITE
    );

    teavm_gc_gcStorageSize = (int) workSize;
    teavm_gc_regionMaxCount = regionsSize;
    teavm_gc_availableBytes = heapSize;
}

static SYSTEMTIME teavm_unixEpochStart = {
    .wYear = 1970,
    .wMonth = 1,
    .wDayOfWeek = 3,
    .wDay = 1,
    .wHour = 0,
    .wMinute = 0,
    .wSecond = 0,
    .wMilliseconds = 0
};

int64_t teavm_currentTimeMillis() {
    SYSTEMTIME time;
    FILETIME fileTime;
    GetSystemTime(&time);
    SystemTimeToFileTime(&time, &fileTime);

    FILETIME fileTimeStart;
    SystemTimeToFileTime(&teavm_unixEpochStart, &fileTimeStart);

    uint64_t current = fileTime.dwLowDateTime | ((uint64_t) fileTime.dwHighDateTime << 32);
    uint64_t start = fileTimeStart.dwLowDateTime | ((uint64_t) fileTimeStart.dwHighDateTime << 32);

    return (int64_t) ((current - start) / 10000);
}
#endif

int32_t teavm_timeZoneOffset() {
    time_t t = time(NULL);
    time_t local = mktime(localtime(&t));
    time_t utc = mktime(gmtime(&t));
    return difftime(utc, local) / 60;
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

void teavm_outOfMemory() {
    fprintf(stderr, "Application crashed due to lack of free memory\n");
    exit(1);
}

void teavm_printString(char* s) {
    fprintf(stderr, "%s", s);
}

void teavm_printInt(int32_t i) {
    fprintf(stderr, "%" PRId32, i);
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
    char buffer[__STDC_UTF_16__];
    mbstate_t state = {0};
    for (int32_t i = 0; i < javaCharsCount; ++i) {
        size_t result = c16rtomb(buffer, javaChars[i], &state);
        if (result < 0) {
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
        if (result == -1) {
            break;
        } else if (result >= 0) {
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

    int32_t j = 0;
    char* dst = result;
    mbstate_t state = {0};
    for (int32_t i = 0; i < charArray->size; ++i) {
        dst += c16rtomb(dst, javaChars[i], &state);
    }
    *dst = '\0';
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
        int32_t result = mbrtoc16(javaChars++, cstring, clen, &state);
        if (result == -1) {
            break;
        } else if (result >= 0) {
            clen -= result;
            cstring += result;
        }
    }
    return teavm_createString(charArray);
}

TeaVM_Array* teavm_parseArguments(int argc, char** argv) {
    TeaVM_Array* array = teavm_allocateStringArray(argc - 1);
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