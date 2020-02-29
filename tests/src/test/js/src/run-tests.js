/*
 *  Copyright 2017 Alexey Andreev.
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

"use strict";
import * as fs from "./promise-fs.js";
import * as http from "http";
import {server as WebSocketServer} from "websocket";

let totalTests = 0;

class TestSuite {
    constructor(name) {
        this.name = name;
        this.testSuites = [];
        this.testCases = [];
    }
}
class TestCase {
    constructor(type, file, argument) {
        this.type = type;
        this.file = file;
        this.argument = argument
    }
}

let rootDir = process.argv[2];
if (rootDir.endsWith("/")) {
    rootDir = rootDir.substring(0, rootDir.length - 1);
}

async function runAll() {
    const rootSuite = new TestSuite("root");
    console.log("Searching tests");
    await walkDir("", rootSuite);

    console.log("Running tests");

    const server = http.createServer((request, response) => {
        if (request.url.endsWith(".js") || request.url.endsWith(".wasm")) {
            serveFile(rootDir + "/" + request.url, response);
            return;
        }
        response.writeHead(404);
        response.end();
    });
    server.listen({ host: "localhost", port: 9090 }, () => {
        console.log((new Date()) + ' Server is listening on port 9090');
    });

    const wsServer = new WebSocketServer({
        httpServer: server,
        autoAcceptConnections: true
    });

    const startTime = new Date().getTime();
    const connectPromise = new Promise(resolve => wsServer.on("connect", resolve));
    const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error("Connection time out")), 15000)
    });
    const conn = await Promise.race([connectPromise, timeoutPromise]);

    const runner = new TestRunner(conn);
    await runner.runTests(rootSuite, "", 0);

    wsServer.unmount();
    server.close();

    const endTime = new Date().getTime();
    for (let i = 0; i < runner.testsFailed.length; i++) {
        const failedTest = runner.testsFailed[i];
        console.log("(" + (i + 1) + ") " + failedTest.path +":");
        console.log(failedTest.message);
        console.log();
    }

    console.log("Tests run: " + runner.testsRun + ", failed: " + runner.testsFailed.length
            + ", elapsed " + ((endTime - startTime) / 1000) + " seconds");

    if (runner.testsFailed.length > 0) {
        process.exit(1);
    } else {
        process.exit(0);
    }
}

async function serveFile(path, response) {
    const stat = await fs.stat(path);
    const contentType = path.endsWith(".wasm") ? "application/octet-stream" : "text/javascript";
    if (stat.isFile()) {
        const content = await fs.readFile(path);
        response.writeHead(200, { 'Content-Type': contentType, 'Access-Control-Allow-Origin': "*" });
        response.end(content, 'utf-8');
    } else {
        response.writeHead(404);
        response.end();
    }
}

async function walkDir(path, suite) {
    const files = await fs.readdir(rootDir + "/" + path);
    if (files.includes("tests.json")) {
        const descriptor = JSON.parse(await fs.readFile(`${rootDir}/${path}/tests.json`));
        for (const { baseDir, fileName, kind, argument } of descriptor) {
            switch (kind) {
                case "JAVASCRIPT":
                case "WASM":
                    suite.testCases.push(new TestCase(kind, `${baseDir}/${fileName}`, argument));
                    totalTests++;
                    break;
            }
        }
    }
    await Promise.all(files.map(async file => {
        const filePath = path + "/" + file;
        const stat = await fs.stat(rootDir + "/" + filePath);
        if (stat.isDirectory()) {
            const childSuite = new TestSuite(file);
            suite.testSuites.push(childSuite);
            await walkDir(filePath, childSuite);
        }
    }));
}

class TestRunner {
    constructor(ws) {
        this.ws = ws;
        this.testsRun = 0;
        this.testsFailed = [];

        this.pendingRequests = Object.create(null);
        this.requestIdGen = 0;

        this.timeout = null;

        this.ws.on("message", (message) => {
            const response = JSON.parse(message.utf8Data);
            const pendingRequest = this.pendingRequests[response.id + "-" + response.index];
            delete this.pendingRequests[response.id];
            if (this.timeout) {
                this.timeout.refresh();
            }
            pendingRequest(response.result);
        })
    }

    async runTests(suite, path, depth) {
        if (suite.testCases.length > 0) {
            console.log("Running " + path);
            let testsFailedInSuite = 0;
            const startTime = new Date().getTime();
            let request = { id: this.requestIdGen++ };
            request.tests = suite.testCases.map(testCase => {
                const result = {
                    type: testCase.type,
                    name: testCase.name,
                    file: testCase.file
                };
                if (testCase.argument) {
                    result.argument = testCase.argument;
                }
                return result;
            });
            this.testsRun += suite.testCases.length;

            const resultPromises = [];

            this.timeout = createRefreshableTimeoutPromise(20000);
            for (let i = 0; i < suite.testCases.length; ++i) {
                resultPromises.push(new Promise(resolve => {
                    this.pendingRequests[request.id + "-" + i] = resolve;
                }));
            }

            this.ws.send(JSON.stringify(request));
            const result = await Promise.race([Promise.all(resultPromises), this.timeout.promise]);

            for (let i = 0; i < suite.testCases.length; i++) {
                const testCase = suite.testCases[i];
                const testResult = result[i];
                switch (testResult.status) {
                    case "OK":
                        break;
                    case "failed":
                        this.logFailure(path, testCase, testResult.errorMessage);
                        testsFailedInSuite++;
                        break;
                }
            }
            const endTime = new Date().getTime();
            const percentComplete = (this.testsRun / totalTests * 100).toFixed(1);
            console.log("Tests run: " + suite.testCases.length + ", failed: " + testsFailedInSuite
                    + ", elapsed: " + ((endTime - startTime) / 1000) + " seconds "
                    + "(" + percentComplete + "% complete)");
            console.log();
        }

        for (const childSuite of suite.testSuites) {
            await this.runTests(childSuite, path + "/" + childSuite.name, depth + 1);
        }
    }

    logFailure(path, testCase, message) {
        console.log("  " + testCase.name + " failure (" + (this.testsFailed.length + 1) + ")");
        this.testsFailed.push({
            path: path + "/" + testCase.name,
            message: message
        });
    }

    async runTeaVMTest(testCase) {
        await this.page.reload();

        const fileContents = await Promise.all(testCase.files.map(async (file) => {
            return fs.readFile(file, 'utf8');
        }));
        for (let i = 0; i < testCase.files.length; i++) {
            const fileName = testCase.files[i];
            const contents = fileContents[i];
            const { scriptId } = await this.runtime.compileScript({
                expression: contents,
                sourceURL: fileName,
                persistScript: true
            });
            TestRunner.checkScriptResult(await this.runtime.runScript({ scriptId : scriptId }), fileName);
        }

        const { scriptId: runScriptId } = await this.runtime.compileScript({
            expression: "(" + runTeaVM.toString() + ")()",
            sourceURL: " ",
            persistScript: true
        });
        const result = TestRunner.checkScriptResult(await this.runtime.runScript({
            scriptId: runScriptId,
            awaitPromise: true,
            returnByValue: true
        }));

        return result.result.value;
    }

    static checkScriptResult(scriptResult, scriptUrl) {
        if (scriptResult.result.subtype === "error") {
            throw new Error("Exception caught from script " + scriptUrl + ":\n" + scriptResult.result.description);
        }
        return scriptResult;
    }
}

function runTeaVM() {
    return new Promise(resolve => {
        main([], result => {
            const message = {};
            if (result instanceof Error) {
                makeErrorMessage(message, result);
            } else {
                message.status = "OK";
            }
            resolve(message);
        });

        function makeErrorMessage(message, e) {
            message.status = "failed";
            if (e.$javaException) {
                message.className = e.$javaException.constructor.name;
                message.message = e.$javaException.getMessage();
            } else {
                message.className = Object.getPrototypeOf(e).name;
                message.message = e.message;
            }
            message.exception = e;
            message.stack = e.stack;
        }
    })
}


function createRefreshableTimeoutPromise(timeout) {
    const obj = {
        currentId: 0,
        resolveFun: () => {},
        refresh: () => {
            const expectedId = ++obj.currentId;
            setTimeout(() => {
                if (expectedId === obj.currentId) {
                    obj.resolveFun(new Error("Connection timeout"))
                }
            }, timeout);
        }
    };

    const result = {
        promise: new Promise((_, reject) => {
            obj.resolveFun = reject;
        }),
        refresh: () => obj.refresh()
    };
    obj.refresh();
    return result;
}

runAll().catch(e => {
    console.log(e.stack);
    process.exit(1);
});