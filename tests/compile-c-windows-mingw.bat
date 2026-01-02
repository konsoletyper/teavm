@echo off
set LC_ALL=C
set SOURCE_DIR=%cd%
echo Using include dir: %SOURCE_DIR%\custom-include
gcc -g -O0 all.c -I custom-include -o run_test -lm -lshell32