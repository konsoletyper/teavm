#!/bin/bash

set -Eeuo pipefail

runtime=${1:-wasmtime}

wasm=target/generated/wasm/teavm-wasm/classes.wasm

function expect_eq {
  if [ "$1" != "$2" ]; then
    echo "FAIL: expected \"$1\", got \"$2\" at line ${BASH_LINENO[0]}" >&2
    exit 1
  fi
}

function expect_almost_eq {
  local diff=$(($1 - $2))
  if (( $diff < -10000 || $diff > 10000 )); then
    echo "FAIL: expected diff between -10000 and 10000, got $diff at line ${BASH_LINENO[0]}" >&2
    exit 1
  fi
}

function expect_is_dir {
  if [ ! -d "$1" ]; then
    echo "FAIL: expected \"$1\" to be a directory at line ${BASH_LINENO[0]}" >&2
    exit 1
  fi
}

function expect_is_file {
  if [ ! -f "$1" ]; then
    echo "FAIL: expected \"$1\" to be a file at line ${BASH_LINENO[0]}" >&2
    exit 1
  fi
}

function expect_nonexistence {
  if [ -e "$1" ]; then
    echo "FAIL: expected \"$1\" to not exist at line ${BASH_LINENO[0]}" >&2
    exit 1
  fi
}

expect_eq "foo bar baz" "$($runtime $wasm echo-args foo bar baz)"

for which in floats doubles; do
  expect_eq "false:true:false" "$($runtime $wasm $which 1 2)"
  expect_eq "false:false:true" "$($runtime $wasm $which 1 0)"
  expect_eq "false:false:true" "$($runtime $wasm $which -1 0)"
  expect_eq "true:false:false" "$($runtime $wasm $which 0 0)"
done

expect_eq 42713 "$($runtime --env foo=42 --env bar=713 $wasm env foo bar)"

expect_eq hello "$($runtime $wasm catch hello)"

expect_almost_eq \
  $(printf "%0.f" "$(bc <<<"$(date +"%s.%N")*1000")") \
  "$($runtime $wasm epoch)"

expect_eq "foo bar baz" "$($runtime $wasm stdin <<<"foo bar baz")"

rm -rf foo bar
mkdir foo
mkdir bar
$runtime --dir bar --dir foo $wasm mkdirs foo/bar/baz
expect_is_dir foo/bar/baz
rm -r foo/bar

$runtime --dir bar --dir foo $wasm create foo/bar.txt
expect_is_file foo/bar.txt

$runtime --dir bar --dir foo $wasm create_already_exists foo/bar.txt
expect_is_file foo/bar.txt

$runtime --dir foo $wasm write foo/bar.txt hola
expect_eq hola "$(cat foo/bar.txt)"

expect_eq hola "$($runtime --dir foo $wasm read foo/bar.txt)"

expect_eq la "$($runtime --dir foo $wasm seek foo/bar.txt 2)"

$runtime --dir foo $wasm resize foo/bar.txt 1
expect_eq h "$(cat foo/bar.txt)"

$runtime --dir foo $wasm rename foo/bar.txt foo/um.txt
expect_nonexistence foo/bar.txt
expect_eq h "$(cat foo/um.txt)"

$runtime --dir foo $wasm delete foo/um.txt
expect_nonexistence foo/um.txt

touch foo/a.txt foo/b.txt foo/c.txt
expect_eq "a.txt b.txt c.txt" "$($runtime --dir foo $wasm list foo)"

$runtime --dir foo $wasm mtime foo/a.txt 274853014000
expect_eq 274853014 "$(stat -c %Y foo/a.txt)"

expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-mkdirs bar/baz/buzz)"
expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-create bar/baz.txt)"
expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-write bar/baz.txt)"

for path in foo/does-not-exist.txt bar/baz.txt; do
  expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-read $path)"
  expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-random-access $path)"
  expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-length $path)"
  expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-rename $path foo/wow.txt)"
  expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-rename foo/wow.txt $path)"
  expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-delete $path)"
  expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-list $path)"
  expect_eq "SUCCESS" "$($runtime --dir foo $wasm bad-mtime $path)"
done

rm -r foo bar

echo "success!"
