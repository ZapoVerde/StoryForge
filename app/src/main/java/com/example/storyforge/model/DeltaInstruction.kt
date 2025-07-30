// file: com/example/storyforge/model/DeltaInstruction.kt

package com.example.storyforge.model

import kotlinx.serialization.json.*

@kotlinx.serialization.Serializable
@kotlinx.serialization.Polymorphic
sealed class DeltaInstruction {

    abstract fun toLogJsonElement(): JsonElement

    data class Add(val key: String, val value: JsonElement) : DeltaInstruction() {
        override fun toLogJsonElement(): JsonElement = buildJsonObject {
            put("op", JsonPrimitive("add"))
            put("key", JsonPrimitive(key))
            put("value", value)
        }
    }

    data class Assign(val key: String, val value: JsonElement) : DeltaInstruction() {
        override fun toLogJsonElement(): JsonElement = buildJsonObject {
            put("op", JsonPrimitive("assign"))
            put("key", JsonPrimitive(key))
            put("value", value)
        }
    }

    data class Declare(val key: String, val value: JsonElement) : DeltaInstruction() {
        override fun toLogJsonElement(): JsonElement = buildJsonObject {
            put("op", JsonPrimitive("declare"))
            put("key", JsonPrimitive(key))
            put("value", value)
        }
    }

    data class Delete(val key: String) : DeltaInstruction() {
        override fun toLogJsonElement(): JsonElement = buildJsonObject {
            put("op", JsonPrimitive("delete"))
            put("key", JsonPrimitive(key))
        }
    }

    fun toLogValue(): Any = when (this) {
        is Add -> mapOf("+" to value)
        is Assign -> mapOf("@" to value)
        is Declare -> mapOf("!" to value)
        is Delete -> "-"
    }

    companion object {
        fun fromJsonElement(key: String, json: JsonElement): DeltaInstruction? {
            val op = key.firstOrNull() ?: return null
            val body = key.drop(1)
            val (target, variable) = body.split(".", limit = 2).takeIf { it.size == 2 } ?: return null

            return when (op) {
                '+' -> Add(variable, json)
                '=' -> Assign(variable, json)
                '!' -> Declare(variable, json)
                '-' -> Delete(variable)
                else -> null
            }
        }
    }
}
