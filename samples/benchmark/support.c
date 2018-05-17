static int64_t currentTimeNano() {
    struct timespec time;
    clock_gettime(CLOCK_REALTIME, &time);
    return (int64_t) time.tv_nsec;
} 
