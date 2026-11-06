@echo off
rem Locates a Visual Studio installation with the C++ build tools and initializes its developer
rem environment (PATH, INCLUDE, LIB, ...). Intentionally does NOT use setlocal: the whole point of
rem this script is for its environment changes to survive into the calling process.

set "VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"
if not exist "%VSWHERE%" set "VSWHERE=%ProgramFiles%\Microsoft Visual Studio\Installer\vswhere.exe"
if not exist "%VSWHERE%" (
    echo error: vswhere.exe not found; a Visual Studio installation with the C++ build tools is required 1>&2
    exit /b 1
)

set "VSINSTALL="
for /f "usebackq tokens=*" %%i in (`"%VSWHERE%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do (
    set "VSINSTALL=%%i"
)
if not defined VSINSTALL (
    echo error: no Visual Studio installation with the "Desktop development with C++" workload was found 1>&2
    exit /b 1
)

set "VCVARSALL=%VSINSTALL%\VC\Auxiliary\Build\vcvars64.bat"
if not exist "%VCVARSALL%" (
    echo error: vcvars64.bat not found at "%VCVARSALL%" 1>&2
    exit /b 1
)

call "%VCVARSALL%"
