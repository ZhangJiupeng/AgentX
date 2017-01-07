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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FrameWrapper extends Wrapper {
    private ByteArrayOutputStream wrapBuffer;
    private ByteArrayOutputStream unwrapBuffer;
    private List<byte[]> unwrapFrames;
    private int frameLength;
    private int reservedHeaderLength;
    private Wrapper frameHandler;


    /*
     * notice that the fixed-frame-length does not require a accurate
     * fixed data. It will work in a pure variable frame mode if this
     * parameter is set to a large number.
     */
    public FrameWrapper(int fixedFrameLength) {
        if (fixedFrameLength < 4) {
            throw new RuntimeException("bad fixed-frame length < 4");
        }
        this.wrapBuffer = new ByteArrayOutputStream();
        this.unwrapBuffer = new ByteArrayOutputStream();
        this.unwrapFrames = new ArrayList<>();
        this.frameLength = fixedFrameLength - 1;
        if (fixedFrameLength < 0xFF)
            reservedHeaderLength = 1;
        else if (fixedFrameLength < 0xFFFF - 2)
            reservedHeaderLength = 2;
        else if (fixedFrameLength < 0xFFFFFF - 3)
            reservedHeaderLength = 3;
        else
            reservedHeaderLength = 4;
    }

    /**
     * frameHandler will be invoked before the frame re-concat into
     * data stream, see unwrap().
     */
    public FrameWrapper(int fixedFrameLength, Wrapper frameHandler) {
        this(fixedFrameLength);
        this.frameHandler = frameHandler;
    }

    /*
     * data will be wrapped into several frames, but marked as a whole
     * chunk (we call it a data-package)
     */
    @Override
    public byte[] wrap(final byte[] bytes) {
        byte[] payload;
        if (frameHandler == null) {
            payload = bytes;
        } else {
            payload = frameHandler.wrap(bytes);
        }
        int i, nof = (payload.length / frameLength) + (payload.length % frameLength == 0 ? 0 : 1);
        for (i = 0; i < nof - 1; i++) {
            wrapBuffer.write(1);
            wrapBuffer.write(payload, i * frameLength, frameLength);
        }
        wrapBuffer.write(0);
        wrapBuffer.write(KeyHelper.getBytes(reservedHeaderLength, payload.length - i * frameLength), 0, reservedHeaderLength);
        wrapBuffer.write(payload, i * frameLength, payload.length - i * frameLength);
        byte[] wrappedBytes = wrapBuffer.toByteArray();
        wrapBuffer.reset();
        return wrappedBytes;
    }

    /*
     * notice that the subscribe below was outdated
     *
     * data will not completely return if the received data is incomplete,
     * it will be held into the unwrap-buffer until the rest part are received;
     * if the incoming data are concatenated, it will return a data-package a time.
     * if no complete data (includes unwrap-buffer) is received, return null.
     *
     * notice that the unwrap methods affects each other because of their
     * usages of the same unwrap-buffer, please use one of these methods
     * from beginning to end.
     */
    @Override
    public byte[] unwrap(final byte[] bytes) {
        byte[] ret;
        if (frameHandler == null) {
            ret = unwrapAll(bytes);
        } else {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            unwrapFrames(bytes).forEach(e -> unwrapFrames.add(frameHandler.unwrap(e)));
            unwrapFrames.forEach(e -> stream.write(e, 0, e.length));
            unwrapFrames.clear();
            ret = stream.toByteArray();
        }
        return ret.length > 0 ? ret : null;
        /*
        unwrapFrames(bytes).forEach(e -> unwrapFrames.add(e));
        if (unwrapFrames.size() > 0) {
            return unwrapFrames.remove(0);
        } else {
            return null;
        }
        */
    }

    /*
     * return all complete data-packages in a byte array, incomplete data will be sent
     * into buffer, two or more concatenated packs will be merged into one.
     *
     * notice that the unwrap methods affects each other because of their
     * usages of the same unwrap-buffer, please use one of these methods
     * from beginning to end.
     */
    public byte[] unwrapAll(byte[] bytes) {
        byte[] data = new byte[unwrapBuffer.size() + bytes.length];
        System.arraycopy(unwrapBuffer.toByteArray(), 0, data, 0, unwrapBuffer.size());
        System.arraycopy(bytes, 0, data, unwrapBuffer.size(), bytes.length);
        unwrapBuffer.reset();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int pos = 0, limit = pos + 1 + frameLength, size = data.length;
        while (pos < size) {
            byte[] buffer;
            switch (data[pos]) {
                case 0:
                    limit = Math.min(limit + 1 + frameLength + 1, size);
                    buffer = Arrays.copyOfRange(data, pos, limit);
                    if (buffer.length < 1 + reservedHeaderLength) {
                        unwrapBuffer.write(buffer, 0, buffer.length);
                        // frame-len was truncated
                        return outputStream.toByteArray();
                    }
                    int endFrameLength = KeyHelper.toBigEndianInteger(Arrays.copyOfRange(buffer, 1, 1 + reservedHeaderLength));
                    if (buffer.length < 1 + reservedHeaderLength + endFrameLength) {
                        unwrapBuffer.write(buffer, 0, buffer.length);
                        // data was truncated
                        return outputStream.toByteArray();
                    } else {
                        outputStream.write(buffer, 1 + reservedHeaderLength, endFrameLength);
                        pos += 1 + reservedHeaderLength + endFrameLength;
                        limit = pos + 1 + frameLength;
                    }
                    break;
                case 1:
                    buffer = Arrays.copyOfRange(data, pos, limit);
                    outputStream.write(buffer, 1, buffer.length - 1);
                    pos += 1 + frameLength;
                    limit = Math.min(limit + 1 + frameLength, data.length);
                    break;
                default:
                    throw new RuntimeException("unknown delimiter " + data[pos]);
            }
        }
        return outputStream.toByteArray();
    }

    /*
     * if the incoming data are concatenated, it will return all complete
     * data-package a time.
     *
     * notice that the unwrap methods affects each other because of their
     * usages of the same unwrap-buffer, please use one of these methods
     * from beginning to end.
     */
    public List<byte[]> unwrapFrames(byte[] bytes) {
        List<byte[]> ret = new ArrayList<>();

        byte[] data = new byte[unwrapBuffer.size() + bytes.length];
        System.arraycopy(unwrapBuffer.toByteArray(), 0, data, 0, unwrapBuffer.size());
        System.arraycopy(bytes, 0, data, unwrapBuffer.size(), bytes.length);
        unwrapBuffer.reset();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int pos = 0, limit = pos + 1 + frameLength, size = data.length;
        while (pos < size) {
            byte[] buffer;
            switch (data[pos]) {
                case 0:
                    limit = Math.min(limit + 1 + frameLength + 1, size);
                    buffer = Arrays.copyOfRange(data, pos, limit);
                    if (buffer.length < 1 + reservedHeaderLength) {
                        unwrapBuffer.write(buffer, 0, buffer.length);
                        // frame-len was truncated
                        return ret;
                    }
                    int endFrameLength = KeyHelper.toBigEndianInteger(Arrays.copyOfRange(buffer, 1, 1 + reservedHeaderLength));
                    if (buffer.length < 1 + reservedHeaderLength + endFrameLength) {
                        unwrapBuffer.write(buffer, 0, buffer.length);
                        // data was truncated
                        return ret;
                    } else {
                        outputStream.write(buffer, 1 + reservedHeaderLength, endFrameLength);
                        ret.add(outputStream.toByteArray());
                        outputStream.reset();
                        pos += 1 + reservedHeaderLength + endFrameLength;
                        limit = pos + 1 + frameLength;
                    }
                    break;
                case 1:
                    buffer = Arrays.copyOfRange(data, pos, limit);
                    outputStream.write(buffer, 1, buffer.length - 1);
                    pos += 1 + frameLength;
                    limit = Math.min(limit + 1 + frameLength, data.length);
                    break;
                default:
                    throw new RuntimeException("unknown delimiter " + data[pos]);
            }
        }
        return ret;
    }

}
