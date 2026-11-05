let passed = false;
const timeout = setTimeout(() => {
    console.error("reproducer timed out");
    process.exit(2);
}, 5000);

const originalInfo = console.info;
console.info = (...args) => {
    const line = args.join(" ");
    originalInfo(...args);
    if (line === "executor=ok,park=ok") passed = true;
};

const app = require("./build/generated/teavm/js/teavm-unciv-executor-repro.js");

app.main([], error => {
    setTimeout(() => {
        clearTimeout(timeout);
        if (error || !passed) {
            console.error(error || "reproducer ended without reaching its success marker");
            process.exit(1);
        }
        process.exit(0);
    }, 100);
});
