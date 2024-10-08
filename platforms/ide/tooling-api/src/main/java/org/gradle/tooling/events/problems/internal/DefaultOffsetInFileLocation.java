/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.tooling.events.problems.internal;

import org.gradle.tooling.events.problems.OffsetInFileLocation;

public class DefaultOffsetInFileLocation extends DefaultFileLocation implements OffsetInFileLocation {
    private final int offset;
    private final int length;

    public DefaultOffsetInFileLocation(String path, int offset, int length) {
        super(path);
        this.offset = offset;
        this.length = length;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public int getLength() {
        return length;
    }
}
