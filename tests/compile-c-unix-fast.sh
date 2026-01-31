export LC_ALL=C
SOURCE_DIR=$(pwd)
gcc -std=c11 -g -O0 -lrt all.c -I custom-include -o run_test -lm
