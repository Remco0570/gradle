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

package org.gradle.internal.enterprise.impl;

import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.StartParameterInternal;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector;
import org.gradle.api.invocation.Gradle;
import org.gradle.internal.service.scopes.Scopes;
import org.gradle.internal.service.scopes.ServiceScope;
import org.gradle.plugin.management.internal.DefaultPluginRequest;
import org.gradle.plugin.management.internal.PluginCoordinates;
import org.gradle.plugin.management.internal.PluginRequestInternal;
import org.gradle.plugin.management.internal.PluginRequests;
import org.gradle.plugin.management.internal.autoapply.AutoAppliedGradleEnterprisePlugin;
import org.gradle.plugin.management.internal.autoapply.AutoAppliedPluginRegistry;

import static org.gradle.initialization.StartParameterBuildOptions.BuildScanOption;
import static org.gradle.plugin.management.internal.PluginRequestInternal.Origin.AUTO_APPLIED;

@ServiceScope(Scopes.BuildTree.class)
public class GradleEnterpriseAutoAppliedPluginRegistry implements AutoAppliedPluginRegistry {

    @Override
    public PluginRequests getAutoAppliedPlugins(Project target) {
        return PluginRequests.EMPTY;
    }

    @Override
    public PluginRequests getAutoAppliedPlugins(Settings target) {
        if (((StartParameterInternal) target.getStartParameter()).isUseEmptySettings() || !shouldApplyGradleEnterprisePlugin(target)) {
            return PluginRequests.EMPTY;
        } else {
            return PluginRequests.of(createGradleEnterprisePluginRequest());
        }
    }

    private boolean shouldApplyGradleEnterprisePlugin(Settings settings) {
        Gradle gradle = settings.getGradle();
        StartParameter startParameter = gradle.getStartParameter();
        return startParameter.isBuildScan() && gradle.getParent() == null;
    }

    private PluginRequestInternal createGradleEnterprisePluginRequest() {
        ModuleIdentifier moduleIdentifier = DefaultModuleIdentifier.newId(AutoAppliedGradleEnterprisePlugin.GROUP, AutoAppliedGradleEnterprisePlugin.NAME);
        ModuleVersionSelector artifact = DefaultModuleVersionSelector.newSelector(moduleIdentifier, AutoAppliedGradleEnterprisePlugin.VERSION);
        return new DefaultPluginRequest(
            AutoAppliedGradleEnterprisePlugin.ID,
            AutoAppliedGradleEnterprisePlugin.VERSION,
            true,
            null,
            getScriptDisplayName(),
            artifact,
            null,
            AUTO_APPLIED,
            develocityPluginCoordinates()
        );
    }

    private static PluginCoordinates develocityPluginCoordinates() {
        ModuleIdentifier moduleIdentifier = DefaultModuleIdentifier.newId(AutoAppliedGradleEnterprisePlugin.GROUP, AutoAppliedGradleEnterprisePlugin.DEVELOCITY_PLUGIN_ARTIFACT_NAME);
        ModuleVersionSelector artifact = DefaultModuleVersionSelector.newSelector(moduleIdentifier, AutoAppliedGradleEnterprisePlugin.VERSION);
        return new PluginCoordinates(AutoAppliedGradleEnterprisePlugin.DEVELOCITY_PLUGIN_ID, artifact);
    }

    private static String getScriptDisplayName() {
        return String.format("auto-applied by using --%s", BuildScanOption.LONG_OPTION);
    }
}
