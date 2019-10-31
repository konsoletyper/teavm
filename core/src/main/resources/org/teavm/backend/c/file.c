#include "definitions.h"
#include "file.h"
#include "string.h"
#include "time.h"
#include "date.h"
#include <stdlib.h>
#include <errno.h>

#if TEAVM_UNIX
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdio.h>
#include <dirent.h>
#include <utime.h>
#include <pwd.h>
#include <string.h>

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

int32_t teavm_file_tempDirectory(char16_t** result) {
  static const char16_t string[] = u"/tmp";
  char16_t* copy = malloc(sizeof(string));
  *result = copy;
  int32_t i = 0;
  while (string[i] != 0) {
    copy[i] = string[i];
    i++;
  }
  return i;
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

int32_t teavm_file_isWindows() {
    return 0;
}

int32_t teavm_file_canonicalize(char16_t* path, int32_t pathSize, char16_t** result) {
    return 0;
}

#endif

#if TEAVM_WINDOWS
#include <Windows.h>

static int32_t teavm_readEnv(char16_t** result, WCHAR const * name) {
    DWORD size = GetEnvironmentVariableW(name, 0, 0);
    char16_t *javaChars = size ? malloc(sizeof(char16_t) * size) : 0;
    *result = javaChars;
    return size ? GetEnvironmentVariableW(name, javaChars, size) : 0;
}

int32_t teavm_file_homeDirectory(char16_t** result) {
    return teavm_readEnv(result, L"USERPROFILE");
}

int32_t teavm_file_tempDirectory(char16_t** result) {
    return teavm_readEnv(result, L"TMP");
}

int32_t teavm_file_workDirectory(char16_t** result) {
    DWORD size = GetCurrentDirectoryW(0, 0);
    char16_t *javaChars = malloc(sizeof(char16_t) * size);
    *result = javaChars;
    return GetCurrentDirectoryW(size, javaChars);
}

static WCHAR* teavm_file_convertPath(char16_t* string, int32_t size) {
    WCHAR *copy = malloc(sizeof(WCHAR) * (size + 1));
    for (size_t i = 0; i != size; i++) {
        char16_t c = string[i];
        copy[i] = c == '/' ? '\\' : c;
    }
    copy[size] = 0;
    return copy;
}

static DWORD teavm_file_getAttributes(char16_t* name, int32_t nameSize) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    int attributes = GetFileAttributesW(nativeName);
    free(nativeName);
    return attributes;
}

int32_t teavm_file_isFile(char16_t* name, int32_t nameSize) {
    DWORD attributes = teavm_file_getAttributes(name, nameSize);
    return attributes != INVALID_FILE_ATTRIBUTES && !(attributes & FILE_ATTRIBUTE_DIRECTORY);
}

int32_t teavm_file_isDir(char16_t* name, int32_t nameSize) {
    DWORD attributes = teavm_file_getAttributes(name, nameSize);
    return attributes != INVALID_FILE_ATTRIBUTES && (attributes & FILE_ATTRIBUTE_DIRECTORY);
}

static int32_t teavm_file_checkExistingFileAccess(char16_t* name, int32_t nameSize, DWORD desiredAccess) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    #if TEAVM_WINDOWS_UWP
        HANDLE fileHandle = CreateFile2(nativeName, desiredAccess, FILE_SHARE_READ, OPEN_EXISTING, NULL);
    #else
        HANDLE fileHandle = CreateFileW(nativeName, desiredAccess, FILE_SHARE_READ, 0, OPEN_EXISTING,
                FILE_ATTRIBUTE_NORMAL, 0);
    #endif
    int32_t result = fileHandle != INVALID_HANDLE_VALUE;
    if (fileHandle != INVALID_HANDLE_VALUE) {
        CloseHandle(fileHandle);
    }
    return result;
}

int32_t teavm_file_canRead(char16_t* name, int32_t nameSize) {
    return teavm_file_checkExistingFileAccess(name, nameSize, GENERIC_READ);
}

int32_t teavm_file_canWrite(char16_t* name, int32_t nameSize) {
    return teavm_file_checkExistingFileAccess(name, nameSize, GENERIC_WRITE);
}

int32_t teavm_file_createDirectory(char16_t* name, int32_t nameSize) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    int32_t result = CreateDirectoryW(nativeName, NULL);
    free(nativeName);
    return result;
}

