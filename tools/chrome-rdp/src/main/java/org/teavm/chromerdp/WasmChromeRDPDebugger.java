/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.chromerdp;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.teavm.backend.wasm.debug.parser.DebugInfoParser;
import org.teavm.backend.wasm.debug.parser.DebugInfoReader;
import org.teavm.chromerdp.data.Message;
import org.teavm.chromerdp.messages.GetScriptSourceCommand;
import org.teavm.chromerdp.messages.ScriptParsedNotification;
import org.teavm.chromerdp.messages.ScriptSource;
import org.teavm.common.CompletablePromise;
import org.teavm.common.Promise;

public class WasmChromeRDPDebugger extends BaseChromeRDPDebugger {
    public WasmChromeRDPDebugger(Executor executor) {
        super(executor);
    }

    @Override
    protected void onAttach() {

    }

    @Override
    protected void onDetach() {

    }

    @Override
    protected Promise<Void> handleMessage(Message message) throws IOException {
        switch (message.getMethod()) {
            case "Debugger.scriptParsed":
                return scriptParsed(parseJson(ScriptParsedNotification.class, message.getParams()));
        }
        return Promise.VOID;
    }

    private Promise<Void> scriptParsed(ScriptParsedNotification params) {
        if (Objects.equals(params.getScriptLanguage(), "WebAssembly")) {
            var callArgs = new GetScriptSourceCommand();
            callArgs.scriptId = params.getScriptId();
            return callMethodAsync("Debugger.getScriptSource", ScriptSource.class, callArgs)
                    .thenVoid(source -> parseWasm(source, params.getUrl()));
        }
        return Promise.VOID;
    }

    private void parseWasm(ScriptSource source, String url) {
        if (source.bytecode == null) {
            return;
        }
        var bytes = Base64.getDecoder().decode(source.bytecode);
        var reader = new DebugInfoReaderImpl(bytes);
        var debugInfoParser = new DebugInfoParser(reader);
        Promise.runNow(() -> {
            debugInfoParser.parse().catchVoid(Throwable::printStackTrace);
            reader.complete();
        });
        var lineInfo = debugInfoParser.getLineInfo();
        if (lineInfo != null) {
            System.out.println("Debug information found in script: " + url);
        }
    }

    private static class DebugInfoReaderImpl implements DebugInfoReader {
        private byte[] data;
        private int ptr;
        private CompletablePromise<Integer> promise;
        private byte[] target;
        private int offset;
        private int count;

        DebugInfoReaderImpl(byte[] data) {
            this.data = data;
        }

        @Override
        public Promise<Integer> skip(int amount) {
            promise = new CompletablePromise<>();
            count = amount;
            return promise;
        }

        @Override
        public Promise<Integer> read(byte[] buffer, int offset, int count) {
            promise = new CompletablePromise<>();
            this.target = buffer;
            this.offset = offset;
            this.count = count;
            return promise;
        }

        private void complete() {
            while (promise != null) {
                var p = promise;
                count = Math.min(count, data.length - ptr);
                promise = null;
                if (target != null) {
                    System.arraycopy(data, ptr, target, offset, count);
                    target = null;
                }
                ptr += count;
                if (count == 0) {
                    count = -1;
                }
                p.complete(count);
            }
        }
    }
}
