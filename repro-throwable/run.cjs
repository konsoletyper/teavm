let passed = false;
const timeout = setTimeout(() => {
    console.error("Throwable contract timed out");
    process.exit(2);
}, 15000);

try {
    const originalInfo = console.info;
    console.info = (...args) => {
        const line = args.join(" ");
        originalInfo(...args);
        if (line === "Throwable contract passed") passed = true;
    };

    const runtime = require("./build/generated/teavm/js/teavm-throwable-repro.js");
    if (typeof runtime.main !== "function") {
        throw new Error("TeaVM throwable contract entry point was not generated");
    }
    runtime.main([], error => {
        setTimeout(() => {
            clearTimeout(timeout);
            if (error) {
                console.error("Throwable contract failed:", error);
                process.exit(1);
            }
            if (!passed) {
                console.error("Throwable contract ended without reaching its success marker");
                process.exit(1);
            }
            process.exit(0);
        }, 100);
    });
} catch (error) {
    clearTimeout(timeout);
    console.error("Throwable contract failed:", error);
    process.exit(1);
}
