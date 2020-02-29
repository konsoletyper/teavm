#include "fiber.h"
#include "definitions.h"
#include <stddef.h>
#include <locale.h>
#include <time.h>

#if TEAVM_UNIX
    #include <signal.h>
#endif
#if TEAVM_WINDOWS
    #include <Windows.h>
    #include <synchapi.h>
#endif

#if TEAVM_UNIX
    static timer_t teavm_queueTimer;
#endif
#if TEAVM_WINDOWS
    static HANDLE teavm_queueTimer;
#endif

void teavm_initFiber() {

    #if TEAVM_UNIX
        setlocale (LC_ALL, "");

        struct sigaction sigact;
        sigact.sa_flags = 0;
        sigact.sa_handler = NULL;
        sigaction(SIGRTMIN, &sigact, NULL);

        sigset_t signals;
        sigemptyset(&signals);
        sigaddset(&signals, SIGRTMIN);
        sigprocmask(SIG_BLOCK, &signals, NULL);

        struct sigevent sev;
        sev.sigev_notify = SIGEV_SIGNAL;
        sev.sigev_signo = SIGRTMIN;
        timer_create(CLOCK_REALTIME, &sev, &teavm_queueTimer);
    #endif

    #if TEAVM_WINDOWS
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


#if TEAVM_UNIX
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

#if TEAVM_WINDOWS
    void teavm_waitFor(int64_t timeout) {
        WaitForSingleObject(teavm_queueTimer, (DWORD) timeout);
        ResetEvent(teavm_queueTimer);
    }

    void teavm_interrupt() {
        SetEvent(teavm_queueTimer);
    }
#endif
