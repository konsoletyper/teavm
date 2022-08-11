# A pure javascript shim for WASI

Implementation status: A subset of wasi_snapshot_preview1 is implemented. The rest either throws an exception, returns an error or is incorrectly implemented.

## Usage

```javascript
import WASI from "./browser_wasi_shim/src/wasi.js";
import { File } from "./browser_wasi_shim/src/fs_core.js";

let args = ["bin", "arg1", "arg2"];
let env = ["FOO=bar"];
let fds = [
    new File([]), // stdin
    new File([]), // stdout
    new File([]), // stderr
    new PreopenDirectory(".", {
        "example.c": new File(new TextEncoder("utf-8").encode(`#include "a"`)),
        "hello.rs": new File(new TextEncoder("utf-8").encode(`fn main() { println!("Hello World!"); }`)),
    }),
];
let wasi = new WASI(args, env, fds);

let wasm = await WebAssembly.compileStreaming(fetch("bin.wasm"));
let inst = await WebAssembly.instantiate(wasm, {
    "wasi_snapshot_preview1": wasi.wasiImport,
});  
wasi.start(inst);
```

## License

Licensed under either of

  * Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or
    http://www.apache.org/licenses/LICENSE-2.0)
  * MIT license ([LICENSE-MIT](LICENSE-MIT) or
    http://opensource.org/licenses/MIT)

at your option.

### Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in the work by you shall be dual licensed as above, without any
additional terms or conditions.
