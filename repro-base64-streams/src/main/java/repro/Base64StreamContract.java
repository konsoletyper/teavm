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
package repro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public final class Base64StreamContract {
    private Base64StreamContract() {
    }

    public static void main(String[] args) throws Exception {
        Base64.Encoder[] encoders = { Base64.getEncoder(), Base64.getEncoder().withoutPadding(),
                Base64.getUrlEncoder(), Base64.getUrlEncoder().withoutPadding() };
        for (int variant = 0; variant < encoders.length; ++variant) {
            Base64.Encoder encoder = encoders[variant];
            Base64.Decoder decoder = variant < 2 ? Base64.getDecoder() : Base64.getUrlDecoder();
            for (int length = 0; length < 100; ++length) {
                byte[] input = new byte[length];
                for (int i = 0; i < length; ++i) {
                    input[i] = (byte) (i * 73 + length);
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (OutputStream stream = encoder.wrap(bytes)) {
                    int prefix = Math.min(2, length);
                    for (int i = 0; i < prefix; ++i) {
                        stream.write(input[i]);
                    }
                    stream.write(input, prefix, length - prefix);
                }
                check(Arrays.equals(bytes.toByteArray(), encoder.encode(input)), "incremental encoding " + length);
                try (InputStream stream = decoder.wrap(new ByteArrayInputStream(bytes.toByteArray()))) {
                    ByteArrayOutputStream decoded = new ByteArrayOutputStream();
                    int first = stream.read();
                    if (first >= 0) {
                        decoded.write(first);
                    }
                    byte[] buffer = new byte[7];
                    int count;
                    while ((count = stream.read(buffer, 1, 5)) >= 0) {
                        check(count > 0, "nonempty read makes progress");
                        decoded.write(buffer, 1, count);
                    }
                    check(stream.read(buffer, 0, 0) == 0, "zero read at EOF");
                    check(Arrays.equals(decoded.toByteArray(), input), "incremental decoding " + length);
                }
            }
        }
        check("_w==".equals(Base64.getUrlEncoder().encodeToString(new byte[] { -1 })), "URL encoder padding");
        TrackedOutput output = new TrackedOutput();
        OutputStream encoder = Base64.getEncoder().wrap(output);
        encoder.write('f');
        encoder.flush();
        check(output.size() == 0 && output.flushes == 1, "flush preserves incomplete quantum");
        encoder.close();
        encoder.close();
        check(output.closes == 1 && "Zg==".equals(output.toString(StandardCharsets.US_ASCII)), "close pads once");
        expectIOException(() -> encoder.write('x'), "write after close");
        TrackedInput input = new TrackedInput("Zg==tail");
        InputStream decoder = Base64.getDecoder().wrap(input);
        check(decoder.read() == 'f' && decoder.read() == -1, "padding terminates stream");
        check(input.read() == 't', "trailing bytes not consumed");
        decoder.close();
        decoder.close();
        check(input.closes == 1, "decoder closes underlying once");
        expectIOException(decoder::read, "read after close");
        for (String malformed : new String[] { "Z", "=AAA", "Z===", "Zg=", "Zg=A", "Zm$=", "Z g==" }) {
            expectIOException(() -> {
                try (InputStream stream = Base64.getDecoder().wrap(asInput(malformed))) {
                    stream.readAllBytes();
                }
            }, "malformed " + malformed);
        }
        check(Arrays.equals(Base64.getDecoder().wrap(asInput("Zg")).readAllBytes(), new byte[] { 'f' }),
                "unpadded two characters");
        check(Arrays.equals(Base64.getDecoder().wrap(asInput("Zm8")).readAllBytes(), new byte[] { 'f', 'o' }),
                "unpadded three characters");
        try {
            Base64.getEncoder().wrap(null);
            throw new AssertionError("null output accepted");
        } catch (NullPointerException expected) {
            // Required null contract.
        }
        try {
            Base64.getDecoder().wrap(null);
            throw new AssertionError("null input accepted");
        } catch (NullPointerException expected) {
            // Required null contract.
        }
        System.out.println("Base64 stream contract passed");
    }

    private static ByteArrayInputStream asInput(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII));
    }

    private static void expectIOException(IoAction action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IOException expected) {
            // Required stream error contract.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private interface IoAction {
        void run() throws Exception;
    }

    private static final class TrackedOutput extends ByteArrayOutputStream {
        int closes;
        int flushes;

        @Override
        public void close() {
            ++closes;
        }

        @Override
        public void flush() {
            ++flushes;
        }
    }

    private static final class TrackedInput extends ByteArrayInputStream {
        int closes;

        TrackedInput(String text) {
            super(text.getBytes(StandardCharsets.US_ASCII));
        }

        @Override
        public void close() {
            ++closes;
        }
    }
}
