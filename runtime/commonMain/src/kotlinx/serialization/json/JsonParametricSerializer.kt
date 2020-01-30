/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*

abstract class JsonParametricSerializer<T : Any> : KSerializer<T> {
    abstract val baseClass: KClass<T>

    override fun serialize(encoder: Encoder, value: T) {
        val actualSerializer =
            encoder.context.getPolymorphic(baseClass, value) ?: throwSubtypeNotRegistered(value::class, baseClass)
        (actualSerializer as KSerializer<T>).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): T {
        val input = decoder as? JsonInput
                ?: throw SerializationException("JsonParametricSerializer can be used only with Json decoders")
        val tree = input.decodeJson()
        val actualSerializer = selectSerializer(tree) as KSerializer<T>
        return input.json.fromJson(actualSerializer, tree)
    }

    override val descriptor: SerialDescriptor =
        NamedDescriptor("JsonParametricSerializer[${baseClass.simpleName()}]", PolymorphicKind.SEALED)

    protected abstract fun selectSerializer(element: JsonElement): KSerializer<out T>
}


