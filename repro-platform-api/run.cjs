let passed = false;
const timeout = setTimeout(() => {
    console.error("Platform API contract timed out");
    process.exit(2);
}, 5000);
const originalInfo = console.info;
console.info = (...args) => {
    originalInfo(...args);
    if (args.join(" ") === "Platform API contract passed") passed = true;
};
try {
    const runtime = require("./build/generated/teavm/js/teavm-platform-api-repro.js");
    runtime.main([], error => {
        clearTimeout(timeout);
        if (error || !passed) {
            console.error(error || "Platform API contract did not reach success marker");
            process.exitCode = 1;
        }
    });
} catch (error) {
    clearTimeout(timeout);
    console.error(error);
    process.exitCode = 1;
}
