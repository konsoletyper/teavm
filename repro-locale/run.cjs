let passed = false;
const timeout = setTimeout(() => {
    console.error("Locale contract timed out");
    process.exit(2);
}, 15000);

function sign(value) {
    return value < 0 ? -1 : value > 0 ? 1 : 0;
}

try {
    const cases = [
        ["de-DE", "ä", "z", -1],
        ["en-US", "e", "é", -1],
        ["tr-TR", "I", "ı", 1],
        ["und-x-private", "a", "b", -1],
    ];
    for (const [tag, left, right, expected] of cases) {
        const reference = new Intl.Collator(tag, { usage: "sort", sensitivity: "variant" });
        if (sign(reference.compare(left, right)) !== expected) {
            throw new Error(`Node Intl.Collator reference mismatch for ${tag}`);
        }
    }

    const originalInfo = console.info;
    console.info = (...args) => {
        const line = args.join(" ");
        originalInfo(...args);
        if (line === "Locale contract passed") passed = true;
    };

    const runtime = require("./build/generated/teavm/js/teavm-locale-collation-repro.js");
    if (typeof runtime.main !== "function") {
        throw new Error("TeaVM locale contract entry point was not generated");
    }
    runtime.main([], error => {
        setTimeout(() => {
            clearTimeout(timeout);
            if (error) {
                console.error("Locale contract failed:", error);
                process.exit(1);
            }
            if (!passed) {
                console.error("Locale contract ended without reaching its success marker");
                process.exit(1);
            }
            process.exit(0);
        }, 100);
    });
} catch (error) {
    clearTimeout(timeout);
    console.error("Locale contract failed:", error);
    process.exit(1);
}
