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

public abstract class PaddingWrapper extends Wrapper {
    protected int paddingThreshold;
    protected int paddingRange;
    protected int headerLength;

    // if size < threshold + range, then do padding,
    // no package will lower then threshold,
    // no package with padding will above threshold + range
    // +---------+-----+-------------+---------------------+------------+
    // |  size:  |  4  |  threshold  |  threshold + range  |  infinite  |
    // +---------+-----+-------------+---------------------+------------+
    // | option: | len |            with padding           | no padding |
    // +---------+-----+-----------------------------------+------------+
    public PaddingWrapper(int paddingThreshold, int paddingRange) {
        if (paddingThreshold < paddingRange || paddingRange < 4)
            throw new RuntimeException("bad padding range, 4 to threshold is accepted");
        this.paddingThreshold = paddingThreshold;
        this.paddingRange = paddingRange;
        this.headerLength = 4; // 4 bytes integer (default)
    }

}
