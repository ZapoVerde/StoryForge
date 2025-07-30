package com.example.storyforge.core

import android.content.Context
import org.json.JSONObject
import com.example.storyforge.R

object SecretsLoader {
    fun loadApiKey(context: Context): String {
        return try {
            val input = context.resources.openRawResource(R.raw.secrets)
            val json = input.bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            obj.getString("deepseek_api_key")
        } catch (e: Exception) {
            "MISSING_API_KEY"
        }
    }
}
