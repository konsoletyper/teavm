let passed = false;
const timeout = setTimeout(() => {
    console.error("SHA-1 contract timed out");
    process.exit(2);
}, 15000);

const originalInfo = console.info;
console.info = (...args) => {
    const line = args.join(" ");
    originalInfo(...args);
    if (line === "SHA-1 contract passed") passed = true;
};

try {
    const runtime = require("./build/generated/teavm/js/teavm-sha1-repro.js");
    runtime.main([], error => {
        setTimeout(() => {
            clearTimeout(timeout);
            if (error) {
                console.error("SHA-1 contract failed:", error);
                process.exit(1);
            }
            if (!passed) {
                console.error("SHA-1 contract ended without reaching its success marker");
                process.exit(1);
            }
            process.exit(0);
        }, 100);
    });
} catch (error) {
    clearTimeout(timeout);
    console.error("SHA-1 contract failed:", error);
    process.exit(1);
}
