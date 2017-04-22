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
import { server as WebSocketServer } from "websocket";

const TEST_FILE_NAME = "test.js";
const RUNTIME_FILE_NAME = "runtime.js";
const TEST_FILES = [
    { file: TEST_FILE_NAME, name: "simple" },
    { file: "test-min.js", name: "minified" },
    { file: "test-optimized.js", name: "optimized" }
];
let totalTests = 0;

class TestSuite {
    constructor(name) {
        this.name = name;
        this.testSuites = [];
        this.testCases = [];
    }
}
class TestCase {
    constructor(name, files) {
        this.name = name;
        this.files = files;
    }
}

async function runAll() {
    const rootSuite = new TestSuite("root");
    console.log("Searching tests");
    await walkDir(process.argv[2], "root", rootSuite);

    console.log("Running tests");
    const stats = { testRun: 0, testsFailed: [] };

    const server = http.createServer((request, response) => {
        response.writeHead(404);
        response.end();
    });
    server.listen(9090, () => {
        console.log((new Date()) + ' Server is listening on port 8080');
    });

    const wsServer = new WebSocketServer({
        httpServer: server,
        autoAcceptConnections: true
    });

    const startTime = new Date().getTime();
    await new Promise((resolve, reject) => {
        wsServer.on("connect", async (conn) => {
            try {
                const runner = new TestRunner(conn);
                await runner.runTests(rootSuite, "", 0);
                stats.testRun = runner.testsRun;
                stats.testsFailed = runner.testsFailed;
                resolve();
            } catch (e) {
                reject(e);
            }
        })
    });

    wsServer.unmount();
    server.close();

    const endTime = new Date().getTime();
    for (let i = 0; i < stats.testsFailed.length; i++) {
        const failedTest = stats.testsFailed[i];
        console.log("(" + (i + 1) + ") " + failedTest.path +":");
        console.log(failedTest.message);
        console.log();
    }

    console.log("Tests run: " + stats.testRun + ", failed: " + stats.testsFailed.length
            + ", elapsed " + ((endTime - startTime) / 1000) + " seconds");

    if (stats.testsFailed.length > 0) {
        process.exit(1);
    } else {
        process.exit(0);
    }
}

async function walkDir(path, name, suite) {
    const files = await fs.readdir(path);
    if (files.includes(TEST_FILE_NAME) && files.includes(RUNTIME_FILE_NAME)) {
        for (const { file: fileName, name: profileName } of TEST_FILES) {
            if (files.includes(fileName)) {
                suite.testCases.push(new TestCase(
                    name + " " + profileName,
                    [path + "/" + RUNTIME_FILE_NAME, path + "/" + fileName]));
                totalTests++;
            }
        }
    } else if (files) {
        const childSuite = new TestSuite(name);
        suite.testSuites.push(childSuite);
        await Promise.all(files.map(async file => {
            const filePath = path + "/" + file;
            const stat = await fs.stat(filePath);
            if (stat.isDirectory()) {
                await walkDir(filePath, file, childSuite);
            }
        }));
    }
}

class TestRunner {
    constructor(ws) {
        this.ws = ws;
        this.testsRun = 0;
        this.testsFailed = [];

        this.pendingRequests = Object.create(null);
        this.requestIdGen = 0;

        this.ws.on("message", (message) => {
            const response = JSON.parse(message.utf8Data);
            const pendingRequest = this.pendingRequests[response.id];
            delete this.pendingRequests[response.id];
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
                return {
                    name: testCase.name,
                    files: testCase.files.map(fileName => process.cwd() + "/" + fileName)
                };
            });
            this.testsRun += suite.testCases.length;

            const resultPromise = new Promise(resolve => {
                this.pendingRequests[request.id] = resolve;
            });
            const timeoutPromise = new Promise((_, reject) => {
                setTimeout(() => reject(new Error("connection timeout")), 120000);
            });
            this.ws.send(JSON.stringify(request));
            const result = await Promise.race([resultPromise, timeoutPromise]);

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
        console.log("  " + testCase.name + "failure (" + (this.testsFailed.length + 1) + ")");
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
        $rt_startThread(() => {
            const thread = $rt_nativeThread();
            let instance;
            if (thread.isResuming()) {
                instance = thread.pop();
            }
            try {
                runTest();
            } catch (e) {
                resolve({ status: "failed", errorMessage: buildErrorMessage(e) });
                return;
            }
            if (thread.isSuspending()) {
                thread.push(instance);
                return;
            }
            resolve({ status: "OK" });
        });

        function buildErrorMessage(e) {
            let stack = e.stack;
            if (e.$javaException && e.$javaException.constructor.$meta) {
                stack = e.$javaException.constructor.$meta.name + ": ";
                const exceptionMessage = extractException(e.$javaException);
                stack += exceptionMessage ? $rt_ustr(exceptionMessage) : "";
            }
            stack += "\n" + stack;
            return stack;
        }
    })
}

runAll().catch(e => {
    console.log(e.stack);
    process.exit(1);
});