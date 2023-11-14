export LC_ALL=C
SOURCE_DIR=$(pwd)
gcc -g -O0 -lrt all.c -o run_test -lm
