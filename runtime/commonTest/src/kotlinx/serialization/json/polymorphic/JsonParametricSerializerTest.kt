/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.reflect.*
import kotlin.test.*

class JsonParametricSerializerTest : JsonTestBase() {

    @Serializable
    sealed class Choices {
        @Serializable
        data class HasA(val a: String) : Choices()

        @Serializable
        data class HasB(val b: Int) : Choices()

        @Serializable
        data class HasC(val c: Boolean) : Choices()
    }

    object ChoicesParametricSerializer : JsonParametricSerializer<Choices>() {
        override val baseClass: KClass<Choices>
            get() = Choices::class

        override fun selectSerializer(element: JsonElement): KSerializer<out Choices> {
            val element = element.jsonObject
            return when {
                "a" in element -> Choices.HasA.serializer()
                "b" in element -> Choices.HasB.serializer()
                "c" in element -> Choices.HasC.serializer()
                else -> throw SerializationException("Unknown choice")
            }
        }
    }

    @Serializable
    data class WithChoices(@Serializable(ChoicesParametricSerializer::class) val response: Choices)

    private val testDataInput = listOf(
        """{"response": {"a":"string"}}""",
        """{"response": {"b":42}}""",
        """{"response": {"c":true}}"""
    )

    private val testDataOutput = listOf(
        WithChoices(Choices.HasA("string")),
        WithChoices(Choices.HasB(42)),
        WithChoices(Choices.HasC(true))
    )

    val json = Json(JsonConfiguration.Default)

    @Test
    fun testParsesParametrically() = parametrizedTest { streaming ->
        for (i in testDataInput.indices) {
            assertEquals(
                testDataOutput[i],
                json.parse(WithChoices.serializer(), testDataInput[i], streaming),
                "failed test on ${testDataInput[i]}, useStreaming = $streaming"
            )
        }
    }

}
