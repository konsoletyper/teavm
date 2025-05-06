export LC_ALL=C
SOURCE_DIR=$(pwd)
gcc -g -O0 -lrt all.c -I custom-include -o run_test -lm
