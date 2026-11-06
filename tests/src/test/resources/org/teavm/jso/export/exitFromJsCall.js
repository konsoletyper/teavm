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

import { checkExit, checkExitError } from "./exitCommon.js";

export async function test() {
    await checkExit("exitFromJsCall", (module, ExitError) => {
        // thread is started asynchronously, so it should not start at all after exit
        module.startThenLog("thread started before exit");
        let error = null;
        try {
            module.exitNow(42);
        } catch (e) {
            error = e;
        }
        if (error === null) {
            throw new Error("exitNow returned normally");
        }
        checkExitError(error, ExitError, 42);
    }, 42, []);
}
