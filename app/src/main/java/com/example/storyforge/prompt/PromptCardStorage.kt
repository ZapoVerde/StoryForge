package com.example.storyforge.prompt

import android.content.Context
import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PromptCardStorage(private val context: Context) {

    private val fileName = "prompt_cards.json"
    private val file: File
        get() = File(context.filesDir, fileName)

    fun loadCards(): List<PromptCard> {
        if (!file.exists()) {
            Log.d("PromptCardStorage", "No saved file found, returning empty list")
            return emptyList()
        }
        return try {
            val json = file.readText()
            val cards = Json.decodeFromString<List<PromptCard>>(json)
            Log.d("PromptCardStorage", "Loaded ${cards.size} saved prompt cards")
            cards
        } catch (e: Exception) {
            Log.e("PromptCardStorage", "Failed to load saved cards", e)
            emptyList()
        }
    }

    fun saveCards(cards: List<PromptCard>) {
        try {
            val json = Json.encodeToString(cards)
            file.writeText(json)
            Log.d("PromptCardStorage", "Saved ${cards.size} cards to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("PromptCardStorage", "Failed to save cards", e)
        }
    }


    fun loadDefaultsIfEmpty(): List<PromptCard> {
        val existing = loadCards()
        if (existing.isNotEmpty()) return existing

        return try {
            val input = context.assets.open("prompt_cards.json").bufferedReader().use { it.readText() }
            val defaults = Json.decodeFromString<List<PromptCard>>(input)
            saveCards(defaults)
            defaults
        } catch (e: Exception) {
            Log.e("PromptCardStorage", "Failed to load default prompt cards from assets", e)
            emptyList()
        }
    }



}
