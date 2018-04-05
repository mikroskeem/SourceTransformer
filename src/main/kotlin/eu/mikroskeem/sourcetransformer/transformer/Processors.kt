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

import com.google.common.collect.ImmutableList
import net.techcable.srglib.FieldData
import net.techcable.srglib.JavaType
import net.techcable.srglib.MethodData
import net.techcable.srglib.MethodSignature
import net.techcable.srglib.mappings.ImmutableMappings
import spoon.processing.AbstractProcessor
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtField
import spoon.reflect.declaration.CtMethod
import java.util.WeakHashMap

/**
 * @author Mark Vainomaa
 */
private val classTypeCache = WeakHashMap<CtClass<*>, JavaType>()
private val fieldTypeCache = WeakHashMap<CtField<*>, FieldData>()
private val methodTypeCache = WeakHashMap<CtMethod<*>, MethodData>()

fun CtClass<*>.toJavaType(): JavaType =
        classTypeCache.computeIfAbsent(this) { JavaType.fromName(qualifiedName) }

fun CtField<*>.toFieldData(): FieldData =
        fieldTypeCache.computeIfAbsent(this) { FieldData.create((declaringType as CtClass<*>).toJavaType(), simpleName) }

val CtMethod<*>.srgParameters: MethodSignature get() = MethodSignature.create(
        ImmutableList.copyOf(parameters.map { JavaType.fromName(it.type.qualifiedName) }),
        JavaType.fromName(type.qualifiedName) // TODO: primitives
)

fun CtMethod<*>.toMethodData(): MethodData =
        methodTypeCache.computeIfAbsent(this) { MethodData.create((declaringType as CtClass<*>).toJavaType(), simpleName, srgParameters) }

class ClassProcessor(private val mappings: ImmutableMappings): AbstractProcessor<CtClass<Any?>>() {
    override fun process(element: CtClass<Any?>) {
        val `class` = element.toJavaType()
        val newClass = mappings.getNewType(`class`)

        if(`class` == newClass)
            return

        println("Mapping class: $`class` -> $newClass")

        // TODO: set package!
        element.setSimpleName<CtClass<Any?>>(newClass.simpleName)
    }
}

class FieldProcessor(private val mappings: ImmutableMappings): AbstractProcessor<CtField<Any?>>() {
    override fun process(element: CtField<Any?>) {
        val field = element.toFieldData()
        val newField = mappings.getNewField(field)

        if(field == newField)
            return

        println("Mapping field: $field -> $newField")

        //element.type = newField. // TODO: field type remapping
        element.setSimpleName<CtField<Any?>>(newField.name)
    }
}

class MethodProcessor(private val mappings: ImmutableMappings): AbstractProcessor<CtMethod<Any?>>() {
    override fun process(element: CtMethod<Any?>) {
        val method = element.toMethodData()
        val newMethod = mappings.getNewMethod(method)

        if(method == newMethod)
            return

        println("Mapping method: $method -> $newMethod")

        //element.setType<CtMethod<Any?>>(CtTypeReferenceImpl<T>(newMethod.returnType.name)) // TODO: method type remapping
        element.setSimpleName<CtMethod<Any?>>(newMethod.name)
    }
}