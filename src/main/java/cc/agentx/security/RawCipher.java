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

public class RawCipher extends Cipher {
    @Override
    protected void _init(boolean isEncrypt, byte[] iv) {
        this.iv = new byte[0];
    }

    @Override
    protected byte[] _encrypt(final byte[] originData) {
        return originData;
    }

    @Override
    protected byte[] _decrypt(final byte[] encryptedData) {
        return encryptedData;
    }

    @Override
    public int getIVLength() {
        return 0;
    }
}
