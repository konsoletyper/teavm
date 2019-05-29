#include "runtime.h"
#include <stdlib.h>
#include <errno.h>

#ifdef __GNUC__
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdio.h>
#include <dirent.h>
#include <utime.h>
#include <pwd.h>

int32_t teavm_file_homeDirectory(char16_t** result) {
    struct passwd *pw = getpwuid(getuid());
    int32_t length;
    *result = teavm_mbToChar16(pw->pw_dir, &length);
    return length;
}

int32_t teavm_file_workDirectory(char16_t** result) {
    long pathMax;
    size_t size;
    char *buf;
    int32_t length;

    pathMax = pathconf(".", _PC_PATH_MAX);
    if (pathMax == -1) {
        size = 1024;
    } else if (pathMax > 10240) {
        size = 10240;
    } else {
        size = pathMax;
    }

    buf = malloc(size);

    while (1) {
        if (getcwd(buf, size) != buf) {
            if (errno != ERANGE) {
                *result = teavm_mbToChar16(strerror(errno), &length);
                return -1 - length;
            }
        } else {
            *result = teavm_mbToChar16(buf, &length);
            free(buf);
            return length;
        }
        size *= 2;
        char* newBuf = realloc(buf, size);
        if (newBuf == NULL) {
            *result = teavm_mbToChar16("Out of memory", &length);
            free(buf);
            return -1 - length;
        }
        buf = newBuf;
    }
}

int32_t teavm_file_isFile(char16_t* name, int32_t nameSize) {
    struct stat s;
    char* mbName = teavm_char16ToMb(name, nameSize);
    int statResult = stat(mbName, &s);
    free(mbName);
    if (statResult != 0) {
        return 0;
    }

    return !S_ISDIR(s.st_mode);
}

int32_t teavm_file_isDir(char16_t* name, int32_t nameSize) {
    struct stat s;
    char* mbName = teavm_char16ToMb(name, nameSize);
    int statResult = stat(mbName, &s);
    free(mbName);
    if (statResult != 0) {
        return 0;
    }

    return S_ISDIR(s.st_mode);
}

int32_t teavm_file_canRead(char16_t* name, int32_t nameSize) {
    char* mbName = teavm_char16ToMb(name, nameSize);
    int result = access(mbName, R_OK);
    free(mbName);
    return result == 0;
}

int32_t teavm_file_canWrite(char16_t* name, int32_t nameSize) {
    char* mbName = teavm_char16ToMb(name, nameSize);
    int result = access(mbName, W_OK);
    free(mbName);
    return result == 0;
}

int32_t teavm_file_setReadonly(char16_t* name, int32_t nameSize, int32_t readonly) {
    struct stat s;
    char* mbName = teavm_char16ToMb(name, nameSize);
    int statResult = stat(mbName, &s);
    if (statResult != 0) {
        free(mbName);
        return 0;
    }

    mode_t mode = s.st_mode;
    if (readonly) {
        mode &= ~S_IWUSR;
    } else {
        mode |= S_IWUSR;
    }
    int result = chmod(mbName, mode);
    free(mbName);
    return result == 0;
}

TeaVM_StringList* teavm_file_listFiles(char16_t* name, int32_t nameSize) {
    char* mbName = teavm_char16ToMb(name, nameSize);
    DIR* dir = opendir(mbName);
    free(mbName);
    if (dir == NULL) {
        return NULL;
    }

    TeaVM_StringList *strings = teavm_appendString(NULL, NULL, 0);

    while (1) {
        struct dirent* entry = readdir(dir);
        if (entry == NULL) {
            break;
        }

        int32_t len;
        char16_t* name = teavm_mbToChar16(entry->d_name, &len);
        if (name == NULL) {
            teavm_disposeStringList(strings);
            return NULL;
        }

        strings = teavm_appendString(strings, name, len);
    }

    closedir(dir);
    return strings;
}

int32_t teavm_file_createDirectory(char16_t* name, int32_t nameSize) {
    char* mbName = teavm_char16ToMb(name, nameSize);
    int result = mkdir(mbName, S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH);
    free(mbName);
    return result == 0;
}

