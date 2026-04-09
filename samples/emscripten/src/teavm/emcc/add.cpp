#include <stdint.h>

extern "C" {
    void addInBuffer(int32_t* args) {
        args[2] = args[0] + args[1];
    }
}