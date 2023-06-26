/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.process;

import java.util.Collections;

/**
 * Provides arguments to a process.
 *
 * @since 4.6
 */
public interface CommandLineArgumentProvider {
    /**
     * The arguments which will be provided to the process.
     */
    Iterable<String> asArguments();

    /**
     * An argument provider that provides no arguments.
     *
     * @since 8.4
     */
    CommandLineArgumentProvider NONE = new CommandLineArgumentProvider() {
        @Override
        public Iterable<String> asArguments() {
            return Collections.emptyList();
        }
    };
}
