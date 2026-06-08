#!/bin/bash
export LC_ALL=C.UTF-8
SCRIPT_DIR=$(dirname "$0")
gdb \
  --batch-silent \
  -ex 'set verbose off' \
  -ex 'set complaints 0' \
  -ex run \
  -x "$SCRIPT_DIR/run-process-unix-gdb-commands.gdb" \
  --args env TEAVM_TEST_EXCEPTION_FILE="$TEAVM_TEST_EXCEPTION_FILE" "$@"
GDB_STATUS=$?
exit $GDB_STATUS
