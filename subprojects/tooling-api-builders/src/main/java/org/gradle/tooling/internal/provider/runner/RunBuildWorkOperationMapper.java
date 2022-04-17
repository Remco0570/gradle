/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.tooling.internal.provider.runner;

import org.gradle.execution.RunBuildWorkBuildOperationType;
import org.gradle.internal.build.event.BuildEventSubscriptions;
import org.gradle.internal.build.event.types.DefaultOperationStartedProgressEvent;
import org.gradle.internal.build.event.types.DefaultRunBuildWorkDescriptor;
import org.gradle.internal.operations.BuildOperationCategory;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.OperationFinishEvent;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.internal.operations.OperationStartEvent;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.internal.protocol.events.InternalOperationFinishedProgressEvent;
import org.gradle.tooling.internal.protocol.events.InternalOperationStartedProgressEvent;

import javax.annotation.Nullable;

public class RunBuildWorkOperationMapper implements BuildOperationMapper<RunBuildWorkBuildOperationType.Details, DefaultRunBuildWorkDescriptor> {
    @Override
    public boolean isEnabled(BuildEventSubscriptions subscriptions) {
        return subscriptions.isRequested(OperationType.BUILD_PHASE);
    }

    @Override
    public Class<RunBuildWorkBuildOperationType.Details> getDetailsType() {
        return RunBuildWorkBuildOperationType.Details.class;
    }

    @Override
    public DefaultRunBuildWorkDescriptor createDescriptor(RunBuildWorkBuildOperationType.Details details, BuildOperationDescriptor buildOperation, @Nullable OperationIdentifier parent) {
        if (!(buildOperation.getMetadata() instanceof BuildOperationCategory)) {
            return null;
        }
        String buildPhase;
        switch ((BuildOperationCategory) buildOperation.getMetadata()) {
            case CONFIGURE_ROOT_BUILD:
            case CONFIGURE_BUILD:
            case CONFIGURE_PROJECT:
            case RUN_MAIN_TASKS:
            case RUN_WORK:
                buildPhase = buildOperation.getMetadata().toString();
                break;
            default:
                throw new IllegalStateException("Build operation category: " + buildOperation.getMetadata() + " is not supported by " + this.getClass().getName() + ".");
        }
        return new DefaultRunBuildWorkDescriptor(buildOperation, parent, buildPhase, buildOperation.getTotalProgress());
    }

    @Override
    public InternalOperationStartedProgressEvent createStartedEvent(DefaultRunBuildWorkDescriptor descriptor, RunBuildWorkBuildOperationType.Details details, OperationStartEvent startEvent) {
        return new DefaultOperationStartedProgressEvent(startEvent.getStartTime(), descriptor);
    }

    @Override
    public InternalOperationFinishedProgressEvent createFinishedEvent(DefaultRunBuildWorkDescriptor descriptor, RunBuildWorkBuildOperationType.Details details, OperationFinishEvent finishEvent) {
        return null;
    }
}
