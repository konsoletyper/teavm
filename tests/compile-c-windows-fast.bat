@echo off
cl /nologo /std:c11 /utf-8 /O0 /I custom-include all.c /Fe:run_test.exe /link
