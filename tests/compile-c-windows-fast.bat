@echo off
cl /nologo /std:c11 /utf-8 /Od /I custom-include all.c /Fe:run_test.exe /link
