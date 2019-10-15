#pragma once
#include "string.h"
#include <stdint.h>
#include <uchar.h>

extern int32_t teavm_file_homeDirectory(char16_t**);
extern int32_t teavm_file_workDirectory(char16_t**);
extern int32_t teavm_file_tempDirectory(char16_t**);
extern int32_t teavm_file_isFile(char16_t*, int32_t);
extern int32_t teavm_file_isDir(char16_t*, int32_t);
extern int32_t teavm_file_canRead(char16_t*, int32_t);
extern int32_t teavm_file_canWrite(char16_t*, int32_t);
extern TeaVM_StringList* teavm_file_listFiles(char16_t*, int32_t);
extern int32_t teavm_file_createDirectory(char16_t*, int32_t);
extern int32_t teavm_file_createFile(char16_t*, int32_t);
extern int32_t teavm_file_delete(char16_t*, int32_t);
extern int32_t teavm_file_rename(char16_t*, int32_t, char16_t*, int32_t);
extern int64_t teavm_file_lastModified(char16_t*, int32_t);
extern int32_t teavm_file_setLastModified(char16_t*, int32_t, int64_t);
extern int32_t teavm_file_setReadonly(char16_t*, int32_t, int32_t);
extern int32_t teavm_file_length(char16_t*, int32_t);
extern int64_t teavm_file_open(char16_t*, int32_t, int32_t);
extern int32_t teavm_file_close(int64_t);
extern int32_t teavm_file_flush(int64_t);
extern int32_t teavm_file_seek(int64_t, int32_t, int32_t);
extern int32_t teavm_file_tell(int64_t);
extern int32_t teavm_file_read(int64_t, int8_t*, int32_t, int32_t);
extern int32_t teavm_file_write(int64_t, int8_t*, int32_t, int32_t);
extern int32_t teavm_file_isWindows();
extern int32_t teavm_file_canonicalize(char16_t*, int32_t, char16_t**);