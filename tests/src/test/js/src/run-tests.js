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

import * as fs from "./promise-fs.js";
import { default as CDP } from 'chrome-remote-interface';

const TEST_FILE_NAME = "test.js";
const RUNTIME_FILE_NAME = "runtime.js";
const TEST_FILES = [
    { file: TEST_FILE_NAME, name: "simple" },
    { file: "test-min.js", name: "minified" },
    { file: "test-optimized.js", name: "optimized" }
];

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
    const startTime = new Date().getTime();

    await new Promise((resolve, reject) => {
        CDP(async (client) => {
            try {
                const {Page, Runtime} = client;
                await Promise.all([Runtime.enable(), Page.enable()]);
                await Page.navigate({url: "about:blank"});
                //await Page.loadEventFired();

                const runner = new TestRunner(Page, Runtime);
                await runner.runTests(rootSuite, "", 0);
                stats.testRun = runner.testsRun;
                stats.testsFailed = runner.testsFailed;
                await client.close();
                resolve();
            } catch (e) {
                reject(e);
            }
        }).on("error", err => {
            reject(err);
        }).on("disconnect", () => {
            reject("disconnected from chrome");
        });
    });

    const endTime = new Date().getTime();
    console.log();
    for (let i = 0; i < stats.testsFailed.length; i++) {
        const failedTest = stats.testsFailed[i];
        console.log("(" + (i + 1) + ") " + failedTest.path +":");
        console.log(failedTest.message);
        console.log();
    }

    console.log("Tests run: " + stats.testRun + ", failed: " + stats.testsFailed.length
            + ", took " + (endTime - startTime) + " millisecond(s)");

    if (stats.testsFailed.length > 0) {
        process.exit(1);
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
            }
        }
    } else if (files) {
        await Promise.all(files.map(async file => {
            const filePath = path + "/" + file;
            const stat = await fs.stat(filePath);
            if (stat.isDirectory()) {
                const childSuite = new TestSuite(file);
                suite.testSuites.push(childSuite);
                await walkDir(filePath, file, childSuite);
            }
        }));
    }
}

class TestRunner {
    constructor(page, runtime) {
        this.page = page;
        this.runtime = runtime;
        this.testsRun = 0;
        this.testsFailed = [];
    }

    async runTests(suite, path, depth) {
        let prefix = "";
        for (let i = 0; i < depth; i++) {
            prefix += "  ";
        }

        console.log(prefix + suite.name + "/");
        for (const testCase of suite.testCases) {
            this.testsRun++;
            process.stdout.write(prefix + "  " + testCase.name + "... ");
            try {
                const testRun = Promise.race([
                    this.runTeaVMTest(testCase),
                    new Promise(resolve => {
                        setTimeout(() => resolve({ status: "failed", errorMessage: "timeout" }), 1000);
                    })
                ]);
                const result = await testRun;
                switch (result.status) {
                    case "OK":
                        process.stdout.write("OK");
                        break;
                    case "failed":
                        this.logFailure(path, testCase, result.errorMessage);
                        break;
                }
            } catch (e) {
                this.logFailure(path, testCase, e.stack);
            }
            process.stdout.write("\n");
        }
        for (const childSuite of suite.testSuites) {
            await this.runTests(childSuite, path + "/" + suite.name, depth + 1);
        }
    }

    logFailure(path, testCase, message) {
        process.stdout.write("failure (" + (this.testsFailed.length + 1) + ")");
        this.testsFailed.push({
            path: path + "/" + testCase.name,
            message: message
        });
    }

    async runTeaVMTest(testCase) {
        await this.page.reload();
        //await this.page.loadEventFired();

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