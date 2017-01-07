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

package cc.agentx.security;

import javax.crypto.SecretKey;

public abstract class Cipher {
    protected SecretKey key;
    protected byte[] iv;
    protected boolean isEncrypt;

    public void init(boolean isEncrypt, byte[] iv) {
        if (this.iv == null) {
            this.isEncrypt = isEncrypt;
            this.iv = iv;
            _init(isEncrypt, iv);
        } else {
            throw new RuntimeException("cipher cannot reinitiate");
        }
    }

    public byte[] encrypt(byte[] originData) {
        if (this.iv == null)
            throw new CipherNotInitializedException();
        if (!isEncrypt)
            throw new RuntimeException("cannot encrypt in decrypt mode");
        return _encrypt(originData);
    }

    public byte[] decrypt(byte[] encryptedData) {
        if (this.iv == null)
            throw new CipherNotInitializedException();
        if (isEncrypt)
            throw new RuntimeException("cannot decrypt in encrypt mode");
        return _decrypt(encryptedData);
    }

    public SecretKey getKey() {
        return key;
    }

    public byte[] getIV() {
        return iv;
    }

    public abstract int getIVLength();

    protected abstract void _init(boolean isEncrypt, byte[] iv);

    protected abstract byte[] _encrypt(final byte[] originData);

    protected abstract byte[] _decrypt(final byte[] encryptedData);

}
