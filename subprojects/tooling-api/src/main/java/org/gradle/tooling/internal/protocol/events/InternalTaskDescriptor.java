/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.tooling.internal.protocol.events;

import org.gradle.tooling.internal.protocol.InternalProtocolInterface;

/**
 * DO NOT CHANGE THIS INTERFACE. It is part of the cross-version protocol.
 *
 * @since 2.5
 */
public interface InternalTaskDescriptor extends InternalProtocolInterface {

    /**
     * Returns the id that uniquely identifies the task.
     *
     * @return The unique id of the test, never null
     */
    Object getId();

    /**
     * Returns the name of the task.
     *
     * @return The name of the task, never null
     */
    String getName();

    /**
     * Returns a human consumable display name for the task.
     */
    String getDisplayName();

    /**
     * Returns the path of the task.
     *
     * @return the path of the task.
     */
    String getTaskPath();

    /**
     * Returns the id of the parent of this task, if any.
     *
     * @return The id of the parent of this task, can be null
     */
    Object getParentId();

}
