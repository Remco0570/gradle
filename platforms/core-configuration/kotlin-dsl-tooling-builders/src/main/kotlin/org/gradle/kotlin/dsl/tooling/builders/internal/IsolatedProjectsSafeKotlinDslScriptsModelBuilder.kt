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

package org.gradle.kotlin.dsl.tooling.builders.internal

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectHierarchyUtils
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.classpath.ClassPath
import org.gradle.kotlin.dsl.accessors.AccessorsClassPath
import org.gradle.kotlin.dsl.provider.ClassPathModeExceptionCollector
import org.gradle.kotlin.dsl.provider.ignoringErrors
import org.gradle.kotlin.dsl.resolver.SourceDistributionResolver
import org.gradle.kotlin.dsl.resolver.SourcePathProvider
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.tooling.builders.AbstractKotlinDslScriptsModelBuilder
import org.gradle.kotlin.dsl.tooling.builders.KotlinDslScriptsParameter
import org.gradle.kotlin.dsl.tooling.builders.StandardKotlinDslScriptModel
import org.gradle.kotlin.dsl.tooling.builders.StandardKotlinDslScriptsModel
import org.gradle.kotlin.dsl.tooling.builders.accessorsClassPathOf
import org.gradle.kotlin.dsl.tooling.builders.compilationClassPathForScriptPluginOf
import org.gradle.kotlin.dsl.tooling.builders.discoverBuildScript
import org.gradle.kotlin.dsl.tooling.builders.discoverInitScripts
import org.gradle.kotlin.dsl.tooling.builders.discoverPrecompiledScriptPluginScripts
import org.gradle.kotlin.dsl.tooling.builders.discoverSettingScript
import org.gradle.kotlin.dsl.tooling.builders.resolveCorrelationIdParameter
import org.gradle.kotlin.dsl.tooling.builders.runtimeFailuresLocatedIn
import org.gradle.kotlin.dsl.tooling.builders.scriptCompilationClassPath
import org.gradle.kotlin.dsl.tooling.builders.scriptHandlerFactoryOf
import org.gradle.kotlin.dsl.tooling.builders.scriptImplicitImports
import org.gradle.kotlin.dsl.tooling.builders.settings
import org.gradle.kotlin.dsl.tooling.builders.sourcePathFor
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptModel
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder
import org.gradle.tooling.provider.model.internal.IntermediateToolingModelProvider
import java.io.File


internal
class IsolatedProjectsSafeKotlinDslScriptsModelBuilder(
    private val intermediateModelProvider: IntermediateToolingModelProvider
) : AbstractKotlinDslScriptsModelBuilder() {

    override fun prepareParameter(rootProject: Project): KotlinDslScriptsParameter {
        require(rootProject.findProperty(KotlinDslScriptsModel.SCRIPTS_GRADLE_PROPERTY_NAME) == null) {
            "Property ${KotlinDslScriptsModel.SCRIPTS_GRADLE_PROPERTY_NAME} is not supported with Isolated Projects"
        }

        return KotlinDslScriptsParameter(rootProject.resolveCorrelationIdParameter(), emptyList())
    }

    override fun buildFor(parameter: KotlinDslScriptsParameter, rootProject: Project): KotlinDslScriptsModel {
        return buildFor(rootProject as ProjectInternal)
    }

    private
    fun buildFor(rootProject: ProjectInternal): StandardKotlinDslScriptsModel {
        val intermediateScriptModels = mutableListOf<IntermediateScriptModel>()
        intermediateScriptModels += initScriptModels(rootProject)
        intermediateScriptModels += listOfNotNull(settingsScriptModels(rootProject))
        intermediateScriptModels += projectHierarchyScriptModels(rootProject)

        val exceptions = rootProject.serviceOf<ClassPathModeExceptionCollector>().exceptions

        val scriptModels = intermediateScriptModels.associateBy({ it.scriptFile }) {
            buildModel(it, exceptions)
        }

        return StandardKotlinDslScriptsModel.from(scriptModels)
    }

    private
    fun projectHierarchyScriptModels(rootProject: ProjectInternal): List<IntermediateScriptModel> {
        return projectHierarchyScriptModels(rootProject, getRootParameter(), intermediateModelProvider)
    }

    private
    fun getRootParameter() =
        // TODO: root classpaths should not be empty
        DefaultIsolatedScriptModelParameter(ClassPath.EMPTY, ClassPath.EMPTY)

    private
    fun buildModel(midModel: IntermediateScriptModel, exceptions: List<Exception>): KotlinDslScriptModel {
        return StandardKotlinDslScriptModel(
            classPath = emptyList(),
            sourcePath = emptyList(),
            implicitImports = emptyList(),
            editorReports = emptyList(),
            exceptions = getExceptionsForFile(midModel.scriptFile, exceptions)
        )
    }

    private
    fun getExceptionsForFile(scriptFile: File, exceptions: List<Exception>): List<String> =
        exceptions.asSequence().runtimeFailuresLocatedIn(scriptFile.path).map(::exceptionToString).toList()

    private
    fun exceptionToString(exception: Exception) = exception.stackTraceToString()
}


private
fun initScriptModels(rootProject: ProjectInternal): List<IntermediateScriptModel> {
    return rootProject.discoverInitScripts().map {
        initScriptModelBuilder(it, rootProject).buildModel()
    }
}


private
fun settingsScriptModels(rootProject: ProjectInternal): IntermediateScriptModel? {
    return rootProject.discoverSettingScript()?.let {
        settingsScriptModelBuilder(it, rootProject).buildModel()
    }
}


