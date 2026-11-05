let passed = false;
const timeout = setTimeout(() => {
    console.error("Concurrency contract timed out");
    process.exit(2);
}, 15000);

const originalInfo = console.info;
console.info = (...args) => {
    const line = args.join(" ");
    originalInfo(...args);
    if (line === "Concurrency contract passed") passed = true;
};

try {
    const runtime = require("./build/generated/teavm/js/teavm-concurrency-compatibility-repro.js");
    if (typeof runtime.main !== "function") {
        throw new Error("TeaVM contract entry point was not generated");
    }

    runtime.main([], error => {
        setTimeout(() => {
            clearTimeout(timeout);
            if (error) {
                console.error("Concurrency contract failed:", error);
                process.exit(1);
            }
            if (!passed) {
                console.error("Concurrency contract ended without reaching its success marker");
                process.exit(1);
            }
            process.exit(0);
        }, 100);
    });
} catch (error) {
    clearTimeout(timeout);
    console.error("Concurrency contract failed:", error);
    process.exit(1);
}
