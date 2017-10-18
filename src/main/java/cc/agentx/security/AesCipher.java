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

import cc.agentx.util.KeyHelper;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.spec.SecretKeySpec;

public class AesCipher extends Cipher {
    // encryption mode
    public static final int AES_128_CFB = 16;
    public static final int AES_192_CFB = 24;
    public static final int AES_256_CFB = 32;
    public static final int AES_128_OFB = -16;
    public static final int AES_192_OFB = -24;
    public static final int AES_256_OFB = -32;
    private final int keyLength;
    private final StreamBlockCipher cipher;

    /**
     * <b>Notice: </b><br>
     * 1. the <code>AESFastEngine</code> was replaced by <code>AESEngine</code> now.<br>
     * 2. in <code>new CFBBlockCipher(engine, <b>16</b> * 8);</code> the IV length (16) is
     * reference to the shadowsocks's design.
     *
     * @see <a href="https://www.bouncycastle.org/releasenotes.html">
     * https://www.bouncycastle.org/releasenotes.html</a>#CVE-2016-1000339<br>
     * <a href="https://shadowsocks.org/en/spec/cipher.html">
     * https://shadowsocks.org/en/spec/cipher.html</a>#Cipher
     */
    public AesCipher(String password, int mode) {
        key = new SecretKeySpec(password.getBytes(), "AES");
        keyLength = Math.abs(mode);
        AESEngine engine = new AESEngine();
        if (mode > 0) {
            cipher = new CFBBlockCipher(engine, 16 * 8);
        } else {
            cipher = new OFBBlockCipher(engine, 16 * 8);
        }
    }

    public static boolean isValidMode(int mode) {
        int modeAbs = Math.abs(mode);
        return modeAbs == 16 || modeAbs == 24 || modeAbs == 32;
    }

    @Override
    protected void _init(boolean isEncrypt, byte[] iv) {
        String keyStr = new String(key.getEncoded());
        ParametersWithIV params = new ParametersWithIV(
                new KeyParameter(KeyHelper.generateKeyDigest(keyLength, keyStr)), iv
        );
        cipher.init(isEncrypt, params);
    }

    @Override
    protected byte[] _encrypt(final byte[] originData) {
        byte[] encryptedData = new byte[originData.length];
        cipher.processBytes(originData, 0, originData.length, encryptedData, 0);
        return encryptedData;
    }

    @Override
    protected byte[] _decrypt(final byte[] encryptedData) {
        byte[] originData = new byte[encryptedData.length];
        cipher.processBytes(encryptedData, 0, encryptedData.length, originData, 0);
        return originData;
    }

    @Override
    public int getIVLength() {
        return 16;
    }
}
