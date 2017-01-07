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

package cc.agentx.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class KeyHelper {
    private static SecureRandom randomizer = new SecureRandom();

    private KeyHelper() {
    }

    public static int generateRandomInteger(int min, int max) {
        return min + randomizer.nextInt(max - min);
    }

    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        randomizer.nextBytes(bytes);
        return bytes;
    }

    public static byte[] generateKeyDigest(int keyLength, String password) {
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            int length = (keyLength + 15) / 16 * 16;
            byte[] passwordBytes = password.getBytes("UTF-8");
            byte[] temp = digester.digest(passwordBytes);
            byte[] key = Arrays.copyOf(temp, length);
            for (int i = 1; i < length / 16; i++) {
                temp = Arrays.copyOf(temp, 16 + passwordBytes.length);
                System.arraycopy(passwordBytes, 0, temp, 16, passwordBytes.length);
                System.arraycopy(digester.digest(temp), 0, key, i * 16, 16);
            }
            return Arrays.copyOf(key, keyLength);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new byte[keyLength];
    }

    public static byte[] getCompressedBytes(int value) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((value >> 24) & 0xff);
        bytes[1] = (byte) ((value >> 16) & 0xff);
        bytes[2] = (byte) ((value >> 8) & 0xff);
        bytes[3] = (byte) (value & 0xff);
        if (bytes[0] == 0 && bytes[1] == 0 && bytes[2] == 0)
            return Arrays.copyOfRange(bytes, 3, 4);
        if (bytes[0] == 0 && bytes[1] == 0)
            return Arrays.copyOfRange(bytes, 2, 4);
        if (bytes[0] == 0)
            return Arrays.copyOfRange(bytes, 1, 4);
        return bytes;
    }

    public static byte[] getBytes(int length, int value) {
        if (length < 1 || length > 4)
            throw new RuntimeException("bad length");
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((value >> 24) & 0xff);
        bytes[1] = (byte) ((value >> 16) & 0xff);
        bytes[2] = (byte) ((value >> 8) & 0xff);
        bytes[3] = (byte) (value & 0xff);
        if (length == 4)
            return bytes;
        else
            return Arrays.copyOfRange(bytes, 4 - length, 4);
    }

    public static byte[] getBytes(int value) {
        return getBytes(4, value);
    }

    public static int toBigEndianInteger(byte[] bytes) {
        byte[] value = new byte[4];
        System.arraycopy(bytes, 0, value, 4 - bytes.length, bytes.length);
        return (value[0] & 0xff) << 24 | (value[1] & 0xff) << 16
                | (value[2] & 0xff) << 8 | (value[3] & 0xff);
    }

    public int getRandomIdentifier(int length) {
        int base = (int) Math.pow(10, length - 1);
        return randomizer.nextInt(base * 9) + base;
    }
}
