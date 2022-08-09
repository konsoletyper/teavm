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

expect_eq "foo bar baz" "$($runtime $wasm foo bar baz)"

for which in floats doubles; do
  expect_eq "false:true:false" "$($runtime --invoke $which $wasm <<<1/2)"
  expect_eq "false:false:true" "$($runtime --invoke $which $wasm <<<1/0)"
  expect_eq "false:false:true" "$($runtime --invoke $which $wasm <<<-1/0)"
  expect_eq "true:false:false" "$($runtime --invoke $which $wasm <<<0/0)"
done

expect_eq "alifuelzb89" "$($runtime --invoke lower $wasm <<<alIFUElzB89)"
expect_eq "ALIFUELZB89" "$($runtime --invoke upper $wasm <<<alIFUElzB89)"

expect_eq 0 "$($runtime --invoke timezone $wasm <<<1660079800000)"

expect_eq 42713 "$($runtime --env foo=42 --env bar=713 --invoke env $wasm <<<foo:bar)"

expect_eq hello "$($runtime --invoke catch $wasm <<<hello)"

expect_almost_eq \
  $(printf "%0.f" "$(bc <<<"$(date +"%s.%N")*1000")") \
  "$($runtime --invoke epoch $wasm)"

expect_eq "foo bar baz" "$($runtime --invoke stdin $wasm <<<"foo bar baz")"

rm -rf foo bar
mkdir foo
mkdir bar
$runtime --dir bar --dir foo --invoke mkdirs $wasm <<<foo/bar/baz
expect_is_dir foo/bar/baz
rm -r foo/bar

$runtime --dir bar --dir foo --invoke create $wasm <<<foo/bar.txt
expect_is_file foo/bar.txt

$runtime --dir bar --dir foo --invoke create_already_exists $wasm <<<foo/bar.txt
expect_is_file foo/bar.txt

$runtime --dir foo --invoke write $wasm <<<foo/bar.txt:hola
expect_eq hola "$(cat foo/bar.txt)"

expect_eq hola "$($runtime --dir foo --invoke read $wasm <<<foo/bar.txt)"

expect_eq la "$($runtime --dir foo --invoke seek $wasm <<<foo/bar.txt:2)"

$runtime --dir foo --invoke resize $wasm <<<foo/bar.txt:1
expect_eq h "$(cat foo/bar.txt)"

$runtime --dir foo --invoke rename $wasm <<<foo/bar.txt:foo/um.txt
expect_nonexistence foo/bar.txt
expect_eq h "$(cat foo/um.txt)"

$runtime --dir foo --invoke delete $wasm <<<foo/um.txt
expect_nonexistence foo/um.txt

touch foo/a.txt foo/b.txt foo/c.txt
expect_eq "a.txt b.txt c.txt" "$($runtime --dir foo --invoke list $wasm <<<foo)"

$runtime --dir foo --invoke mtime $wasm <<<foo/a.txt:274853014000
expect_eq 274853014 "$(stat -c %Y foo/a.txt)"

expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_mkdirs $wasm <<<bar/baz/buzz)"
expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_create $wasm <<<bar/baz.txt)"
expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_write $wasm <<<bar/baz.txt)"

for path in foo/does-not-exist.txt bar/baz.txt; do
  expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_read $wasm <<<$path)"
  expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_random_access $wasm <<<$path)"
  expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_length $wasm <<<$path)"
  expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_rename $wasm <<<$path:foo/wow.txt)"
  expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_rename $wasm <<<foo/wow.txt:$path)"
  expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_delete $wasm <<<$path)"
  expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_list $wasm <<<$path)"
  expect_eq "SUCCESS" "$($runtime --dir foo --invoke bad_mtime $wasm <<<$path)"
done

rm -r foo bar

echo "success!"