int32_t teavm_file_createFile(char16_t* name, int32_t nameSize) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    #if TEAVM_WINDOWS_UWP
        HANDLE fileHandle = CreateFile2(nativeName, GENERIC_WRITE, FILE_SHARE_READ, OPEN_EXISTING, NULL);
    #else
        HANDLE fileHandle = CreateFileW(nativeName, GENERIC_WRITE, 0, 0, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, 0);
    #endif
    int32_t result = 2;
    free(nativeName);

    if (fileHandle != INVALID_HANDLE_VALUE) {
        result = GetLastError() == ERROR_ALREADY_EXISTS ? 1 : 0;
        CloseHandle(fileHandle);
    }
    return result;
}

int32_t teavm_file_delete(char16_t* name, int32_t nameSize) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    int attributes = GetFileAttributesW(nativeName);
    int32_t result;
    if (attributes != INVALID_FILE_ATTRIBUTES && (attributes & FILE_ATTRIBUTE_DIRECTORY)) {
        result = RemoveDirectoryW(nativeName);
    } else {
        result = DeleteFileW(nativeName);
    }
    free(nativeName);
    return result;
}

int32_t teavm_file_rename(char16_t* name, int32_t nameSize, char16_t* newName, int32_t newNameSize) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    WCHAR* nativeNewName = teavm_file_convertPath(newName, newNameSize);
    int32_t result = MoveFileExW(nativeName, nativeNewName, 0);
    free(nativeName);
    free(nativeNewName);
    return result;
}

int32_t teavm_file_setReadonly(char16_t* name, int32_t nameSize, int32_t readonly) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    int attributes = GetFileAttributesW(nativeName);
    if (attributes == INVALID_FILE_ATTRIBUTES) {
        free(nativeName);
        return 0;
    }
    if (readonly) {
        attributes |= FILE_ATTRIBUTE_READONLY;
    } else {
        attributes &= ~FILE_ATTRIBUTE_READONLY;
    }
    BOOL status = SetFileAttributesW(nativeName, attributes);
    free(nativeName);
    return status;
}

int64_t teavm_file_lastModified(char16_t* name, int32_t nameSize) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    FILETIME modified;
    #if TEAVM_WINDOWS_UWP
        HANDLE fileHandle = CreateFile2(nativeName, GENERIC_READ, FILE_SHARE_READ, OPEN_EXISTING, NULL);
    #else
        HANDLE fileHandle = CreateFileW(nativeName, GENERIC_READ, 0, 0, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, 0);
    #endif
	free(nativeName);
	if (fileHandle == INVALID_HANDLE_VALUE) {
		return 0;
	}

    BOOL status = GetFileTime(fileHandle, NULL, NULL, &modified);
	CloseHandle(fileHandle);

    if (!status) {
        return 0;
    }
    int64_t t = modified.dwLowDateTime | ((uint64_t) modified.dwHighDateTime << 32);
    return (t - teavm_unixTimeOffset) / 10000;
}

int32_t teavm_file_setLastModified(char16_t* name, int32_t nameSize, int64_t lastModified) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);

    #if TEAVM_WINDOWS_UWP
        HANDLE fileHandle = CreateFile2(nativeName, GENERIC_WRITE, FILE_SHARE_READ, OPEN_EXISTING, NULL);
    #else
        HANDLE fileHandle = CreateFileW(nativeName, GENERIC_WRITE, 0, 0, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, 0);
    #endif
	free(nativeName);
	if (fileHandle == INVALID_HANDLE_VALUE) {
		return 0;
	}

	FILETIME modified;
    int64_t t = lastModified * 10000 + teavm_unixTimeOffset;

    modified.dwLowDateTime = (DWORD) (t & 0xFFFFFFFF);
    modified.dwHighDateTime = (DWORD) ((t >> 32) & 0xFFFFFFFF);
    BOOL status = SetFileTime(fileHandle, NULL, NULL, &modified);
	CloseHandle(fileHandle);
    return status;
}

int32_t teavm_file_length(char16_t* name, int32_t nameSize) {
    WIN32_FILE_ATTRIBUTE_DATA fileAttributeData;
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    int attributes = GetFileAttributesExW(nativeName, GetFileExInfoStandard, &fileAttributeData);
    free(nativeName);
    return fileAttributeData.nFileSizeLow;
}

