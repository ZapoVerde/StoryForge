package com.example.storyforge.core

import kotlinx.serialization.json.*

object TagValidator {

    data class TagIssue(val path: String, val issue: String)

    /**
     * Scan the worldState for tag field issues:
     * - Missing tag
     * - Malformed (no @/# prefix)
     * - Duplicate tag
     */
    fun validateTags(worldState: JsonObject): List<TagIssue> {
        val seenTags = mutableMapOf<String, String>() // tag -> full path
        val issues = mutableListOf<TagIssue>()

        for ((category, entityMap) in worldState) {
            val entities = entityMap as? JsonObject ?: continue
            for ((entity, obj) in entities) {
                val path = "$category.$entity"
                val jsonObj = obj as? JsonObject ?: continue
                val tag = jsonObj["tag"]?.jsonPrimitive?.contentOrNull


                when {
                    tag == null -> issues += TagIssue(path, "Missing tag field")
                    tag.isBlank() -> issues += TagIssue(path, "Empty tag field")
                    !(tag.startsWith("@") || tag.startsWith("#")) ->
                        issues += TagIssue(path, "Tag missing prefix (@ or #): '$tag'")
                    seenTags.containsKey(tag) ->
                        issues += TagIssue(path, "Duplicate tag '$tag' (already used by ${seenTags[tag]})")
                    else -> seenTags[tag] = path
                }
            }
        }

        return issues
    }
}
