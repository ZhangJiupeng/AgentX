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

import cc.agentx.protocol.request.FakedHttpRequestResolver;
import cc.agentx.protocol.request.ShadowsocksRequestResolver;
import cc.agentx.protocol.request.XRequestResolver;
import cc.agentx.security.AesCipher;
import cc.agentx.security.BlowfishCipher;
import cc.agentx.security.Cipher;

public class WrapperFactory {

    private WrapperFactory() {
    }

    public static Wrapper getInstance(cc.agentx.client.Configuration config, String id) throws Exception {
        return getInstance(config.getEncryption(), config.getPassword(), id);
    }

    public static Wrapper getInstance(cc.agentx.server.Configuration config, String id) throws Exception {
        return getInstance(config.getEncryption(), config.getPassword(), id);
    }

    /**
     * Notice:
     * The frame-based processes below cannot be configured simultaneously in this version of implementation
     * <p>
     * compress, zero-padding, random-padding
     */
    public static Wrapper getInstance(String encryption, String password, String id) throws Exception {
        switch (id) {
            case "raw":
                return new RawWrapper();
            case "encrypt":
                switch (encryption) {
                    case "aes-256-cfb":
                        return new CipherWrapper(
                                new AesCipher(password, AesCipher.AES_256_CFB),
                                new AesCipher(password, AesCipher.AES_256_CFB)
                        );
                    case "aes-192-cfb":
                        return new CipherWrapper(
                                new AesCipher(password, AesCipher.AES_192_CFB),
                                new AesCipher(password, AesCipher.AES_192_CFB)
                        );
                    case "aes-128-cfb":
                        return new CipherWrapper(
                                new AesCipher(password, AesCipher.AES_128_CFB),
                                new AesCipher(password, AesCipher.AES_128_CFB)
                        );
                    case "aes-256-ofb":
                        return new CipherWrapper(
                                new AesCipher(password, AesCipher.AES_256_OFB),
                                new AesCipher(password, AesCipher.AES_256_OFB)
                        );
                    case "aes-192-ofb":
                        return new CipherWrapper(
                                new AesCipher(password, AesCipher.AES_192_OFB),
                                new AesCipher(password, AesCipher.AES_192_OFB)
                        );
                    case "aes-128-ofb":
                        return new CipherWrapper(
                                new AesCipher(password, AesCipher.AES_128_OFB),
                                new AesCipher(password, AesCipher.AES_128_OFB)
                        );
                    case "bf-cfb":
                        return new CipherWrapper(
                                new BlowfishCipher(password, BlowfishCipher.BLOWFISH_CFB),
                                new BlowfishCipher(password, BlowfishCipher.BLOWFISH_CFB)
                        );
                    default:
                        throw new Exception("unknown encryption");
                }
            case "compress":
                return new FrameWrapper(262144, new CompressWrapper());
            case "zero-padding":
                return new FrameWrapper(262144, new ZeroPaddingWrapper(200, 56));
            case "random-padding":
                return new FrameWrapper(262144, new RandomPaddingWrapper(200, 56));
            default:
                throw new Exception("unknown process function");
        }
    }

    public static boolean exists(cc.agentx.client.Configuration config, String id) {
        return exists(config.getEncryption(), id);
    }

    public static boolean exists(cc.agentx.server.Configuration config, String id) {
        return exists(config.getEncryption(), id);
    }

    public static boolean exists(String encryption, String id) {
        switch (id) {
            case "raw":
                return true;
            case "encrypt":
                switch (encryption) {
                    case "aes-256-cfb":
                    case "aes-192-cfb":
                    case "aes-128-cfb":
                    case "aes-256-ofb":
                    case "aes-192-ofb":
                    case "aes-128-ofb":
                    case "bf-cfb":
                        return true;
                    default:
                        return false;
                }
            case "compress":
            case "zero-padding":
            case "random-padding":
                return true;
            default:
                return false;
        }
    }

    public static Wrapper getInstance(Wrapper... wrapper) {
        return new MultiWrapper(wrapper);
    }

    public static Wrapper newRawWrapperInstance() {
        return new RawWrapper();
    }

    public static Wrapper newZeroPaddingWrapper(int threshold, int range) {
        return new ZeroPaddingWrapper(threshold, range);
    }

    public static Wrapper newRandomPaddingWrapper(int threshold, int range) {
        return new RandomPaddingWrapper(threshold, range);
    }

    public static Wrapper newCompressWrapperInstance() {
        return new CompressWrapper();
    }

    public static Wrapper newHttpWrapperInstance(boolean requestMode) {
        return new FakedHttpWrapper(requestMode);
    }

    public static Wrapper newMultiWrapperInstance(Wrapper... wrappers) {
        return new MultiWrapper(wrappers);
    }

    public static Wrapper newCipherWrapperInstance(Cipher encipher, Cipher decipher) {
        return new CipherWrapper(encipher, decipher);
    }

    public static Wrapper newFrameWrapperInstance(int fixedFrameLength) {
        return new FrameWrapper(fixedFrameLength);
    }

    public static Wrapper newFrameWrapperInstance(int fixedFrameLength, Wrapper frameHandler) {
        return new FrameWrapper(fixedFrameLength, frameHandler);
    }

    public static XRequestResolver newShadowsocksRequestWrapperInstance() {
        return new ShadowsocksRequestResolver();
    }

    public static XRequestResolver newFakedHttpRequestWrapperInstance() {
        return new FakedHttpRequestResolver();
    }

}
