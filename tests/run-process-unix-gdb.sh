#!/bin/bash
export LC_ALL=C.UTF-8
SCRIPT_DIR=$(dirname "$0")
echo About to run gdb
gdb \
  #--batch-silent \
  #-ex 'set debuginfod enabled off' \
  #-ex 'set startup-with-shell off' \
  #-ex 'set verbose off' \
  #-ex 'set complaints 0' \
  -ex run \
  -x "$SCRIPT_DIR/run-process-unix-gdb-commands.gdb" \
  --args "$@"
echo gdb status: $GDB_STATUS
GDB_STATUS=$?
exit $GDB_STATUS
