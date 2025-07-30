// file: com/example/storyforge/StoryForgeViewModel.kt
package com.example.storyforge

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyforge.core.*
import com.example.storyforge.logging.*
import com.example.storyforge.model.*
import com.example.storyforge.prompt.*
import com.example.storyforge.save.*
import com.example.storyforge.settings.*
import com.example.storyforge.stack.*
import com.example.storyforge.ui.screens.flattenJsonObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.time.Instant

class StoryForgeViewModel(
    private val narratorFactory: () -> Narrator,
    private val _settings: Settings,
    private val promptCardStorage: PromptCardStorage,
    val appContext: Context
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    private val _promptCards = MutableStateFlow<List<PromptCard>>(emptyList())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isProcessing = MutableStateFlow(false)
    private val _worldChangeMessage = MutableStateFlow<String?>(null)
    private val _pinnedKeys = MutableStateFlow<Set<String>>(emptySet())
    private val _activePromptCard = MutableStateFlow<PromptCard?>(null)
    private val _useDummyNarrator = MutableStateFlow(false)
    private val _aiConnections = MutableStateFlow(_settings.aiConnections)
    private val _turnLogEntries = MutableStateFlow<List<TurnLogEntry>>(emptyList())
    private val _selectedLogViews = MutableStateFlow<List<LogViewMode>>(
        listOf(
            LogViewMode.RAW, LogViewMode.DIGEST, LogViewMode.DELTAS,
            LogViewMode.CONTEXT, LogViewMode.TOKENS, LogViewMode.SETTINGS,
            LogViewMode.ERRORS, LogViewMode.API
        )
    )
    private val _narratorUiState = MutableStateFlow(NarratorUiState())
    private val _turns = MutableStateFlow<List<ConversationTurn>>(emptyList())
    private val _stackLogs = MutableStateFlow<List<StackLogEntry>>(emptyList())

    val turns = _turns.asStateFlow()
    val stackLogs = _stackLogs.asStateFlow()
    val narratorUiState = _narratorUiState.asStateFlow()
    val turnLogEntries = _turnLogEntries.asStateFlow()
    val selectedLogViews = _selectedLogViews.asStateFlow()
    val aiConnections = _aiConnections.asStateFlow()
    val activePromptCard = _activePromptCard.asStateFlow()
    val gameState = _gameState.asStateFlow()
    val promptCards = _promptCards.asStateFlow()
    val errorMessage = _errorMessage.asStateFlow()
    val isProcessing = _isProcessing.asStateFlow()
    val worldChangeMessage = _worldChangeMessage.asStateFlow()
    val pinnedKeys = _pinnedKeys.asStateFlow()
    val useDummyNarrator = _useDummyNarrator.asStateFlow()
    val settings: Settings get() = _settings

    init {
        _promptCards.value = promptCardStorage.loadDefaultsIfEmpty()

        viewModelScope.launch {
            GameStateStorage.loadSnapshot(appContext)?.let { loadSnapshot(it) }
        }
    }

    fun processAction(action: String) {
        if (action.isBlank()) {
            _errorMessage.value = "Action cannot be empty"
            return
        }

        val sendTurnId = _turns.value.size
        _turns.value += ConversationTurn(user = action, narrator = "")

        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null

            val narrator = resolveNarrator()
            val promptCard = _activePromptCard.value ?: return@launch
            val connectionId = promptCard.aiSettings.selectedConnectionId
            val connection = _settings.aiConnections.find { it.id == connectionId }
            if (connection == null) {
                _errorMessage.value = "No AI connection found"
                _isProcessing.value = false
                return@launch
            }

            val stackInput = StackAssemblerInput(
                turnNumber = sendTurnId,
                userMessage = action,
                stackInstructions = StackInstructionsLoader.fromJson(promptCard.stackInstructions),
                aiPrompt = promptCard.prompt,
                emitSkeleton = promptCard.emitSkeleton,
                gameRules = promptCard.gameRules,
                firstTurnOnlyBlock = promptCard.firstTurnOnlyBlock,
                turnHistory = _turns.value.map { ConversationTurn(it.user, it.narrator) },
                digestLines = DigestManager.getAllLines(),
                expressionLog = emptyMap(),
                worldState = _gameState.value.worldState,
                sceneTags = SceneManager.getSceneTags(_gameState.value.worldState).toSet()
            )

            val assembledMessages = StackAssembler.assemble(stackInput)

            val request = ChatCompletionRequest(
                model = connection.modelSlug,
                messages = assembledMessages,
                temperature = promptCard.aiSettings.temperature,
                top_p = promptCard.aiSettings.topP,
                max_tokens = promptCard.aiSettings.maxTokens,
                presence_penalty = promptCard.aiSettings.presencePenalty,
                frequency_penalty = promptCard.aiSettings.frequencyPenalty,
                stream = false
            )

            _stackLogs.update { it + StackLogEntry(
                turn = sendTurnId,
                timestamp = Instant.now().toString(),
                model = connection.modelSlug,
                stack = assembledMessages,
                token_summary = TokenSummary(
                    input = assembledMessages.sumOf { it.content.length / 4 },
                    output = promptCard.aiSettings.maxTokens,
                    total = assembledMessages.sumOf { it.content.length / 4 } + promptCard.aiSettings.maxTokens
                ),
                latency_ms = 0L
            ) }

            val requestJson = Json.encodeToString(request.copy(messages = emptyList()))
            val apiUrl = connection.apiUrl.trim().ensureEndsWithSlash() + "chat/completions"

            val preLogEntry = TurnLogAssembler.assemble(
                turnNumber = sendTurnId,
                userInput = action,
                rawNarratorOutput = "",
                parsedDigest = null,
                parsedDeltas = null,
                contextSnapshot = assembledMessages.joinToString("\n\n") { "${it.role}: ${it.content}" },
                tokenUsage = null,
                aiSettings = promptCard.aiSettings,
                errorFlags = emptyList(),
                apiRequestBody = requestJson,
                apiResponseBody = "",
                apiUrl = apiUrl,
                latencyMs = 0L,
                modelSlugUsed = connection.modelSlug
            )
            appendTurnLog(preLogEntry)

            val requestForLog = narrator.generateFull(
                messages = assembledMessages,
                settings = promptCard.aiSettings,
                modelName = connection.modelName,
                turnId = sendTurnId,
                context = appContext
            )

            requestForLog.onSuccess { narratorResult ->
                val narration = narratorResult.narration
                val deltas = narratorResult.deltas

                val taggedDeltas = deltas.mapValues { (key, instruction) ->
                    if (instruction is DeltaInstruction.Declare) {
                        val pathParts = instruction.key.split(".")
                        if (pathParts.size >= 2) {
                            val (category, _) = pathParts
                            val valueObj = instruction.value as? JsonObject ?: return@mapValues instruction
                            if (!valueObj.containsKey("tag")) {
                                val inferredTag = when (category) {
                                    "npcs", "entities" -> "character"
                                    "locations", "places" -> "location"
                                    else -> null
                                }
                                if (inferredTag != null) {
                                    val patched = valueObj.toMutableMap()
                                    patched["tag"] = JsonPrimitive(inferredTag)
                                    return@mapValues DeltaInstruction.Declare(instruction.key, JsonObject(patched))
                                }
                            }
                        }
                    }
                    instruction
                }

                val enrichedProse = narration
                _turns.value += ConversationTurn(user = "", narrator = enrichedProse)

                _gameState.value.applyDeltas(taggedDeltas)
                WorldStateLog.append(appContext, sendTurnId + 1, taggedDeltas)

                val snapshot = buildSnapshot()
                GameStateStorage.saveSnapshot(appContext, snapshot)

                val parsed = NarrationParser.extractJsonAndCleanNarration(narration)
                val digest = parsed.digestLines.firstOrNull()

                val receiveLog = TurnLogAssembler.assemble(
                    turnNumber = sendTurnId + 1,
                    userInput = "",
                    rawNarratorOutput = enrichedProse,
                    parsedDigest = digest?.let { listOf(it) },
                    parsedDeltas = deltas,
                    contextSnapshot = null,
                    tokenUsage = null,
                    aiSettings = promptCard.aiSettings,
                    errorFlags = emptyList(),
                    apiRequestBody = "",
                    apiResponseBody = narratorResult.apiResponseBody,
                    apiUrl = narratorResult.apiUrl,
                    latencyMs = narratorResult.latencyMs,
                    modelSlugUsed = narratorResult.modelSlugUsed
                )
                appendTurnLog(receiveLog)

            }.onFailure { ex ->
                _errorMessage.value = "Narration failed: ${ex.localizedMessage}"
                ex.printStackTrace()
            }

            _isProcessing.value = false
        }
    }

    fun togglePin(prefixKey: String) {
        Log.d("PIN_DEBUG", "Toggling pin for: $prefixKey")

        val flatWorld = flattenJsonObject(_gameState.value.worldState)
        val affectedKeys = flatWorld.keys.filter { it.startsWith(prefixKey) }

        if (affectedKeys.isEmpty()) {
            Log.w("PIN_DEBUG", "No keys matched for $prefixKey")
            return
        }

        val currentlyPinned = _pinnedKeys.value
        val isAdding = affectedKeys.any { it !in currentlyPinned }

        val newSet = if (isAdding) {
            currentlyPinned + affectedKeys
        } else {
            currentlyPinned - affectedKeys.toSet()
        }

        _pinnedKeys.value = newSet
        Log.d("PIN_DEBUG", "Updated pinned keys: $newSet")
    }



    private fun buildSnapshot(): GameSnapshot {
        return GameSnapshot(
            promptCard = _activePromptCard.value!!,
            gameState = _gameState.value,
            turns = _turns.value,
            digestLines = DigestManager.getAllLines(),
            worldDeltas = WorldStateLog.readAll(appContext),
            turnLogs = _turnLogEntries.value,
            stackLogs = _stackLogs.value,
            sceneState = SceneManager.getSnapshot(),
            timestamp = Instant.now().toString(),
            narratorUiState = _narratorUiState.value
        )
    }

    fun setActivePromptCard(card: PromptCard) {
        if (card.title.trim().isEmpty()) {
            Log.w("PromptCard", "Refused to activate card — missing title")
            return
        }

        _activePromptCard.value = card

        val initBlock = card.worldStateInit.trim()
        if (initBlock.isNotBlank()) {
            try {
                val parsed = Json.parseToJsonElement(initBlock).jsonObject
                val allValid = parsed.values.all { top ->
                    top is JsonObject && top.values.all { it is JsonObject }
                }

                if (!allValid) {
                    Log.w("PromptCard", "Refused to load worldStateInit — not a 3-level object")
                    return
                }

                _gameState.value = _gameState.value.copy(worldState = parsed)

            } catch (e: Exception) {
                Log.e("PromptCard", "Failed to parse structured worldStateInit", e)
            }
        }
    }

    fun resetSession(newState: GameState = GameState()) {
        _gameState.value = newState
        _turns.value = emptyList()
        _errorMessage.value = null
        _isProcessing.value = false
    }

    fun saveToSlot(promptCardName: String) {
        GameStateSlotStorage.saveSlot(appContext, _gameState.value, promptCardName)
    }

    fun addPromptCard(card: PromptCard) {
        _promptCards.update { current ->
            val updated = current.toMutableList()
            val index = updated.indexOfFirst { it.id == card.id }

            if (index != -1) {
                updated[index] = card  // ✅ update in-place
            } else {
                updated.add(card)      // ✅ append new
            }

            promptCardStorage.saveCards(updated)
            updated
        }
    }

    fun setUseDummyNarrator(enabled: Boolean) {
        _useDummyNarrator.value = enabled
    }

    fun isUsingDummyNarrator(): Boolean {
        return _useDummyNarrator.value
    }

    fun deletePromptCard(id: String) {
        _promptCards.update { current ->
            val updated = current.filterNot { it.id == id }
            promptCardStorage.saveCards(updated)
            updated
        }
    }

    fun loadSnapshot(snapshot: GameSnapshot) {
        _activePromptCard.value = snapshot.promptCard
        _gameState.value = snapshot.gameState
        _turns.value = snapshot.turns
        _turnLogEntries.value = snapshot.turnLogs
        _narratorUiState.value = snapshot.narratorUiState
        _stackLogs.value = snapshot.stackLogs
        SceneManager.setFromSnapshot(snapshot.sceneState)
        DigestManager.setFromSnapshot(snapshot.digestLines)
        WorldStateLog.setAll(snapshot.worldDeltas)
    }

    fun setSelectedLogViews(list: List<LogViewMode>) {
        _selectedLogViews.value = list
    }

    fun appendStackLog(log: StackLogEntry) {
        _stackLogs.update { it + log }
    }

    fun clearStackLogs() {
        _stackLogs.value = emptyList()
    }

    fun updateNarratorUiState(update: NarratorUiState) {
        _narratorUiState.value = update
    }

    fun appendTurnLog(entry: TurnLogEntry) {
        _turnLogEntries.update { current -> current + entry }
    }

    fun updateAiConnections(newList: List<AiConnection>) {
        _aiConnections.value = newList
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun resolveNarrator(): Narrator {
        if (_useDummyNarrator.value) return DummyNarrator()

        val promptCard = _activePromptCard.value ?: return DummyNarrator()
        val connectionId = promptCard.aiSettings.selectedConnectionId
        val connection = _settings.aiConnections.find { it.id == connectionId }
        if (connection == null) {
            Log.w("Narrator", "No connection found for ID: $connectionId")
        }

        return try {
            if (connection == null || !connection.apiUrl.trim().startsWith("http")) {
                DummyNarrator()
            } else {
                AINarrator.fromConnection(connection)
            }
        } catch (e: Exception) {
            DummyNarrator()
        }
    }

    // --- Add these missing functions back into your StoryForgeViewModel ---

    fun GameState.buildMessageList(action: String): List<Message> {
        val priorTurns = turns.value // Assuming 'turns' is the StateFlow<List<ConversationTurn>>

        val history = priorTurns.flatMap {
            listOf(
                Message(role = "user", content = it.user),
                Message(role = "assistant", content = it.narrator)
            )
        }
        return history + Message(role = "user", content = action)
    }

    fun deleteWorldCategory(category: String) {
        val updatedWorldState = _gameState.value.worldState.toMutableMap()
        updatedWorldState.remove(category)
        _gameState.update {
            it.copy(worldState = JsonObject(updatedWorldState))
        }
        // Consider if you need to save the GameState snapshot here
        // GameStateStorage.save(appContext, _gameState.value) // If using direct save
        // or trigger a full snapshot save if appropriate
    }

    fun deleteWorldKey(fullKey: String) {
        val segments = fullKey.split(".")
        if (segments.size < 2) return // Need at least a category and a key

        val worldStateMap = _gameState.value.worldState.toMutableMap()

        var currentLevelMap: MutableMap<String, JsonElement> = worldStateMap
        for (i in 0 until segments.size - 1) {
            val segment = segments[i]
            when (val element = currentLevelMap[segment]) {
                is JsonObject -> {
                    val nextLevelMutableMap = element.toMutableMap()
                    currentLevelMap[segment] = JsonObject(nextLevelMutableMap) // Update with mutable copy
                    currentLevelMap = nextLevelMutableMap
                }
                else -> {
                    Log.w("VM_WORLD_EDIT", "Path segment '$segment' in '$fullKey' is not an object or does not exist.")
                    return // Path is invalid or segment not found
                }
            }
        }

        currentLevelMap.remove(segments.last())

        _gameState.update {
            it.copy(worldState = JsonObject(worldStateMap))
        }
        // Consider saving snapshot
    }


    fun editWorldKey(fullKey: String, newValue: JsonElement) {
        val segments = fullKey.split(".")
        if (segments.isEmpty()) return

        val originalWorldState = _gameState.value.worldState
        val updatedWorldStateMap = originalWorldState.toMutableMap()
        var currentLevelMap: MutableMap<String, JsonElement> = updatedWorldStateMap

        for (i in 0 until segments.size - 1) {
            val segment = segments[i]
            when (val element = currentLevelMap[segment]) {
                is JsonObject -> {
                    val nextLevelMutableMap = element.toMutableMap()
                    currentLevelMap[segment] = JsonObject(nextLevelMutableMap) // Update with mutable copy
                    currentLevelMap = nextLevelMutableMap
                }
                else -> {
                    Log.w("VM_WORLD_EDIT", "Path segment '$segment' in '$fullKey' for edit is not an object or does not exist.")
                    return // Path is invalid or segment not found
                }
            }
        }

        currentLevelMap[segments.last()] = newValue
        _gameState.update {
            it.copy(worldState = JsonObject(updatedWorldStateMap))
        }
        // Consider saving snapshot
    }

    fun deleteEntity(category: String, entity: String, level: Int = 2) {
        val worldStateMap = _gameState.value.worldState.toMutableMap()

        when (level) {
            1 -> { // Top-level category
                (worldStateMap[category] as? JsonObject)?.toMutableMap()?.let { categoryMap ->
                    categoryMap.remove(entity)
                    worldStateMap[category] = JsonObject(categoryMap)
                } ?: run {
                    Log.w("VM_DELETE_ENTITY", "Category '$category' not found or not an object for level 1 delete.")
                    return
                }
            }
            2 -> { // Nested under "entities" or similar structure
                (worldStateMap["entities"] as? JsonObject)?.toMutableMap()?.let { entitiesMap ->
                    (entitiesMap[category] as? JsonObject)?.toMutableMap()?.let { categoryMap ->
                        categoryMap.remove(entity)
                        entitiesMap[category] = JsonObject(categoryMap)
                        worldStateMap["entities"] = JsonObject(entitiesMap)
                    } ?: run {
                        Log.w("VM_DELETE_ENTITY", "Sub-category '$category' under 'entities' not found or not an object.")
                        return
                    }
                } ?: run {
                    Log.w("VM_DELETE_ENTITY", "'entities' category not found or not an object for level 2 delete.")
                    return
                }
            }
            else -> {
                Log.w("VM_DELETE_ENTITY", "Invalid level '$level' for deleteEntity.")
                return
            }
        }

        _gameState.update {
            it.copy(worldState = JsonObject(worldStateMap))
        }
        // Consider saving snapshot
    }

    fun renameEntity(category: String, oldName: String, newName: String) {
        if (oldName == newName || newName.isBlank()) return

        val worldStateMap = _gameState.value.worldState.toMutableMap()
        val categoryMap = (worldStateMap[category] as? JsonObject)?.toMutableMap() ?: run {
            Log.w("VM_RENAME_ENTITY", "Category '$category' not found or not an object.")
            return
        }

        val entityValue = categoryMap[oldName] ?: run {
            Log.w("VM_RENAME_ENTITY", "Entity '$oldName' not found in category '$category'.")
            return
        }

        if (categoryMap.containsKey(newName)) {
            Log.w("VM_RENAME_ENTITY", "New entity name '$newName' already exists in category '$category'.")
            return
        }

        categoryMap.remove(oldName)
        categoryMap[newName] = entityValue
        worldStateMap[category] = JsonObject(categoryMap)

        _gameState.update {
            it.copy(worldState = JsonObject(worldStateMap))
        }

        // Update pinned keys
        val oldPrefix = "$category.$oldName."
        val newPrefix = "$category.$newName."
        _pinnedKeys.update { currentKeys ->
            currentKeys.map { key ->
                if (key.startsWith(oldPrefix)) newPrefix + key.removePrefix(oldPrefix) else key
            }.toSet()
        }
        // Consider saving snapshot
    }

    fun renameCategory(oldName: String, newName: String) {
        if (oldName == newName || newName.isBlank()) return

        val worldStateMap = _gameState.value.worldState.toMutableMap()

        if (!worldStateMap.containsKey(oldName)) {
            Log.w("VM_RENAME_CATEGORY", "Old category name '$oldName' not found.")
            return
        }
        if (worldStateMap.containsKey(newName)) {
            Log.w("VM_RENAME_CATEGORY", "New category name '$newName' already exists.")
            return
        }

        val categoryValue = worldStateMap.remove(oldName)!! // remove and get value
        worldStateMap[newName] = categoryValue

        _gameState.update {
            it.copy(worldState = JsonObject(worldStateMap))
        }

        // Update pinned keys
        val oldPrefix = "$oldName."
        val newPrefix = "$newName."
        _pinnedKeys.update { currentKeys ->
            currentKeys.map { key ->
                if (key.startsWith(oldPrefix)) newPrefix + key.removePrefix(oldPrefix) else key
            }.toSet()
        }
        // Consider saving snapshot
    }

    // --- End of added functions ---





    // All other ViewModel methods remain unchanged.
}
