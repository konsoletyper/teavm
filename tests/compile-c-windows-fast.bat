@echo off
setlocal enabledelayedexpansion

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

call "%VCVARSALL%" >nul
if errorlevel 1 (
    echo error: failed to initialize the Visual Studio developer environment 1>&2
    exit /b 1
)

cl /nologo /std:c11 /utf-8 /O0 /I custom-include all.c /Fe:run_test.exe /link
