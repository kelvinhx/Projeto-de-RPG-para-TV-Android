package com.example.rpg

import android.app.Application
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

sealed class AudioState {
    object Idle : AudioState()
    object Recording : AudioState()
    object Transcribing : AudioState()
    object ProcessingTurn : AudioState()
    object SpeakingNarrator : AudioState()
}

class GameViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val saveDao = database.gameSaveDao()
    private val geminiRepository = GeminiRepository()
    private val audioRecorder = AudioRecorder(context)

    // Current full state of the RPG engine
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Status on audio recorder/speech
    private val _audioState = MutableStateFlow<AudioState>(AudioState.Idle)
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    // Temporary info about transcribed input
    private val _transcription = MutableStateFlow<String>("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    // Errors or alerts for UI dialog
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // TextToSpeech fallback
    private var nativeTts: TextToSpeech? = null
    private var ttsInitialized = false

    // Media player for Gemini generated TTS
    private var mediaPlayer: MediaPlayer? = null

    // Moshi instance for JSON parsing
    private val moshi: Moshi = RetrofitClient.moshiInstance

    init {
        // Initialize Native Android TTS fallback
        nativeTts = TextToSpeech(context, this)

        // Observe local database save slot 1 to load saved state automatically if it exists!
        viewModelScope.launch {
            saveDao.getSaveById(1).collectLatest { gameSave ->
                if (gameSave != null) {
                    try {
                        val adapter = moshi.adapter(GameState::class.java)
                        val loadedState = adapter.fromJson(gameSave.stateJson)
                        if (loadedState != null) {
                            _gameState.value = loadedState
                            Log.d("GameViewModel", "Successfully loaded saved game from Room db.")
                        }
                    } catch (e: Exception) {
                        Log.e("GameViewModel", "Failed to deserialize state on startup: ${e.message}")
                    }
                } else {
                    // Initialize with a welcoming log history entry
                    initializeFreshGame()
                }
            }
        }
    }

    private fun initializeFreshGame() {
        val welcomeLogs = listOf(
            LogEntry(
                speaker = "Narrador",
                message = "Bem-vindo a 'WhatIsRPG? O Eco da Podridão' — Uma simulação de RPG viva projetada para Android TV.\n\nControle a história falando com o microfone do seu controle remoto ou selecionando as ações na tela.\n\nPara iniciar a criação de personagem e começar a jogar, diga ou digite:\n'Vamos começar a jornada'"
            )
        )
        _gameState.value = GameState(
            creationStep = "NOT_STARTED",
            history = welcomeLogs,
            options = listOf("Vamos começar a jornada")
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = nativeTts?.setLanguage(Locale("pt", "BR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsInitialized = true
                Log.d("GameViewModel", "Native Text-To-Speech initialized in Brazilian Portuguese.")
            } else {
                Log.e("GameViewModel", "Portuguese language is not supported or missing speech data on this TV device.")
            }
        } else {
            Log.e("GameViewModel", "Failed to initialize native TextToSpeech.")
        }
    }

    // Dismiss current error toast
    fun clearError() {
        _errorMessage.value = null
    }

    // Manual typing action input fallback
    fun enterCommandLine(text: String) {
        if (text.isBlank()) return
        stopSpokenNarration()
        viewModelScope.launch {
            handlePlayerAction(text)
        }
    }

    // Toggle speech recording button action (Remote OK Click helper)
    fun toggleVoiceRecording() {
        if (_audioState.value == AudioState.Recording) {
            stopVoiceAndTranscribe()
        } else {
            stopSpokenNarration()
            val started = audioRecorder.startRecording {
                // When 1.5s silence is detected, automatically stop and transcribe
                if (_audioState.value == AudioState.Recording) {
                    stopVoiceAndTranscribe()
                }
            }
            if (started) {
                _audioState.value = AudioState.Recording
            } else {
                _errorMessage.value = "Não foi possível acessar o microfone. Verifique as permissões do app."
            }
        }
    }

    private fun stopVoiceAndTranscribe() {
        _audioState.value = AudioState.Transcribing
        val wavFile = audioRecorder.stopRecording()
        if (wavFile == null || !wavFile.exists() || wavFile.length() <= 0) {
            _audioState.value = AudioState.Idle
            _errorMessage.value = "Áudio não gravado ou muito curto. Tente novamente."
            return
        }

        viewModelScope.launch {
            try {
                val audioBytes = withContext(Dispatchers.IO) { wavFile.readBytes() }
                val transcribedText = geminiRepository.transcribeAudio(audioBytes)
                
                _transcription.value = transcribedText
                if (transcribedText.isNotBlank()) {
                    handlePlayerAction(transcribedText)
                } else {
                    _audioState.value = AudioState.Idle
                    _errorMessage.value = "Não foi possível compreender o áudio. Tente falar mais perto ou digite a ação."
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Transcription error: ${e.message}")
                _audioState.value = AudioState.Idle
                _errorMessage.value = "Erro ao transcrever áudio: ${e.message}"
            }
        }
    }

    // Process player action based on character creation workflow or general GM running loop
    private suspend fun handlePlayerAction(rawAction: String) {
        val action = rawAction.trim()
        val current = _gameState.value
        
        // Feed action to current history
        val updatedHistory = current.history.toMutableList()
        updatedHistory.add(LogEntry(speaker = "Jogador", message = action))
        _gameState.value = current.copy(history = updatedHistory)

        _audioState.value = AudioState.ProcessingTurn

        val step = current.creationStep

        // 1. Check if user wants to restart or reset the game
        if (action.lowercase(Locale.getDefault()).contains("reiniciar jogo") || 
            action.lowercase(Locale.getDefault()).contains("resetar jogo")) {
            resetGame()
            return
        }

        // 2. Character creation sequence machine
        when (step) {
            "NOT_STARTED" -> {
                if (action.lowercase(Locale.getDefault()).contains("vamos comecar a jornada") ||
                    action.lowercase(Locale.getDefault()).contains("vamos começar a jornada") ||
                    action.lowercase(Locale.getDefault()).contains("começar") ||
                    action.lowercase(Locale.getDefault()).contains("iniciar")) {
                    
                    val textStr = "Muito bem. O véu do tempo se rasga e as estrelas sussurram sua chegada.\n\nPara forjar seu destino no mundo sombrio de WhatIsRPG, diga ou digite:\nQual será o NOME do seu personagem?"
                    speakAndAdvanceState(
                        text = textStr,
                        nextStep = "NOME",
                        tempOptions = emptyList() // User must type or speak their name
                    )
                } else {
                    val fallbackStr = "Comando não reconhecido. Por favor, diga ou digite 'Vamos começar a jornada' para iniciar a aventura classic!"
                    speakAndAdvanceState(
                        text = fallbackStr,
                        nextStep = "NOT_STARTED",
                        tempOptions = listOf("Vamos começar a jornada")
                    )
                }
            }
            "NOME" -> {
                val nextText = "O nome '$action' reverbera pelos salões ancestrais.\n\nAgora, digite ou descreva a APARÊNCIA FÍSICA de seu herói (como cor da pele, estilo e tom de cabelo, olhos e altura)."
                _gameState.value = _gameState.value.copy(
                    playerState = _gameState.value.playerState.copy(name = action)
                )
                speakAndAdvanceState(nextText, "APARENCIA", emptyList())
            }
            "APARENCIA" -> {
                val nextText = "Uma figura marcante se delineia sob a luz lunar.\n\nQual é o GÊNERO do seu personagem?"
                _gameState.value = _gameState.value.copy(
                    playerState = _gameState.value.playerState.copy(appearance = action)
                )
                speakAndAdvanceState(nextText, "GENERO", listOf("Feminino", "Masculino", "Não-Binário"))
            }
            "GENERO" -> {
                val nextText = "Entendido.\n\nPor favor, defina a SEXUALIDADE de seu herói (ex: Heterossexual, Homossexual, Bissexual, etc)."
                _gameState.value = _gameState.value.copy(
                    playerState = _gameState.value.playerState.copy(gender = action)
                )
                speakAndAdvanceState(nextText, "SEXUALIDADE", listOf("Heterossexual", "Homossexual", "Bissexual", "Pansexual"))
            }
            "SEXUALIDADE" -> {
                val nextText = "Registrado sob os anais de marfim.\n\nEscolha uma das RAÇAS iniciais:\n\n- Elfo Negro (afinidade com sombra, gravidade e marés, +2 INT, +1 AGI)\n- Humano (alta adaptabilidade, +1 em todos os atributos)\n- Anão de Ferro (resistência pesada, +2 VIT, +1 FOR)"
                _gameState.value = _gameState.value.copy(
                    playerState = _gameState.value.playerState.copy(sexuality = action)
                )
                speakAndAdvanceState(nextText, "RACE", listOf("Elfo Negro", "Humano", "Anão de Ferro"))
            }
            "RACE" -> {
                val raceChosen = action
                val initialAttribs = when {
                    raceChosen.contains("elfo", ignoreCase = true) -> Attributes(8, 11, 12, 9, 10, 10)
                    raceChosen.contains("anão", ignoreCase = true) || raceChosen.contains("anao", ignoreCase = true) -> Attributes(12, 8, 8, 12, 10, 10)
                    else -> Attributes(10, 10, 10, 10, 10, 10)
                }
                val nextText = "Uma linhagem digna.\n\nAgora defina a sua CLASSE inicial:\n- Guerreiro (espadas, escudos, força bruta)\n- Mago (magias celestes, feitiços gravitacionais, intelecto)\n- Híbrido (uma mescla equilibrada de força e ocultismo)"
                _gameState.value = _gameState.value.copy(
                    playerState = _gameState.value.playerState.copy(race = raceChosen, attributes = initialAttribs)
                )
                speakAndAdvanceState(nextText, "CLASS", listOf("Guerreiro", "Mago", "Híbrido"))
            }
            "CLASS" -> {
                val chosenClass = action
                val inventory = when {
                    chosenClass.contains("guerreiro", ignoreCase = true) -> listOf(
                        Item("Espada de Bronze", "Uma espada robusta porém rústica.", "Weapon", "+5 Força", 20),
                        Item("Escudo de Madeira", "Oferece proteção básica.", "Armor", "+3 Vitalidade", 15),
                        Item("Poção de Cura", "Restaura 30 HP.", "Consumable", "+30 HP", 10)
                    )
                    chosenClass.contains("mago", ignoreCase = true) -> listOf(
                        Item("Cajado Sombrio", "Cajado simples de salgueiro negro.", "Weapon", "+5 Inteligência", 25),
                        Item("Manto de Seda Estelar", "Umedecido em poeira lunar.", "Armor", "+2 Inteligência", 20),
                        Item("Frasco de Mana", "Restaura 20 MP.", "Consumable", "+20 MP", 10)
                    )
                    else -> listOf(
                        Item("Adaga de Prata", "Ágil e perigosa.", "Weapon", "+4 Agilidade", 22),
                        Item("Armadura de Couro", "Versátil e flexível.", "Armor", "+2 Agilidade", 18),
                        Item("Poção de Cura", "Restaura 30 HP.", "Consumable", "+30 HP", 10)
                    )
                }

                val nextText = "Excelente. Para refinar seus poderes mágica ou físicos rústicos, selecione a sua SUBCLASSE (especialização):\n\n- Guerreiro Gravitacional (suas espadas criam peso extra lunar)\n- Mago Sombrio Lunar (feitiços focados em ilusões, marés e sombras celestes)\n- Híbrido Arcano (um guerreiro ágil que encanta suas lâminas com energia gravitacional)"
                _gameState.value = _gameState.value.copy(
                    playerState = _gameState.value.playerState.copy(
                        className = chosenClass,
                        inventory = inventory,
                        unassignedPoints = 5
                    )
                )
                speakAndAdvanceState(nextText, "SUBCLASS", listOf("Guerreiro Gravitacional", "Mago Sombrio Lunar", "Híbrido Arcano"))
            }
            "SUBCLASS" -> {
                val subclass = action
                val starterGrimoire = when {
                    subclass.contains("lunar", ignoreCase = true) -> listOf(
                        Skill("Luz de Prata", "Dispara um raio gélido lunar. Custo: 10 MP", 10, "Lunar Magic", 18),
                        Skill("Maré de Sombras", "Obscurece a visão de inimigos. Custo: 15 MP", 15, "Lunar Magic", 5)
                    )
                    subclass.contains("gravitacional", ignoreCase = true) -> listOf(
                        Skill("Impacto Sísmico", "Bate no chão alterando a gravidade para atrasar turnos. Custo: 12 MP", 12, "Gravity Magic", 20),
                        Skill("Escudo Magnético", "Atrai metais para se proteger de projéteis. Custo: 8 MP", 8, "Gravity Magic", 0)
                    )
                    else -> listOf(
                        Skill("Corte Crepuscular", "Usa sombras para estender o golpe de ferro. Custo: 8 MP", 8, "Mixed", 15),
                        Skill("Passo Lunar", "Avança no espaço em uma velocidade fantasma. Custo: 10 MP", 10, "Mixed", 5)
                    )
                }

                _gameState.value = _gameState.value.copy(
                    creationStep = "RUNNING",
                    playerState = _gameState.value.playerState.copy(
                        subclass = subclass,
                        grimoire = starterGrimoire,
                        unassignedPoints = 0
                    ),
                    options = listOf("Explorar arredores", "Inspecionar grimório", "Verificar inventário")
                )

                // Complete creation! Now invoke Gemini GM Narrative turn to start the actual adventure world!
                invokeGameMasterTurn("Vamos começar a jornada oficial. Apresente o início clássico do RPG nesta floresta sombria e misteriosa.")
            }
            "RUNNING" -> {
                // Game is fully running, send state JSON to Gemini for simulation
                invokeGameMasterTurn(action)
            }
        }
    }

    private fun speakAndAdvanceState(text: String, nextStep: String, tempOptions: List<String>) {
        val current = _gameState.value
        val updatedHistory = current.history.toMutableList()
        updatedHistory.add(LogEntry(speaker = "Narrador", message = text))

        _gameState.value = current.copy(
            creationStep = nextStep,
            history = updatedHistory,
            options = tempOptions
        )
        _audioState.value = AudioState.Idle
        speakOutLoud(text)
        saveCurrentGame()
    }

    private fun invokeGameMasterTurn(playerAction: String) {
        val current = _gameState.value
        viewModelScope.launch {
            try {
                // Serialize current state to JSON string using moshi
                val adapter = moshi.adapter(GameState::class.java)
                val stateJson = withContext(Dispatchers.Default) { adapter.toJson(current) }

                val systemPrompt = buildSystemPrompt()
                val response = geminiRepository.processTurn(stateJson, playerAction, systemPrompt)

                if (response != null) {
                    processGMResponse(response)
                } else {
                    _audioState.value = AudioState.Idle
                    _errorMessage.value = "Sem resposta do Mestre da Masmorra. Tente outro comando ou verifique a internet."
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "GM Turn calling error: ${e.message}", e)
                _audioState.value = AudioState.Idle
                _errorMessage.value = "Algum erro ocorreu com a Inteligência do Mestre: ${e.message}"
            }
        }
    }

    private fun processGMResponse(gm: GMResponse) {
        val current = _gameState.value
        val updatedHistory = current.history.toMutableList()
        updatedHistory.add(LogEntry(speaker = "Narrador", message = gm.narrative))

        // Update player state with diff packet generated by GM to keep stats persistent
        var newPlayer = current.playerState
        val pu = gm.playerUpdate
        if (pu != null) {
            newPlayer = newPlayer.copy(
                level = pu.level ?: newPlayer.level,
                experience = pu.experience ?: newPlayer.experience,
                unassignedPoints = pu.unassignedPoints ?: newPlayer.unassignedPoints,
                hp = (pu.hp ?: newPlayer.hp).coerceIn(0, pu.maxHp ?: newPlayer.maxHp),
                maxHp = pu.maxHp ?: newPlayer.maxHp,
                mp = (pu.mp ?: newPlayer.mp).coerceIn(0, pu.maxMp ?: newPlayer.maxMp),
                maxMp = pu.maxMp ?: newPlayer.maxMp,
                gold = pu.gold ?: newPlayer.gold,
                attributes = Attributes(
                    strength = pu.strength ?: newPlayer.attributes.strength,
                    agility = pu.agility ?: newPlayer.attributes.agility,
                    intelligence = pu.intelligence ?: newPlayer.attributes.intelligence,
                    vitality = pu.vitality ?: newPlayer.attributes.vitality,
                    perception = pu.perception ?: newPlayer.attributes.perception,
                    willpower = pu.willpower ?: newPlayer.attributes.willpower
                )
            )

            // Modify inventory if item list received
            val inventoryMutable = newPlayer.inventory.toMutableList()
            pu.itemsGained?.let { inventoryMutable.addAll(it) }
            pu.itemsLost?.let { lostNames ->
                lostNames.forEach { name ->
                    val foundIdx = inventoryMutable.indexOfFirst { it.name.contains(name, ignoreCase = true) }
                    if (foundIdx != -1) {
                        inventoryMutable.removeAt(foundIdx)
                    }
                }
            }
            
            // Add grimoire skills
            val grimoireMutable = newPlayer.grimoire.toMutableList()
            pu.skillsGained?.let { grimoireMutable.addAll(it) }

            // Titles and Scars
            val titlesMutable = newPlayer.titles.toMutableList()
            pu.titlesGained?.let { titlesMutable.addAll(it) }
            val scarsMutable = newPlayer.scars.toMutableList()
            pu.scarsGained?.let { scarsMutable.addAll(it) }

            newPlayer = newPlayer.copy(
                inventory = inventoryMutable,
                grimoire = grimoireMutable,
                titles = titlesMutable,
                scars = scarsMutable
            )
        }

        // Update World State
        var newWorld = current.worldState
        val wu = gm.worldUpdate
        if (wu != null) {
            var activeQuestsMutable = newWorld.activeQuests.toMutableList()
            wu.questEvents?.let { activeQuestsMutable.addAll(it) }

            newWorld = newWorld.copy(
                region = wu.region ?: newWorld.region,
                timeOfDay = wu.timeOfDay ?: newWorld.timeOfDay,
                rotLevel = (wu.rotLevel ?: newWorld.rotLevel).coerceIn(0, 100),
                locationDescription = wu.locationDescription ?: newWorld.locationDescription,
                activeQuests = activeQuestsMutable
            )
        }

        // Update NPCs list based on updates or keep
        val npcsMutable = current.npcs.toMutableList()
        val nu = gm.npcUpdates
        if (nu != null) {
            nu.forEach { item ->
                val existingIdx = npcsMutable.indexOfFirst { it.name.equals(item.name, ignoreCase = true) }
                if (existingIdx != -1) {
                    val npc = npcsMutable[existingIdx]
                    val newMemory = npc.memory.toMutableList()
                    item.memoryAddition?.let { newMemory.add(it) }
                    npcsMutable[existingIdx] = npc.copy(
                        description = item.description ?: npc.description,
                        affinity = (npc.affinity + (item.affinityChange ?: 0)).coerceIn(-100, 100),
                        emotion = item.emotion ?: npc.emotion,
                        memory = newMemory
                    )
                } else {
                    // Create newly encountered NPC
                    val firstMem = mutableListOf<String>()
                    item.memoryAddition?.let { firstMem.add(it) }
                    npcsMutable.add(
                        NpcState(
                            name = item.name,
                            description = item.description ?: "Um habitante local misterioso do Reino Sombrio.",
                            affinity = item.affinityChange ?: 0,
                            emotion = item.emotion ?: "Neutro",
                            memory = firstMem,
                            activeRole = "Encontrado"
                        )
                    )
                }
            }
        }

        _gameState.value = current.copy(
            history = updatedHistory,
            playerState = newPlayer,
            worldState = newWorld,
            npcs = npcsMutable,
            options = gm.curatedOptions ?: listOf("Investigar região", "Confrontrar perigo", "Conferir status")
        )

        _audioState.value = AudioState.Idle
        speakOutLoud(gm.narrative)
        saveCurrentGame()
    }

    // Allocate manually available attribute stats points (Module 19/27)
    fun distributeStat(statName: String) {
        val current = _gameState.value
        val player = current.playerState
        if (player.unassignedPoints <= 0) return

        val attributes = player.attributes
        val newAttributes = when (statName.uppercase()) {
            "FOR", "STRENGTH" -> attributes.copy(strength = attributes.strength + 1)
            "AGI", "AGILITY" -> attributes.copy(agility = attributes.agility + 1)
            "INT", "INTELLIGENCE" -> attributes.copy(intelligence = attributes.intelligence + 1)
            "VIT", "VITALITY" -> attributes.copy(vitality = attributes.vitality + 1)
            "PER", "PERCEPTION" -> attributes.copy(perception = attributes.perception + 1)
            "WIL", "WILLPOWER" -> attributes.copy(willpower = attributes.willpower + 1)
            else -> attributes
        }

        _gameState.value = current.copy(
            playerState = player.copy(
                attributes = newAttributes,
                unassignedPoints = player.unassignedPoints - 1
            )
        )
        saveCurrentGame()
    }

    // Persistent Room Database synchronization (Auto-Save)
    private fun saveCurrentGame() {
        val state = _gameState.value
        viewModelScope.launch {
            try {
                val adapter = moshi.adapter(GameState::class.java)
                val stateJson = withContext(Dispatchers.Default) { adapter.toJson(state) }
                saveDao.insertSave(GameSave(id = 1, stateJson = stateJson))
                Log.d("GameViewModel", "Game Auto-Saved via Room successfully.")
            } catch (e: Exception) {
                Log.e("GameViewModel", "Room Database auto-save error: ${e.message}")
            }
        }
    }

    // Reset current saving and return to main creation screen
    fun resetGame() {
        stopSpokenNarration()
        viewModelScope.launch {
            try {
                saveDao.deleteSave(1)
                initializeFreshGame()
                Log.d("GameViewModel", "Deleted Room save slot. Fresh initialization started.")
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error deleting database slot save: ${e.message}")
            }
        }
    }

    // Spoken narrator output handler with dual AI TTS / Native fallback stream
    private fun speakOutLoud(text: String) {
        stopSpokenNarration()
        _audioState.value = AudioState.SpeakingNarrator
        
        viewModelScope.launch {
            // First attempt: Generative Gemini model AI TextToSpeech (using gemini-3.1-flash-tts-preview)
            val cleanTextForSpeech = text.replace(Regex("[#*`_]+"), " ") // Strip markdown characters
            val ttsBytes = geminiRepository.convertTextToSpeech(cleanTextForSpeech)

            if (ttsBytes != null) {
                playGeneratedAudio(ttsBytes)
            } else {
                // Secondary local fallback: Android TV native Text-To-Speech
                Log.w("GameViewModel", "Gemini AI TTS failed. Swapping instantly to Android Native TextToSpeech engine.")
                if (ttsInitialized && nativeTts != null) {
                    nativeTts?.speak(cleanTextForSpeech, TextToSpeech.QUEUE_FLUSH, null, "GameNarrationID")
                } else {
                    Log.e("GameViewModel", "No speech engine available/initialized on this TV device.")
                    _audioState.value = AudioState.Idle
                }
            }
        }
    }

    private fun playGeneratedAudio(audioBytes: ByteArray) {
        try {
            val tempFile = File(context.cacheDir, "speech_narrator.wav")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    _audioState.value = AudioState.Idle
                    Log.d("GameViewModel", "Narrator speech playback finished successfully.")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("GameViewModel", "MediaPlayer error during synthesized WAV play: $what, $extra")
                    _audioState.value = AudioState.Idle
                    false
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "MediaPlayer playback initialization failed: ${e.message}", e)
            _audioState.value = AudioState.Idle
        }
    }

    fun stopSpokenNarration() {
        if (_audioState.value == AudioState.SpeakingNarrator) {
            _audioState.value = AudioState.Idle
        }
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error stopping MediaPlayer: ${e.message}")
        }
        try {
            if (nativeTts?.isSpeaking == true) {
                nativeTts?.stop()
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error stopping local speech engine: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSpokenNarration()
        try {
            nativeTts?.shutdown()
            nativeTts = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    // Structured dungeon master prompt builder following all Engine rules (Module 1, 2, 3, 4, 5)
    private fun buildSystemPrompt(): String {
        return """
            Você é a mente inteligente e o Narrador profissional de RPG por trás do jogo "WhatIsRPG? O Eco da Podridão".
            Seu dever é conduzir uma simulação contínua e persistente de jogo baseada em comandos falados em linguagem natural.
            
            Siga escrupulosamente os seguintes mandamentos técnicos de engine:
            
            1. ESTADO E PERSISTÊNCIA (Módulos 1 e 5):
               - Mantenha a realidade unificada. Mudanças em HP/MP, ouro, itens, atributos locais do jogador ou relação com NPCs são irreversíveis.
               - Suas saídas devem sincronizar e atualizar perfeitamente estes campos na resposta estruturada para que o aplicativo persista.
               
            2. UNIVERSO DE RETRO-FANTASIA E A PODRIDÃO (Módulos 3 e 4):
               - O cenário é o Reino Sombrio, atormentado pelo "Eco da Podridão". A 'Podridão' corrompe a fauna, muda as marés mágicas e infecta NPCs e masmorras. O nível varia de 0 a 100.
               - As ações consomem tempo de forma lógica (Manhã, Tarde, Noite, Madrugada). A noite fortalece forças lunares celestes e perigos gravitacionais.
               
            3. MECÂNICAS DE COMBATE E ATRIBUTOS (Módulo 2):
               - Se houver conflito ativo, as resoluções físicas baseiam-se em Força (FOR), Agilidade (AGI) e Vitalidade (VIT). Magias e resistências celestes dependem de Inteligência (INT), Percepção (PER) e Força de Vontade (WIL).
               - Magias não são genéricas! Para personagens com afinidade lunar ou magia de gravidade, descreva seus feitiços de forma procedural e elegante (luz de prata, ondas gravitacionais, marés, marfim estelar). Nunca use feitiços vulgares de fogo ou gelo tradicionais.
               - O combate deve ser tático. O inimigo ataca visando fraquezas emocionais ou físicas.
               
            4. NPCS E MISSÕES INTELIGENTES (Módulo 3):
               - NPCs agem como seres autônomos. Eles lembram de suas atitudes anteriores. Altere a 'affinity' de -100 (ódio/traição) a +100 (lealdade/respeito de sangue).
               - Altere e use status emocionais deles (Medo, Raiva, Confiança, Ambição). Eles reagem às cicatrizes e títulos conquistados pelo herói.
               
            5. FORMATO DE RETORNO OBRIGATÓRIO (JSON):
               Você DEVE responder UNICAMENTE com um objeto JSON válido que possua a estrutura abaixo:
               {
                 "narrative": "Uma narração imersiva, rica, literária e envolvente em português (máximo de 3 parágrafos) descrevendo as consequências imediatas da ação do jogador, do ambiente e diálogos.",
                 "player_update": {
                   "level": null, // substitua por número se subir de nível
                   "experience": null, // substitua por número se ganhar XP
                   "unassignedPoints": null, // pontos de atributos extras ganhos ao upar
                   "hp": null, // substitua pelo novo valor líquido se houver cura ou dano físico
                   "maxHp": null,
                   "mp": null, // custo líquido das magias gastas ou poções carregadas
                   "maxMp": null,
                   "gold": null, // novo valor total se ganhar ou gastar ouro
                   "strength": null,
                   "agility": null,
                   "intelligence": null,
                   "vitality": null,
                   "perception": null,
                   "willpower": null,
                   "itemsGained": null, // Array de novos objetos Item se coletar/lootear: [{"name":"Faca Prateada", "description":"Corte gélido.", "type":"Weapon", "effect":"+2 AGI", "value":10}]
                   "itemsLost": null, // Array de strings com nomes dos itens consumidos/perdidos: ["Poção de Cura"]
                   "skillsGained": null, // Array de feitiços/habilidades Skill aprendidos: [{"name":"Giro Maré", "description":"...', cost: 5}]
                   "titlesGained": null, // Array de strings com novos títulos ex: ["O Ceifador da Névoa"]
                   "scarsGained": null // Marcas físicas/mentais do combate: ["Olho Esquerdo Cego (Névoa)"]
                 },
                 "world_update": {
                   "region": null, // mude o nome da região se viajou
                   "timeOfDay": null, // "Manhã", "Tarde", "Noite", "Madrugada"
                   "rotLevel": null, // alteração na Podridão física
                   "locationDescription": null,
                   "questEvents": null // lista de strings registrando missões novas/concluídas
                 },
                 "npc_updates": [
                   {
                     "name": "Nome do NPC",
                     "description": null, // descrição de nova aparência física se alterada
                     "affinityChange": null, // número positivo/negativo representando variação na afeição (ex: -5 ou +10)
                     "emotion": null, // "Medo", "Raiva", "Confiança", "Ambição" ou "Neutro"
                     "memoryAddition": null // string com nova memória sobre o herói para guardar permanentemente
                   }
                 ],
                 "curated_options": [
                   "Um comando curto em linguagem natural de ação tática de sobrevivência 1",
                   "Um comando curto em linguagem natural de ação de diálogo ou inteligência 2",
                   "Um comando curto em linguagem natural de exploração local do mundo 3"
                 ],
                 "combat_active": false // defina como true se a narrativa resultar em um confronto ativo com turno estruturado
               }
               
            Certifique-se de retornar as chaves do JSON exatamente com estes nomes e não envolver o retorno em blocos markdown de código ```json, envie apenas a string JSON bruta e válida para parsing de moshi!
        """.trimIndent()
    }
}
