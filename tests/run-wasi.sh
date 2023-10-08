mkdir -p build/wasi-testdir
~/.wasmtime/bin/wasmtime run --mapdir /::build/wasi-testdir $1 $2