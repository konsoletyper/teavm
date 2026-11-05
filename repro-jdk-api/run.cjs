let passed = false;
const timeout = setTimeout(() => {
    console.error("JDK API contract timed out");
    process.exit(2);
}, 5000);

const originalInfo = console.info;
console.info = (...args) => {
    const line = args.join(" ");
    originalInfo(...args);
    if (line === "JDK API contract passed") passed = true;
};

try {
    const runtime = require("./build/generated/teavm/js/teavm-jdk-api-repro.js");
    if (typeof runtime.main !== "function") {
        throw new Error("TeaVM contract entry point was not generated");
    }

    runtime.main([], error => {
        setTimeout(() => {
            clearTimeout(timeout);
            if (error) {
                console.error("JDK API contract failed:", error);
                process.exit(1);
            }
            if (!passed) {
                console.error("JDK API contract ended without reaching its success marker");
                process.exit(1);
            }
            process.exit(0);
        }, 100);
    });
} catch (error) {
    clearTimeout(timeout);
    console.error("JDK API contract failed:", error);
    process.exit(1);
}
