#!/bin/bash
echo Before running test
export LC_ALL=C.UTF-8
SCRIPT_DIR=$(dirname "$0")
echo Before running gdb
gdb \
  --batch \
  -ex 'set verbose off' \
  -ex 'set complaints 0' \
  -ex run \
  -x "$SCRIPT_DIR/run-process-unix-gdb-commands.gdb" \
  --args env TEAVM_TEST_EXCEPTION_FILE="$TEAVM_TEST_EXCEPTION_FILE" "$@"
GDB_STATUS=$?
echo gdb complete with status $GDB_STATUS
exit $GDB_STATUS
