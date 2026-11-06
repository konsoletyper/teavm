const mode = process.argv[2] || "instrumented";
let calls = 0;
let serial = 0;
let passed = false;
const timeout = setTimeout(() => {
    console.error("Secure random contract timed out");
    process.exit(2);
}, 10000);
if (mode === "unavailable") {
    delete globalThis.crypto;
} else if (mode === "instrumented") {
    Object.defineProperty(globalThis, "crypto", { configurable: true, value: {
        getRandomValues(view) {
            if (view.byteLength > 65536) throw new Error("Web Crypto quota exceeded");
            ++calls;
            for (let i = 0; i < view.length; ++i) view[i] = ++serial & 255;
            return view;
        }
    }});
} else {
    Object.defineProperty(globalThis, "crypto", {
        configurable: true, value: require("node:crypto").webcrypto
    });
}
const marker = mode === "unavailable" ? "Secure random unavailable contract passed" : "Secure random contract passed";
const originalInfo = console.info;
console.info = (...args) => {
    originalInfo(...args);
    if (args.join(" ") === marker) passed = true;
};
try {
    const runtime = require("./build/generated/teavm/js/teavm-secure-random-repro.js");
    runtime.main(mode === "unavailable" ? ["unavailable"] : [], error => {
        clearTimeout(timeout);
        if (error || !passed || mode === "instrumented" && calls < 9) {
            console.error(error || `Secure random contract failed: passed=${passed}, native calls=${calls}`);
            process.exitCode = 1;
        }
    });
} catch (error) {
    clearTimeout(timeout);
    console.error(error);
    process.exitCode = 1;
}
