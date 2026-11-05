let passed = false;
const timeout = setTimeout(() => {
    console.error("scheduled executor reproducer timed out");
    process.exit(2);
}, 15000);

const originalInfo = console.info;
console.info = (...args) => {
    const line = args.join(" ");
    originalInfo(...args);
    if (line === "scheduled-executor=ok") passed = true;
};

try {
    const app = require("./build/generated/teavm/js/teavm-unciv-scheduler-repro.js");
    app.main([], error => {
        setTimeout(() => {
            clearTimeout(timeout);
            if (error) {
                console.error("scheduled executor reproducer failed:", error);
                process.exit(1);
            }
            if (!passed) {
                console.error("reproducer ended without reaching its success marker");
                process.exit(1);
            }
            process.exit(0);
        }, 100);
    });
} catch (error) {
    clearTimeout(timeout);
    console.error("scheduled executor reproducer failed:", error);
    process.exit(1);
}
