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

import cc.agentx.util.KeyHelper;

import java.util.Arrays;

public class RandomPaddingWrapper extends PaddingWrapper {
    public RandomPaddingWrapper(int paddingThreshold, int paddingRange) {
        super(paddingThreshold, paddingRange);
        if (paddingThreshold + paddingRange < 0xFF - 1)
            headerLength = 1;
        else if (paddingThreshold + paddingRange < 0xFFFF - 2)
            headerLength = 2;
        else if (paddingThreshold + paddingRange < 0xFFFFFF - 3)
            headerLength = 3;
    }

    @Override
    public byte[] wrap(byte[] bytes) {
        if (bytes.length < paddingThreshold + paddingRange) {
            int randomLength = KeyHelper.generateRandomInteger(
                    Math.max(paddingThreshold, bytes.length)
                    , paddingThreshold + paddingRange
            ) + headerLength;
            byte[] wrapBytes = KeyHelper.generateRandomBytes(randomLength);
            byte[] headerBytes = KeyHelper.getBytes(headerLength, randomLength - bytes.length);
            System.arraycopy(bytes, 0, wrapBytes, wrapBytes.length - bytes.length, bytes.length);
            System.arraycopy(headerBytes, 0, wrapBytes, 0, headerBytes.length);
            return wrapBytes;
        } else {
            byte[] wrapBytes = KeyHelper.generateRandomBytes(headerLength + bytes.length);
            System.arraycopy(bytes, 0, wrapBytes, headerLength, bytes.length);
            System.arraycopy(KeyHelper.getBytes(headerLength, headerLength), 0, wrapBytes, 0, headerLength);
            return wrapBytes;
        }
    }

    @Override
    public byte[] unwrap(byte[] bytes) {
        int paddingSize = KeyHelper.toBigEndianInteger(Arrays.copyOfRange(bytes, 0, headerLength));
        return Arrays.copyOfRange(bytes, paddingSize, bytes.length);
    }
}
