/*
 * Copyright 2017 ZhangJiupeng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.agentx.wrapper;

import cc.agentx.security.Cipher;
import cc.agentx.util.KeyHelper;

import java.util.Arrays;

public class CipherWrapper extends Wrapper {
    private final Cipher encipher;
    private final Cipher decipher;
    private byte[] encipherIv;
    private byte[] decipherIv;

    public CipherWrapper(Cipher encipher, Cipher decipher) {
        if (encipher.getClass() != decipher.getClass())
            throw new RuntimeException("cipher type not match");

        this.encipher = encipher;
        this.decipher = decipher;
    }

    @Override
    public byte[] wrap(final byte[] bytes) {
        if (encipherIv == null) {
            int ivLength = encipher.getIVLength();
            this.encipherIv = KeyHelper.generateRandomBytes(ivLength);
            encipher.init(true, encipherIv);
            byte[] encryptedBytes = new byte[ivLength + bytes.length];
            System.arraycopy(encipherIv, 0, encryptedBytes, 0, ivLength);
            System.arraycopy(encipher.encrypt(bytes), 0, encryptedBytes, ivLength, bytes.length);
            return encryptedBytes;
        }
        return encipher.encrypt(bytes);
    }

    @Override
    public byte[] unwrap(final byte[] bytes) {
        if (decipherIv == null) {
            int ivLength = decipher.getIVLength();
            if (bytes.length < ivLength)
                throw new RuntimeException("invalid encrypted data");

            this.decipherIv = Arrays.copyOfRange(bytes, 0, ivLength);
            decipher.init(false, decipherIv);
            byte[] encryptedBytes = new byte[bytes.length - ivLength];
            System.arraycopy(bytes, ivLength, encryptedBytes, 0, encryptedBytes.length);
            return decipher.decrypt(encryptedBytes);
        }
        return decipher.decrypt(bytes);
    }
}
