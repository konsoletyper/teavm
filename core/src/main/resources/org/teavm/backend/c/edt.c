#include "fiber.h"
#include "definitions.h"
#include <stddef.h>
#include <locale.h>
#include <time.h>

#if TEAVM_UNIX
    #include <signal.h>
    #if TEAVM_APPLE
        #include <unistd.h>
        #include <fcntl.h>
        #include <sys/select.h>
    #endif
#endif
#if TEAVM_WINDOWS
    #include <Windows.h>
    #include <synchapi.h>
#endif
#if TEAVM_PSP
    #include <pspkernel.h>
#endif

#if TEAVM_UNIX && !TEAVM_APPLE && !defined(__EMSCRIPTEN__)
    static timer_t teavm_queueTimer;
#endif
#if TEAVM_APPLE
    static int teavm_pipefd[2];
#endif
#if TEAVM_WINDOWS
    static HANDLE teavm_queueTimer;
#endif

void teavm_edt_init() {

    #if TEAVM_UNIX
        #if TEAVM_APPLE
            setlocale(LC_ALL, "");
            pipe(teavm_pipefd);
            fcntl(teavm_pipefd[0], F_SETFL, O_NONBLOCK);
        #elif !defined(__EMSCRIPTEN__)
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
    #ifdef __EMSCRIPTEN__
        void teavm_edt_waitFor(int64_t timeout) {
            abort();
        }
        void teavm_edt_interrupt() {
            abort();
        }
    #elif TEAVM_APPLE
        void teavm_edt_waitFor(int64_t timeout) {
            fd_set fds;
            FD_ZERO(&fds);
            FD_SET(teavm_pipefd[0], &fds);
            struct timeval tv;
            tv.tv_sec = (long) (timeout / 1000);
            tv.tv_usec = (int) ((timeout % 1000) * 1000);
            select(teavm_pipefd[0] + 1, &fds, NULL, NULL, &tv);
            char buf;
            while (read(teavm_pipefd[0], &buf, 1) > 0) {}
        }

        void teavm_edt_interrupt() {
            char c = 1;
            write(teavm_pipefd[1], &c, 1);
        }
    #else
        void teavm_edt_waitFor(int64_t timeout) {
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

        void teavm_edt_interrupt() {
            struct itimerspec its = {0};
            timer_settime(teavm_queueTimer, 0, &its, NULL);
            raise(SIGRTMIN);
        }
    #endif
#endif

#if TEAVM_WINDOWS
    void teavm_edt_waitFor(int64_t timeout) {
        WaitForSingleObject(teavm_queueTimer, (DWORD) timeout);
        ResetEvent(teavm_queueTimer);
    }

    void teavm_edt_interrupt() {
        SetEvent(teavm_queueTimer);
    }
#endif

#if TEAVM_PSP
    void teavm_edt_waitFor(int64_t timeout) {
        // PSP implementation: simple delay
        if (timeout > 0) {
            sceKernelDelayThread(timeout * 1000); // timeout in ms, sceKernelDelayThread in us
        }
    }

    void teavm_edt_interrupt() {
        // Stub for PSP
    }
#endif
