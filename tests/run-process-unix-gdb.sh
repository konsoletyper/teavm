#!/bin/bash
export LC_ALL=C
gdb -batch \
  -ex 'set debuginfod enabled off' \
  -ex 'set startup-with-shell off' \
  -ex 'set verbose off' \
  -ex 'set complaints 0' \
  -ex run \
  -ex bt \
  -ex 'quit $_exitcode' \
  --args "$@"
GDB_STATUS=$?
exit $GDB_STATUS
