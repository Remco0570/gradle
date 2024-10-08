/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.internal.tasks.userinput;

import java.util.Collection;

public class NonInteractiveUserInputHandler implements UserInputHandler {
    @Override
    public Boolean askYesNoQuestion(String question) {
        return null;
    }

    @Override
    public boolean askYesNoQuestion(String question, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public <T> T selectOption(String question, Collection<T> options, T defaultOption) {
        return defaultOption;
    }

    @Override
    public String askQuestion(String question, String defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean interrupted() {
        return false;
    }
}
