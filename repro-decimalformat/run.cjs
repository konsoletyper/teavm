let passed = false;
const failures = [];
const timeout = setTimeout(() => {
    console.error("DecimalFormat affix contract timed out");
    process.exit(2);
}, 10000);

function interceptOutput(original) {
    return (...args) => {
        const line = args.join(" ");
        if (line.startsWith("DecimalFormat affix contract failed:")) {
            failures.push(line);
            return;
        }
        if (line === "DecimalFormat affix contract passed") passed = true;
        original(...args);
    };
}
console.log = interceptOutput(console.log);
console.info = interceptOutput(console.info);

try {
    const runtime = require("./build/generated/teavm/js/teavm-decimalformat-repro.js");
    if (typeof runtime.main !== "function") {
        throw new Error("TeaVM contract entry point was not generated");
    }

    runtime.main([], error => {
        setTimeout(() => {
            clearTimeout(timeout);
            if (error) {
                console.error("DecimalFormat affix contract failed:", error);
                process.exit(1);
            }
            if (failures.length > 0) {
                console.error(failures.join("\n"));
                process.exit(1);
            }
            if (!passed) {
                console.error("DecimalFormat affix contract ended without its success marker");
                process.exit(1);
            }
            process.exit(0);
        }, 100);
    });
} catch (error) {
    clearTimeout(timeout);
    console.error("DecimalFormat affix contract failed:", error);
    process.exit(1);
}
