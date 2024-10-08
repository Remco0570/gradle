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

package org.gradle.plugin.management.internal;

import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.plugin.management.PluginRequest;
import org.gradle.plugin.use.PluginId;
import org.gradle.plugin.use.internal.DefaultPluginId;

import javax.annotation.Nullable;
import java.util.Optional;

public class DefaultPluginRequest implements PluginRequestInternal {

    private final PluginId id;
    private final String version;
    private final boolean apply;
    private final Integer lineNumber;
    private final String scriptDisplayName;
    private final ModuleVersionSelector artifact;
    private final PluginRequest originalRequest;
    private final Origin origin;
    private final PluginCoordinates alternativeCoordinates;

    public DefaultPluginRequest(PluginId id, String version, boolean apply, Integer lineNumber, ScriptSource scriptSource) {
        this(id, version, apply, lineNumber, scriptSource.getDisplayName(), null);
    }

    public DefaultPluginRequest(String id, String version, boolean apply, Integer lineNumber, String scriptDisplayName) {
        this(DefaultPluginId.of(id), version, apply, lineNumber, scriptDisplayName, null);
    }

    public DefaultPluginRequest(PluginId id, String version, boolean apply, Integer lineNumber, String scriptDisplayName, ModuleVersionSelector artifact) {
        this(id, version, apply, lineNumber, scriptDisplayName, artifact, null, Origin.OTHER, null);
    }

    public DefaultPluginRequest(
        PluginId id,
        String version,
        boolean apply,
        Integer lineNumber,
        String scriptDisplayName,
        ModuleVersionSelector artifact,
        PluginRequest originalRequest,
        Origin origin,
        PluginCoordinates alternativeCoordinates
    ) {
        this.id = id;
        this.version = version;
        this.apply = apply;
        this.lineNumber = lineNumber;
        this.scriptDisplayName = scriptDisplayName;
        this.artifact = artifact;
        this.originalRequest = originalRequest != null ? originalRequest : this;
        this.origin = origin;
        this.alternativeCoordinates = alternativeCoordinates;
    }

    @Override
    public PluginId getId() {
        return id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Nullable
    @Override
    public ModuleVersionSelector getModule() {
        return artifact;
    }

    @Override
    public boolean isApply() {
        return apply;
    }

    @Override
    public Integer getLineNumber() {
        return lineNumber;
    }

    @Override
    public String getScriptDisplayName() {
        return scriptDisplayName;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("[id: '").append(id).append("'");
        if (version != null) {
            b.append(", version: '").append(version).append("'");
        }
        if (artifact != null) {
            b.append(", artifact: '").append(artifact).append("'");
        }
        if (!apply) {
            b.append(", apply: false");
        }

        b.append("]");
        return b.toString();
    }

    @Override
    public String getDisplayName() {
        return toString();
    }

    @Override
    public PluginRequest getOriginalRequest() {
        return originalRequest;
    }

    @Override
    public Origin getOrigin() {
        return origin;
    }

    @Override
    public Optional<PluginCoordinates> getAlternativeCoordinates() {
        return Optional.ofNullable(alternativeCoordinates);
    }
}
