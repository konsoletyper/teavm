/*
 *  Copyright 2026 TeaVM contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// This adapts only BrowserRunner's callback protocol. The generated fixture's
// actual exported main gets a fresh IO reader for every fixture mode.
export function main(args, callback) {
    run(args[0]).then(() => callback(), error => callback(error));
}

async function run(path) {
    const url = new URL(path, location.origin);
    const args = [url.searchParams.get("mode")];
    path = url.pathname;
    if (path.endsWith(".js")) {
        const program = await import(path);
        await new Promise((resolve, reject) => {
            program.main(args, error => error instanceof Error ? reject(error) : resolve());
        });
    } else {
        const { load } = await import('/resources/org/teavm/backend/wasm/wasm-gc-module-runtime.js');
        const program = await load(path, {
            stackDeobfuscator: { enabled: false },
            installImports(imports) {
                imports.teavmConsole.putcharStdout = code => $rt_putStdoutCustom(String.fromCharCode(code));
                imports.teavmConsole.putcharStderr = code => $rt_putStderrCustom(String.fromCharCode(code));
            }
        });
        await program.exports.main(args);
    }
    if (args[0] === "host-input") {
        const input = await import('/resources/org/teavm/classlib/java/lang/io-readln/host-input.js');
        if (input.reads === 0) {
            throw new Error("The host input callback was not called");
        }
    }
}