private
fun initScriptModelBuilder(scriptFile: File, project: ProjectInternal) = project.run {

    val (scriptHandler, scriptClassPath) = compilationClassPathForScriptPluginOf(
        target = gradle,
        scriptFile = scriptFile,
        baseScope = gradle.classLoaderScope,
        scriptHandlerFactory = scriptHandlerFactoryOf(gradle),
        project = project,
        resourceDescription = "initialization script"
    )

    IntermediateScriptModelBuilder(
        scriptFile = scriptFile,
        project = project,
        scriptClassPath = scriptClassPath,
        scriptSourcePath = sourcePathFor(listOf(scriptHandler))
    )
}


private
fun settingsScriptModelBuilder(scriptFile: File, project: Project) = project.run {

    IntermediateScriptModelBuilder(
        scriptFile = scriptFile,
        project = project,
        scriptClassPath = settings.scriptCompilationClassPath,
        scriptSourcePath = sourcePathFor(listOf(settings.buildscript))
    )
}


internal
data class IntermediateScriptModel(
    val scriptFile: File,
    val classPath: ClassPath,
    val sourcePath: ClassPath,
    val implicitImports: List<String>,
)


private
data class IntermediateScriptModelBuilder(
    val scriptFile: File,
    val project: Project,
    val scriptClassPath: ClassPath,
    val accessorsClassPath: (ClassPath) -> AccessorsClassPath = { AccessorsClassPath.empty },
    val scriptSourcePath: ClassPath,
    val additionalImports: () -> List<String> = { emptyList() }
) {

    fun buildModel(): IntermediateScriptModel {
        val classPathModeExceptionCollector = project.serviceOf<ClassPathModeExceptionCollector>()
        val accessorsClassPath =
            classPathModeExceptionCollector.ignoringErrors {
                accessorsClassPath(scriptClassPath)
            } ?: AccessorsClassPath.empty

        val additionalImports =
            classPathModeExceptionCollector.ignoringErrors {
                additionalImports()
            } ?: emptyList()

        val classPath = (scriptClassPath + accessorsClassPath.bin)
        val sourcePath = (gradleSource() + scriptSourcePath + accessorsClassPath.src)
        val implicitImports = project.scriptImplicitImports + additionalImports
        return IntermediateScriptModel(scriptFile, classPath, sourcePath, implicitImports)
    }

    private
    fun gradleSource() =
        SourcePathProvider.sourcePathFor(
            scriptClassPath,
            scriptFile,
            project.rootDir,
            project.gradle.gradleHomeDir,
            SourceDistributionResolver(project)
        )
}


internal
interface IsolatedScriptsModelParameter {
    val parentClassPath: ClassPath
    val parentSourcePath: ClassPath
}


internal
data class DefaultIsolatedScriptModelParameter(
    override val parentClassPath: ClassPath,
    override val parentSourcePath: ClassPath
) : IsolatedScriptsModelParameter


internal
data class IsolatedScriptsModel(
    val models: List<IntermediateScriptModel>
)


internal
class IsolatedScriptsModelBuilder(
    private val intermediateModelProvider: IntermediateToolingModelProvider
) : ParameterizedToolingModelBuilder<IsolatedScriptsModelParameter> {

    override fun getParameterType() = IsolatedScriptsModelParameter::class.java

    override fun canBuild(modelName: String): Boolean =
        modelName == "org.gradle.kotlin.dsl.tooling.builders.internal.IsolatedScriptsModel"

    override fun buildAll(modelName: String, project: Project): IsolatedScriptsModel {
        error("Calling $this without parameter is not supported")
    }

    override fun buildAll(modelName: String, parameter: IsolatedScriptsModelParameter, project: Project): IsolatedScriptsModel {
        val models = projectHierarchyScriptModels(project as ProjectInternal, parameter, intermediateModelProvider)
        return IsolatedScriptsModel(models)
    }
}


private
fun projectHierarchyScriptModels(
    project: ProjectInternal,
    parameter: IsolatedScriptsModelParameter,
    intermediateModelProvider: IntermediateToolingModelProvider
): List<IntermediateScriptModel> {

    val models = mutableListOf<IntermediateScriptModel>()

    // TODO: compute own classpaths
//    scriptClassPath = project.scriptCompilationClassPath,
//    scriptSourcePath = sourcePathFor(sourceLookupScriptHandlersFor(project))
    val ownClassPath = parameter.parentClassPath
    val ownSourcePath = parameter.parentSourcePath

    models += listOfNotNull(project.discoverBuildScript()?.let {
        buildScriptModelBuilder(it, project, ownClassPath, ownSourcePath).buildModel()
    })

    models += project.discoverPrecompiledScriptPluginScripts().map {
        // TODO:isolated support fully
        IntermediateScriptModel(it, parameter.parentClassPath, parameter.parentSourcePath, emptyList())
    }

    val childrenParameter = DefaultIsolatedScriptModelParameter(ownClassPath, ownSourcePath)
    val children = ProjectHierarchyUtils.getChildProjectsForInternalUse(project).toList()
    models += intermediateModelProvider
        .getModels(project, children, IsolatedScriptsModel::class.java, childrenParameter)
        .flatMap { it.models }

    return models
}


private
fun buildScriptModelBuilder(
    scriptFile: File,
    project: ProjectInternal,
    scriptClassPath: ClassPath,
    scriptSourcePath: ClassPath
) = IntermediateScriptModelBuilder(
    scriptFile = scriptFile,
    project = project,
    scriptClassPath,
    accessorsClassPath = { project.accessorsClassPathOf(it) },
    scriptSourcePath
)
