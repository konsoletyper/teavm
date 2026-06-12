export LC_ALL=C.UTF-8
SOURCE_DIR=$(pwd)
clang -std=c11 -g -iquote . -I custom-include -o run_test -D_DARWIN_C_SOURCE all.c
