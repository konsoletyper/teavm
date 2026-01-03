@echo off
set LC_ALL=C
set SOURCE_DIR=%cd%
dir /b custom-include
for /f "usebackq tokens=*" %%i in (`"C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do set VS_PATH=%%i
call "%VS_PATH%\VC\Auxiliary\Build\vcvarsall.bat" x64
cl.exe /Zi /Od all.c /I custom-include /Fe:run_test.exe /link shell32.lib
