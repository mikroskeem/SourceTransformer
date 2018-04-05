/*
 * This file is part of project SourceTransformer, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018 Mark Vainomaa <mikroskeem@mikroskeem.eu>
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package eu.mikroskeem.sourcetransformer

import eu.mikroskeem.sourcetransformer.transformer.CodeTransformer
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.task
import java.io.File

/**
 * Source code transformer plugin
 *
 * @author Mark Vainomaa
 */
open class SourceTransformer: Plugin<Project> {
    override fun apply(project: Project) {
        // Check if Java plugin exists
        if(!project.plugins.hasPlugin(JavaBasePlugin::class.java))
            throw IllegalStateException("SourceTransformer plugin is useful only on Java projects")

        val transformerDirections =
                project.extensions.create("sourceTransformer", SourceTransformerExtension::class.java)

        project.run {
            val transformTask = task<SourceTransformerTask>("transformSources") {
                doFirst {
                    transformerDirections.validate()
                }
            }
        }
    }
}

open class SourceTransformerExtension {
    var mappingsFile: File? = null
    var sourceDir: File? = null
    var targetDir: File? = null

    internal fun validate() {
        if(mappingsFile == null || !mappingsFile!!.isFile)
            throw IllegalStateException("Mappings are not either present or not provided")

        if(sourceDir == null || !sourceDir!!.isDirectory)
            throw IllegalStateException("Source directory is not present or is not a directory")

        if(targetDir == null || !targetDir!!.isDirectory)
            throw IllegalStateException("Target directory is not present or is not a directory")
    }
}

open class SourceTransformerTask: DefaultTask() {
    @TaskAction
    fun transformSources() {
        val directions = project.extensions.getByType(SourceTransformerExtension::class.java)
        val transformer = directions.run { CodeTransformer(sourceDir!!, targetDir!!, mappingsFile!!) }

        transformer.process()
    }
}
