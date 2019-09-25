#pragma once

#include <stdint.h>
#include <uchar.h>

#define TEAVM_FIELD_TYPE_OBJECT 0
#define TEAVM_FIELD_TYPE_ARRAY 1
#define TEAVM_FIELD_TYPE_BOOLEAN 2
#define TEAVM_FIELD_TYPE_BYTE 3
#define TEAVM_FIELD_TYPE_CHAR 4
#define TEAVM_FIELD_TYPE_SHORT 5
#define TEAVM_FIELD_TYPE_INT 6
#define TEAVM_FIELD_TYPE_LONG 7
#define TEAVM_FIELD_TYPE_FLOAT 8
#define TEAVM_FIELD_TYPE_DOUBLE 9

typedef struct {
    uint16_t offset;
    uint8_t type;
    char16_t* name;
} TeaVM_FieldDescriptor;

typedef struct {
    uint32_t count;
    TeaVM_FieldDescriptor data[65536];
} TeaVM_FieldDescriptors;

typedef struct {
    unsigned char* offset;
    uint8_t type;
    char16_t* name;
} TeaVM_StaticFieldDescriptor;

typedef struct {
    uint32_t count;
    TeaVM_StaticFieldDescriptor data[65536];
} TeaVM_StaticFieldDescriptors;

extern void teavm_gc_writeHeapDump();