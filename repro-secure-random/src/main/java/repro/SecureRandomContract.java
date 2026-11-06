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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class SecureRandomContract {
    private SecureRandomContract() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "unavailable".equals(args[0])) {
            try {
                SecureRandom.getInstanceStrong();
                throw new AssertionError("strong random available without crypto provider");
            } catch (NoSuchAlgorithmException expected) {
                System.out.println("Secure random unavailable contract passed");
                return;
            }
        }
        SecureRandom random = SecureRandom.getInstanceStrong();
        check(random.getAlgorithm() != null, "provider algorithm");
        for (int size : new int[] { 0, 1, 65536, 65537, 131073 }) {
            byte[] bytes = new byte[size];
            random.nextBytes(bytes);
            if (size > 1) {
                boolean changed = false;
                for (byte value : bytes) {
                    changed |= value != 0;
                }
                check(changed, "random source fills output");
            }
        }
        check(random.generateSeed(65537).length == 65537, "large seed generation");
        random.nextInt();
        double fraction = random.nextDouble();
        check(fraction >= 0 && fraction < 1, "random fraction range");
        try {
            random.nextBytes(null);
            throw new AssertionError("null bytes accepted");
        } catch (NullPointerException expected) {
            // Required null contract.
        }
        try {
            random.generateSeed(-1);
            throw new AssertionError("negative seed size accepted");
        } catch (IllegalArgumentException expected) {
            // Required bounds contract.
        }
        System.out.println("Secure random contract passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
