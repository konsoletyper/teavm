/*
 *  Copyright 2026 Alexey Andreev.
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

// Usage: node junit-compiler.js <junit-compiler.wasm>
//
// Server that compiles test modules using TeaVM compiled to Wasm GC. Reads requests from stdin, one JSON
// object per line, writes responses to stdout, one per line, prefixed with RESPONSE_PREFIX
// (other lines are output of the compiler itself). Requests:
//
// - {"type": "init", "classpath": [...]} - sets class path, i.e. list of directories and JAR files, to take
//   classes and other resources from;
// - {"type": "compile", ...} - compiles test module, see CompileRequest.java for the rest of properties.
//
// Response is either "OK" or "ERROR <base64-encoded UTF-8 message>".

"use strict";

const path = require("node:path");
const fs = require("node:fs");
const zlib = require("node:zlib");
const readline = require("node:readline");
require(path.join(__dirname, "wasm-gc-runtime.js"));

const RESPONSE_PREFIX = "@@teavm-bootstrap-response@@";

// Java stack traces are taken from JS stack traces, which V8 truncates to 10 frames by default
Error.stackTraceLimit = Infinity;

let classpath = [];

class DirectoryEntry {
    constructor(dir) {
        this.dir = dir;
    }

    read(name) {
        const file = path.join(this.dir, name);
        try {
            return fs.readFileSync(file);
        } catch (e) {
            return null;
        }
    }
}

class ZipEntry {
    constructor(file) {
        this.file = file;
        this.data = null;
        this.entries = null;
    }

    open() {
        const buf = fs.readFileSync(this.file);
        let eocd = -1;
        for (let i = buf.length - 22; i >= Math.max(0, buf.length - 65557); --i) {
            if (buf.readUInt32LE(i) === 0x06054b50) {
                eocd = i;
                break;
            }
        }
        this.entries = new Map();
        this.data = buf;
        if (eocd < 0) {
            console.error("Not a valid ZIP file: " + this.file);
            return;
        }
        const count = buf.readUInt16LE(eocd + 10);
        let offset = buf.readUInt32LE(eocd + 16);
        for (let i = 0; i < count; ++i) {
            if (buf.readUInt32LE(offset) !== 0x02014b50) {
                console.error("Corrupted central directory in " + this.file);
                break;
            }
            const method = buf.readUInt16LE(offset + 10);
            const compressedSize = buf.readUInt32LE(offset + 20);
            const nameLength = buf.readUInt16LE(offset + 28);
            const extraLength = buf.readUInt16LE(offset + 30);
            const commentLength = buf.readUInt16LE(offset + 32);
            const localOffset = buf.readUInt32LE(offset + 42);
            const name = buf.toString("utf8", offset + 46, offset + 46 + nameLength);
            this.entries.set(name, { method, compressedSize, localOffset });
            offset += 46 + nameLength + extraLength + commentLength;
        }
    }

    read(name) {
        if (this.entries === null) {
            this.open();
        }
        const entry = this.entries.get(name);
        if (entry === undefined) {
            return null;
        }
        const buf = this.data;
        const nameLength = buf.readUInt16LE(entry.localOffset + 26);
        const extraLength = buf.readUInt16LE(entry.localOffset + 28);
        const start = entry.localOffset + 30 + nameLength + extraLength;
        const data = buf.subarray(start, start + entry.compressedSize);
        switch (entry.method) {
            case 0:
                return Buffer.from(data);
            case 8:
                return zlib.inflateRawSync(data);
            default:
                console.error("Unsupported compression method " + entry.method + " of " + name + " in "
                    + this.file);
                return null;
        }
    }
}

function createClasspathEntry(file) {
    try {
        const stat = fs.statSync(file);
        if (stat.isDirectory()) {
            return new DirectoryEntry(file);
        } else if (stat.isFile()) {
            return new ZipEntry(file);
        }
    } catch (e) {
        // Missing class path entries are fine, just ignore them
    }
    return null;
}

globalThis.teavmBootstrapReadResource = name => {
    for (const entry of classpath) {
        const data = entry.read(name);
        if (data !== null) {
            return new Int8Array(data.buffer, data.byteOffset, data.length);
        }
    }
    return null;
};

globalThis.teavmBootstrapWriteFile = (file, data) => {
    fs.writeFileSync(file, data);
};

function respond(error) {
    const response = error ? "ERROR " + Buffer.from(error, "utf8").toString("base64") : "OK";
    process.stdout.write(RESPONSE_PREFIX + response + "\n");
}

async function run() {
    const teavm = await TeaVM.wasmGC.load(path.resolve(process.argv[2]), {
        stackDeobfuscator: {
            enabled: true,
            infoLocation: "embedded"
        }
    });
    const lines = readline.createInterface({ input: process.stdin, crlfDelay: Infinity });
    for await (const line of lines) {
        if (line.trim() === "") {
            continue;
        }
        let error;
        try {
            const request = JSON.parse(line);
            switch (request.type) {
                case "init":
                    classpath = request.classpath.map(createClasspathEntry).filter(e => e !== null);
                    error = "";
                    break;
                case "compile":
                    error = teavm.exports.compile(request);
                    break;
                default:
                    error = "Unknown request type: " + request.type;
                    break;
            }
        } catch (e) {
            error = "Error processing request: " + (e && e.stack ? e.stack : String(e));
        }
        respond(error);
    }
}

run().catch(e => {
    console.error(e);
    process.exit(1);
});
