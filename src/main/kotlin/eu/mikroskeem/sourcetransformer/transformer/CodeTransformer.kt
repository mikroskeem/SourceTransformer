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

package eu.mikroskeem.sourcetransformer.transformer

import net.techcable.srglib.SrgLib
import net.techcable.srglib.format.MappingsFormat
import net.techcable.srglib.mappings.ImmutableMappings
import net.techcable.srglib.mappings.Mappings
import org.gradle.api.JavaVersion
import spoon.Launcher
import spoon.processing.AbstractProcessor
import spoon.reflect.CtModel
import java.io.File
import java.io.FileReader
import kotlin.reflect.full.primaryConstructor

/**
 * @author Mark Vainomaa
 */
class CodeTransformer(private val sourceDirectory: File, private val targetDirectory: File, private val mappingsFile: File) {
    private val spoon: Launcher
    private val mappings: Mappings
    private val immutableMappings: ImmutableMappings

    init {
        if(!sourceDirectory.isDirectory)
            throw IllegalStateException("'$sourceDirectory' is not a directory!")

        // Set up mappings
        try {
            mappings = FileReader(mappingsFile).use(MappingsFormat.SEARGE_FORMAT::parse)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load mappings", e)
        }

        try {
            SrgLib.checkConsistency(mappings)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to validate mappings consistency", e)
        }

        // create snapshot of mappings
        immutableMappings = mappings.snapshot()

        // Set up launcher
        spoon = Launcher().apply {
            addInputResource("$sourceDirectory")
            setSourceOutputDirectory(targetDirectory)

            // Set up environment
            environment.complianceLevel = JavaVersion.current().ordinal
            environment.noClasspath = true
            environment.setShouldCompile(false)
            environment.setCommentEnabled(true)

            // TODO: Causes even `private String a` to become `private java.lang.String a` >.<
            environment.isAutoImports = false

            // Add processors
            addProcessor<ClassProcessor>()
            addProcessor<FieldProcessor>()
            addProcessor<MethodProcessor>()
        }
    }

    fun process() {
        spoon.run()
    }

    private inline fun <reified T: AbstractProcessor<*>> Launcher.addProcessor() {
        addProcessor(T::class.primaryConstructor!!.call(immutableMappings))
    }
}