int32_t teavm_file_createFile(char16_t* name, int32_t nameSize) {
    char* mbName = teavm_char16ToMb(name, nameSize);
    int result = open(mbName, O_RDWR | O_CREAT | O_EXCL, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    free(mbName);
    if (result >= 0) {
        close(result);
        return 0;
    } else if (errno == EEXIST) {
        return 1;
    } else {
        return 2;
    }
}

int32_t teavm_file_delete(char16_t* name, int32_t nameSize) {
    char* mbName = teavm_char16ToMb(name, nameSize);
    int result = remove(mbName);
    free(mbName);
    return result == 0;
}

int32_t teavm_file_rename(char16_t* name, int32_t nameSize, char16_t* newName, int32_t newNameSize) {
    char* mbName = teavm_char16ToMb(name, nameSize);
    char* newMbName = teavm_char16ToMb(newName, newNameSize);
    int result = rename(mbName, newMbName);
    free(mbName);
    free(newMbName);
    return result == 0;
}

int64_t teavm_file_lastModified(char16_t* name, int32_t nameSize) {
    struct stat s;
    char* mbName = teavm_char16ToMb(name, nameSize);
    int statResult = stat(mbName, &s);
    free(mbName);
    if (statResult != 0) {
        return 0;
    }

    return teavm_date_timeToTimestamp(s.st_mtime);
}

int32_t teavm_file_setLastModified(char16_t* name, int32_t nameSize, int64_t lastModified) {
    struct stat s;
    char* mbName = teavm_char16ToMb(name, nameSize);
    int statResult = stat(mbName, &s);
    if (statResult != 0) {
        free(mbName);
        return 0;
    }

    struct utimbuf newTime;
    newTime.actime = s.st_atime;
    newTime.modtime = teavm_date_timestampToTime(lastModified);

    return utime(mbName, &newTime) == 0;
}

int32_t teavm_file_length(char16_t* name, int32_t nameSize) {
    char* mbName = teavm_char16ToMb(name, nameSize);
    FILE* file = fopen(mbName, "r");
    free(mbName);

    if (file == NULL) {
        return 0;
    }

    if (fseek(file, 0, SEEK_END) != 0) {
        fclose(file);
        return 0;
    }

    long result = ftell(file);
    fclose(file);
    if (result < 0) {
        return 0;
    }
    return (int32_t) result;
}

int64_t teavm_file_open(char16_t* name, int32_t nameSize, int32_t mode) {
    char* mbName = teavm_char16ToMb(name, nameSize);
    char* modeString;
    switch (mode) {
        case 1:
            modeString = "r";
            break;
        case 2:
            modeString = "w";
            break;
        case 3:
            modeString = "w+";
            break;
        case 6:
            modeString = "a";
            break;
        case 7:
            modeString = "a+";
            break;
        default:
            modeString = "";
            break;
    }
    FILE* file = fopen(mbName, modeString);
    if (file == NULL) {
        return 0;
    }

    return (int64_t) (intptr_t) file;
}

int32_t teavm_file_close(int64_t file) {
    FILE* handle = (FILE*) file;
    return fclose(handle) == 0;
}

int32_t teavm_file_flush(int64_t file) {
    FILE* handle = (FILE*) file;
    return fflush(handle) == 0;
}

int32_t teavm_file_seek(int64_t file, int32_t where, int32_t offset) {
    FILE* handle = (FILE*) file;
    int whence;
    switch (where) {
        case 0:
            whence = SEEK_SET;
            break;
        case 1:
            whence = SEEK_CUR;
            break;
        case 2:
            whence = SEEK_END;
            break;
    }
    return fseek(handle, offset, whence) == 0;
}

int32_t teavm_file_tell(int64_t file) {
    FILE* handle = (FILE*) file;
    return (int32_t) ftell(handle);
}

int32_t teavm_file_read(int64_t file, int8_t* data, int32_t offset, int32_t size) {
    FILE* handle = (FILE*) file;
    return (int32_t) fread(data + offset, 1, size, handle);
}

int32_t teavm_file_write(int64_t file, int8_t* data, int32_t offset, int32_t size) {
    FILE* handle = (FILE*) file;
    return (int32_t) fwrite(data + offset, 1, size, handle);
}

#endif