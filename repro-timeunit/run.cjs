const runtime = require("./build/generated/teavm/js/teavm-timeunit-repro.js");

if (typeof runtime.main !== "function") {
    throw new Error("TeaVM contract entry point was not generated");
}

runtime.main([]);