int64_t teavm_file_open(char16_t* name, int32_t nameSize, int32_t mode) {
    int32_t readable = (mode & 1) != 0;
    int32_t writable = (mode & 2) != 0;
    int32_t append = (mode & 4) != 0;

    DWORD creationDisposition = writable ? OPEN_ALWAYS : OPEN_EXISTING;
    DWORD desiredAccess = (readable ? GENERIC_READ : 0) | (writable ? GENERIC_WRITE : 0);

    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    #if TEAVM_WINDOWS_UWP
        HANDLE fileHandle = CreateFile2(nativeName, desiredAccess, FILE_SHARE_READ, creationDisposition, NULL);
    #else
        HANDLE fileHandle = CreateFileW(nativeName, desiredAccess, 0, 0, creationDisposition,
         FILE_ATTRIBUTE_NORMAL, 0);
    #endif
    free(nativeName);

	if (fileHandle == INVALID_HANDLE_VALUE) {
		return 0;
	}

	if (writable) {
		if (append) {
			SetFilePointer(fileHandle, 0, 0, FILE_END);
		} else {
			SetFilePointer(fileHandle, 0, 0, FILE_BEGIN);
			SetEndOfFile(fileHandle);
		}
	}

    return (int64_t) fileHandle;
}

int32_t teavm_file_close(int64_t file) {
    return file ? CloseHandle((HANDLE) file) : 0;
}

int32_t teavm_file_flush(int64_t file) {
    return FlushFileBuffers((HANDLE) file);
}

int32_t teavm_file_seek(int64_t file, int32_t where, int32_t offset) {
  return SetFilePointer((HANDLE) file, offset, 0, where) != INVALID_SET_FILE_POINTER;
}

int32_t teavm_file_tell(int64_t file) {
  return SetFilePointer((HANDLE) file, 0, 0, 1);
}

int32_t teavm_file_read(int64_t file, int8_t* data, int32_t offset, int32_t size) {
  DWORD numRead = 0;
  DWORD result = ReadFile((HANDLE) file, data + offset, size, &numRead, 0);
  return result ? numRead : 0;
}

int32_t teavm_file_write(int64_t file, int8_t* data, int32_t offset, int32_t size) {
  DWORD numWritten = 0;
  DWORD result = WriteFile((HANDLE) file, data + offset, size, &numWritten, 0);
  return result ? numWritten : 0;
}

int32_t teavm_file_isWindows() {
    return 1;
}

static TeaVM_StringList* teavm_file_addToList(TeaVM_StringList* strings, char16_t* data) {
    int32_t size = (int32_t) wcslen(data);
    WCHAR* copy = malloc(size * sizeof(char16_t));
    memcpy(copy, data, size * sizeof(char16_t));
    return teavm_appendString(strings, copy, size);
}

TeaVM_StringList* teavm_file_listFiles(char16_t* name, int32_t nameSize) {
    WCHAR* nativeName = teavm_file_convertPath(name, nameSize);
    WCHAR* pattern = malloc((nameSize + 3) * sizeof(WCHAR));
    memcpy(pattern, nativeName, nameSize * sizeof(WCHAR));
    free(nativeName);
    pattern[nameSize] = '\\';
    pattern[nameSize + 1] = '*';
    pattern[nameSize + 2] = 0;

    WIN32_FIND_DATAW fileData;
    HANDLE handle = FindFirstFileW(pattern, &fileData);
    free(pattern);
    if (handle == INVALID_HANDLE_VALUE) {
        if (GetLastError() == ERROR_FILE_NOT_FOUND) {
            return teavm_appendString(NULL, NULL, 0);
        }
        return NULL;
    }

    TeaVM_StringList *strings = teavm_appendString(NULL, NULL, 0);
    strings = teavm_file_addToList(strings, fileData.cFileName);

    while (1) {
        BOOL success = FindNextFileW(handle, &fileData);
        if (!success) {
             if (GetLastError() == ERROR_NO_MORE_FILES) {
                break;
            } else {
                teavm_disposeStringList(strings);
                return NULL;
            }
        }

        strings = teavm_file_addToList(strings, fileData.cFileName);
    }

    FindClose(handle);
    return strings;
}

int32_t teavm_file_canonicalize(char16_t* path, int32_t pathSize, char16_t** result) {
    WCHAR* nativeName = teavm_file_convertPath(path, pathSize);
    WCHAR buffer[256];
    WCHAR* longBuffer;
    DWORD actualSize = GetLongPathNameW(nativeName, buffer, 256);
    longBuffer = malloc(sizeof(WCHAR) * actualSize);
    if (actualSize == 0) {
        free(nativeName);
        return -1;
    }
    if (actualSize >= 256) {
        actualSize = GetLongPathNameW(nativeName, longBuffer, actualSize);
    } else {
        memcpy(longBuffer, buffer, actualSize * 2);
    }
    free(nativeName);
    *result = longBuffer;
    return actualSize;
}

#endif
