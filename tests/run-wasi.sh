mkdir -p build/wasi-testdir
~/.wasmtime/bin/wasmtime run --dir build/wasi-testdir::/ $1 $2